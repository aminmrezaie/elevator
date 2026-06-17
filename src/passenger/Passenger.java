package passenger;

import building.Building;
import elevator.Elevator;
import floor.FloorQueue;
import task.Task;

import java.util.concurrent.atomic.AtomicLong;

public class Passenger implements Runnable {
    private static final AtomicLong counter = new AtomicLong(0);

    private final long id;
    private final PassengerRole role;
    private final String name;
    private final int age;
    private final double weight;
    private final double cargoWeight;
    private final int srcFloor;
    private volatile int dstFloor;
    private volatile int currentFloor;
    private final Task task;
    private final Building building;

    public Passenger(PassengerRole role, int srcFloor, int dstFloor, int age,
                     double weight, double cargoWeight, Task.Priority priority, Building building) {
        this.id = counter.incrementAndGet();
        this.role = role;
        this.name = role.getLabel() + "-" + id;
        this.age = age;
        this.weight = weight;
        this.cargoWeight = cargoWeight;
        this.srcFloor = srcFloor;
        this.dstFloor = dstFloor;
        this.currentFloor = srcFloor;
        this.task = new Task(priority);
        this.building = building;
    }

    @Override
    public void run() {
        try {
            System.out.printf("[مسافر] %s در طبقه %d ظاهر شد، مقصد: %d%n", name, srcFloor, dstFloor);
            doTrip(srcFloor, dstFloor);

            task.start();
            System.out.printf("[مسافر] %s در حال انجام تسک در طبقه %d...%n", name, dstFloor);
            Thread.sleep(1000 + (long) (Math.random() * 2000)); // Simulate work
            task.complete();
            building.logCompletedTask(task);

            if (currentFloor != 0) {
                System.out.printf("[مسافر] %s تسک را تمام کرد. بازگشت به همکف...%n", name);
                doTrip(currentFloor, 0);
            }

            System.out.printf("[مسافر] %s از ساختمان خارج شد.%n", name);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            building.passengerLeft();
        }
    }

    private void doTrip(int fromFloor, int toFloor) throws InterruptedException {
        this.dstFloor = toFloor;
        this.currentFloor = fromFloor;
        Long excludedElevatorId = null;

        while (currentFloor != toFloor) {
            Elevator elevator = building.chooseElevatorFor(this, excludedElevatorId);
            if (elevator == null) {
                if (excludedElevatorId != null) {
                    excludedElevatorId = null;
                    continue;
                }
                System.out.printf("[مسافر] %s آسانسوری پیدا نکرد!%n", name);
                break;
            }
            excludedElevatorId = null;

            FloorQueue queue = building.getFloor(currentFloor).queueFor(elevator.getId());

            boolean switchedQueue = false;
            synchronized (queue) {
                queue.enqueue(this);
                long waitStart = System.currentTimeMillis();
                while (true) {
                    long timeLeft = 5000 - (System.currentTimeMillis() - waitStart);
                    if (timeLeft <= 0) {
                        queue.remove(this);
                        System.out.printf("[مسافر] %s خسته شد و صف آسانسور %d را ترک کرد.%n", name, elevator.getId());
                        excludedElevatorId = elevator.getId(); // Try another one
                        switchedQueue = true;
                        break;
                    }

                    queue.wait(timeLeft);

                    if (elevator.isReadyToPickUp(currentFloor) && queue.peekFair() == this) {
                        if (elevator.tryEnter(this)) {
                            queue.remove(this);
                            break;
                        }
                    }
                }
            }

            if (switchedQueue) continue;

            synchronized (elevator) {
                while (true) {
                    elevator.wait();

                    if (elevator.getCurrentFloor() == dstFloor && !elevator.isBroken()) {
                        elevator.exit(this);
                        this.currentFloor = dstFloor;
                        break;
                    }

                    if (elevator.isBroken()) {
                        elevator.exit(this);
                        this.currentFloor = elevator.getCurrentFloor();
                        excludedElevatorId = elevator.getId();
                        break;
                    }
                }
            }
        }
    }

    public double totalWeight() {
        return weight + cargoWeight;
    }

    public boolean canUse(String elevType) {
        switch (elevType) {
            case "general":
                return role == PassengerRole.STUDENT || role == PassengerRole.REPAIRMAN;
            case "faculty":
                return role == PassengerRole.PROFESSOR || role == PassengerRole.STAFF || role == PassengerRole.REPAIRMAN;
            case "freight":
                return role == PassengerRole.PORTER || role == PassengerRole.REPAIRMAN;
            default:
                return false;

        }
    }

    public boolean isSenior() {
        return age >= 60;
    }

    public long getId() {
        return id;
    }

    public PassengerRole getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public double getWeight() {
        return weight;
    }

    public double getCargoWeight() {
        return cargoWeight;
    }

    public int getSrcFloor() {
        return srcFloor;
    }

    public int getDstFloor() {
        return dstFloor;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Task getTask() {
        return task;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }
    public void setDstFloor(int dstFloor) {
        this.dstFloor = dstFloor;
    }
}
