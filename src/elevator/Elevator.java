package elevator;

import building.Building;
import floor.Floor;
import floor.FloorQueue;
import passenger.Passenger;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Elevator implements Runnable {
    private static final AtomicLong idCounter = new AtomicLong(0);

    private final long id;
    private final ElevatorType type;
    private final double maxWeight;
    private final long speedMs;
    private final List<Floor> floors;
    private final Building building;
    private final AtomicBoolean stopped;

    private final AtomicInteger currentFloor = new AtomicInteger(0);
    private double currentWeight = 0;

    private volatile boolean broken = false;
    private final Object repairMonitor = new Object();
    private volatile CountDownLatch repairedLatch;

    private Passenger passengerInside = null;
    private volatile boolean readyToPickUp = false;
    private volatile long totalTravelTimeMs = 0;

    public Elevator(ElevatorType type, double maxWeight, long speedMs,
                    List<Floor> floors,
                    Building building,
                    AtomicBoolean stopped) {
        this.id = idCounter.incrementAndGet();
        this.type = type;
        this.maxWeight = maxWeight;
        this.speedMs = speedMs;
        this.floors = floors;
        this.building = building;
        this.stopped = stopped;
        for (Floor f : floors) f.registerElevator(id);
    }

    @Override
    public void run() {
        System.out.printf("[آسانسور %d - %s]  شروع به کار کرد%n", id, type.getLabel());
        while (!stopped.get() || hasWaitingPassengers() || passengerInside != null) {
            int nextFloor = determineNextFloor();
            if (nextFloor == -1) {
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                continue;
            }

            if (currentFloor.get() != nextFloor) {
                moveTo(nextFloor);
            }

            if (!broken) {
                handlePassengersAtCurrentFloor();
            } else {
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
            }
        }
        System.out.printf("[آسانسور %d - %s]  خاموش شد%n", id, type.getLabel());
    }

    private int determineNextFloor() {
        if (passengerInside != null) return passengerInside.getDstFloor();

        int nearestFloor = -1;
        int minDiff = Integer.MAX_VALUE;
        for (Floor f : floors) {
            if (f.queueFor(id).size() > 0) {
                int diff = Math.abs(f.getNumber() - currentFloor.get());
                if (diff < minDiff) {
                    minDiff = diff;
                    nearestFloor = f.getNumber();
                }
            }
        }
        return nearestFloor;
    }

    private void handlePassengersAtCurrentFloor() {
        if (passengerInside != null && passengerInside.getDstFloor() == currentFloor.get()) {
            synchronized (this) {
                this.notifyAll();
            }
            long start = System.currentTimeMillis();
            while (passengerInside != null && System.currentTimeMillis() - start < 1000) {
                try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }

        if (passengerInside == null) {
            FloorQueue q = floors.get(currentFloor.get()).queueFor(id);
            if (q.size() > 0) {
                this.readyToPickUp = true;
                q.wakeAll();

                long start = System.currentTimeMillis();
                while (passengerInside == null && q.size() > 0 && System.currentTimeMillis() - start < 1000) {
                    try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
                this.readyToPickUp = false;
            }
        }
    }

    public synchronized boolean tryEnter(Passenger p) {
        if (!readyToPickUp || passengerInside != null) return false;

        if (currentWeight + p.totalWeight() > maxWeight) {
            System.out.printf("[آسانسور %d]   ظرفیت کافی نیست برای %s%n", id, p.getName());
            return false;
        }


        if (!building.tryAddGlobalWeight(p.totalWeight())) {
            return false;
        }

        passengerInside = p;
        currentWeight += p.totalWeight();
        System.out.printf("[آسانسور %d - %s]  %s سوار شد (طبقه %d → %d)%n",
                id, type.getLabel(), p.getName(), currentFloor.get(), p.getDstFloor());
        return true;
    }

    public synchronized void exit(Passenger p) {
        if (passengerInside == p) {
            System.out.printf("[آسانسور %d]  %s پیاده شد در طبقه %d%n", id, p.getName(), currentFloor.get());
            passengerInside = null;
            currentWeight -= p.totalWeight();
            building.removeGlobalWeight(p.totalWeight());
        }
    }

    public boolean isReadyToPickUp(int floor) {
        return readyToPickUp && currentFloor.get() == floor && !broken;
    }

    public boolean hasExited(Passenger p) {
        return passengerInside != p;
    }

    public int getCurrentFloor() {
        return currentFloor.get();
    }

    public boolean isBroken() {
        return broken;
    }

    public long getTotalTravelTimeMs() {
        return totalTravelTimeMs;
    }

    public void markRepaired() {
        synchronized (repairMonitor) {
            broken = false;
            if (repairedLatch != null) repairedLatch.countDown();
            System.out.printf("[آسانسور %d - %s]  تعمیر شد در طبقه %d%n",
                    id, type.getLabel(), currentFloor.get());
        }
    }

    private boolean moveTo(int dst) {
        int src = currentFloor.get();
        if (src == dst) return true;
        int dir = dst > src ? 1 : -1;

        while (currentFloor.get() != dst) {
            try {
                Thread.sleep(speedMs);
                totalTravelTimeMs += speedMs;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            currentFloor.addAndGet(dir);

            synchronized (this) {
                this.notifyAll();
            }

            if (!broken && Math.random() < 0.03) {
                synchronized (repairMonitor) {
                    broken = true;
                    repairedLatch = new CountDownLatch(1);
                }
                int brokenAt = currentFloor.get();
                System.out.printf("[آسانسور %d - %s]   خراب شد در طبقه %d%n",
                        id, type.getLabel(), brokenAt);
                building.reportBroken(id, brokenAt);

                synchronized (this) {
                    this.notifyAll();
                }

                try {
                    repairedLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                return false;
            }
        }
        return true;
    }

    private boolean hasWaitingPassengers() {
        for (Floor f : floors) {
            FloorQueue q = f.queueFor(id);
            if (q != null && q.size() > 0) return true;
        }
        return false;
    }

    public long getId() {
        return id;
    }

    public ElevatorType getType() {
        return type;
    }

}
