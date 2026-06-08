package passenger;

import task.Task;

import java.util.concurrent.atomic.AtomicLong;

public class Passenger {
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

    public Passenger(PassengerRole role, int srcFloor, int dstFloor, int age,
                     double weight, double cargoWeight, int priority) {
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
