package elevator;

import floor.Floor;
import floor.FloorQueue;
import passenger.Passenger;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class Elevator implements Runnable {
    private static final AtomicLong idCounter = new AtomicLong(0);

    private final long id;
    private final ElevatorType type;
    private final double maxWeight;
    private final long speedMs;
    private final List<Floor> floors;
    private final BlockingQueue<RepairRequest> repairQueue;
    private final AtomicBoolean stopped;

    private final AtomicInteger currentFloor = new AtomicInteger(0);
    private final ReentrantLock weightLock = new ReentrantLock();
    private double currentWeight = 0;

    private volatile boolean broken = false;
    private final Object repairMonitor = new Object();
    private volatile CountDownLatch repairedLatch;

    public Elevator(ElevatorType type, double maxWeight, long speedMs,
                    List<Floor> floors,
                    BlockingQueue<RepairRequest> repairQueue,
                    AtomicBoolean stopped) {
        this.id = idCounter.incrementAndGet();
        this.type = type;
        this.maxWeight = maxWeight;
        this.speedMs = speedMs;
        this.floors = floors;
        this.repairQueue = repairQueue;
        this.stopped = stopped;
        for (Floor f : floors) f.registerElevator(id);
    }

    @Override
    public void run() {
        System.out.printf("[آسانسور %d - %s] 🚀 شروع به کار کرد%n", id, type.getLabel());
        while (!stopped.get() || hasWaitingPassengers()) {
            Passenger p = pickPassenger();
            if (p == null) {
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
            servePassenger(p);
        }
        System.out.printf("[آسانسور %d - %s] 🛑 خاموش شد%n", id, type.getLabel());
    }
    public void markRepaired() {
        synchronized (repairMonitor) {
            broken = false;
            if (repairedLatch != null) repairedLatch.countDown();
            System.out.printf("[آسانسور %d - %s] ✅ تعمیر شد در طبقه %d%n",
                    id, type.getLabel(), currentFloor.get());
        }
    }
    private boolean moveTo(int dst) {
        int src = currentFloor.get();
        if (src == dst) return true;
        int dir = dst > src ? 1 : -1;

        for (int f = src; f != dst; f += dir) {
            try { Thread.sleep(speedMs); } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); return false;
            }
            currentFloor.set(f + dir);

            if (!broken && Math.random() < 0.03) {
                synchronized (repairMonitor) {
                    broken = true;
                    repairedLatch = new CountDownLatch(1);
                }
                int brokenAt = currentFloor.get();
                System.out.printf("[آسانسور %d - %s] ⚠️  خراب شد در طبقه %d%n",
                        id, type.getLabel(), brokenAt);
                repairQueue.add(new RepairRequest(id, brokenAt));
                try { repairedLatch.await(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); return false;
                }
                return false;
            }
        }
        return true;
    }

    private Passenger pickPassenger() {
        FloorQueue q = floors.get(currentFloor.get()).queueFor(id);
        return q == null ? null : q.tryDequeue();
    }

    private void servePassenger(Passenger p) {
        if (!canCarry(p)) {
            System.out.printf("[آسانسور %d] ⚖️  ظرفیت کافی نیست برای %s — برگشت به صف%n", id, p.getName());
            floors.get(p.getSrcFloor()).enqueueFor(id, p);
            return;
        }

        if (currentFloor.get() != p.getSrcFloor()) {
            System.out.printf("[آسانسور %d - %s] 🔽 حرکت به طبقه %d برای %s%n",
                    id, type.getLabel(), p.getSrcFloor(), p.getName());
            if (!moveTo(p.getSrcFloor())) {
                floors.get(p.getSrcFloor()).enqueueFor(id, p);
                return;
            }
        }

        addWeight(p.totalWeight());
        p.getTask().start();
        System.out.printf("[آسانسور %d - %s] 🚶 %s سوار شد (طبقه %d → %d)%n",
                id, type.getLabel(), p.getName(), p.getSrcFloor(), p.getDstFloor());

        if (!moveTo(p.getDstFloor())) {
            int stuckAt = currentFloor.get();
            removeWeight(p.totalWeight());
            System.out.printf("[آسانسور %d] 🔴 %s پیاده شد در طبقه %d (خرابی)%n", id, p.getName(), stuckAt);
            p.setCurrentFloor(stuckAt);
            // مسافر از طبقه فعلی مبدأ جدید دارد — در صف همان طبقه می‌ماند
            floors.get(stuckAt).enqueueFor(id, p);
            return;
        }

        removeWeight(p.totalWeight());
        p.setCurrentFloor(p.getDstFloor());
        p.getTask().complete();
        System.out.printf("[آسانسور %d - %s] 🏁 %s به طبقه %d رسید%n",
                id, type.getLabel(), p.getName(), p.getDstFloor());
    }

    private boolean canCarry(Passenger p) {
        weightLock.lock();
        try { return currentWeight + p.totalWeight() <= maxWeight; }
        finally { weightLock.unlock(); }
    }

    private void addWeight(double w) {
        weightLock.lock();
        try { currentWeight += w; }
        finally { weightLock.unlock(); }
    }

    private void removeWeight(double w) {
        weightLock.lock();
        try { currentWeight = Math.max(0, currentWeight - w); }
        finally { weightLock.unlock(); }
    }

    private boolean hasWaitingPassengers() {
        for (Floor f : floors) {
            FloorQueue q = f.queueFor(id);
            if (q != null && q.size() > 0) return true;
        }
        return false;
    }

    public long getId()           { return id; }
    public ElevatorType getType() { return type; }

}
