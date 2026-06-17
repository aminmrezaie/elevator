package simulation;

import building.Building;
import passenger.Passenger;
import passenger.PassengerRole;
import task.Task;

import java.util.Random;

public class Simulation {

    private static Simulation instance;
    private final Config   cfg;
    private final Building building;
    private final Random   rng = new Random();

    private Simulation(Config cfg) {
        this.cfg = cfg;
        int total = cfg.numStudents + cfg.numProfessors + cfg.numStaff + cfg.numPorters;
        this.building = new Building(
                cfg.numFloors,
                cfg.numGeneralElevators,
                cfg.numFacultyElevators,
                cfg.numFreightElevators,
                cfg.numRepairmen,
                cfg.maxWeightGeneral,
                cfg.maxWeightFaculty,
                cfg.maxWeightFreight,
                cfg.elevatorSpeedMs,
                total
        );
    }

    public static synchronized void init(Config cfg) {
        if (instance == null) {
            instance = new Simulation(cfg);
        }
    }

    public static Simulation getInstance() {
        return instance;
    }

    public void run() throws InterruptedException {
        System.out.printf("ساختمان با %d طبقه، %d همگانی، %d دانشگاهی، %d باربری%n%n",
                cfg.numFloors, cfg.numGeneralElevators,
                cfg.numFacultyElevators, cfg.numFreightElevators);

        building.start();
        addPassengers();
        building.shutdown();
    }

    private void addPassengers() throws InterruptedException {
        addGroup(PassengerRole.STUDENT,   cfg.numStudents,   18, 30, 60, 80,  0);
        addGroup(PassengerRole.PROFESSOR, cfg.numProfessors, 35, 65, 65, 90,  0);
        addGroup(PassengerRole.STAFF,     cfg.numStaff,      25, 55, 55, 85,  0);
        addGroup(PassengerRole.PORTER,    cfg.numPorters,    20, 45, 70, 95, 80);
    }

    private void addGroup(PassengerRole role, int count,
                          int minAge, int maxAge,
                          double minW, double maxW,
                          double maxCargo) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            int src = rng.nextInt(cfg.numFloors);
            int dst;
            do { dst = rng.nextInt(cfg.numFloors); } while (dst == src);

            int    age    = minAge + rng.nextInt(maxAge - minAge + 1);
            double weight = minW   + rng.nextDouble() * (maxW - minW);
            double cargo  = maxCargo > 0 ? rng.nextDouble() * maxCargo : 0;

            Task.Priority priority;
            int randPriority = rng.nextInt(3);
            if (randPriority == 0) priority = Task.Priority.LOW;
            else if (randPriority == 1) priority = Task.Priority.MEDIUM;
            else priority = Task.Priority.HIGH;

            Passenger p = new Passenger(role, src, dst, age, weight, cargo, priority, building);
            building.addPassenger(p);
            Thread.sleep(10);
        }
    }
}
