package building;

import elevator.Elevator;
import elevator.ElevatorType;
import elevator.RepairRequest;
import elevator.Repairman;
import floor.Floor;
import passenger.Passenger;
import task.Task;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Building {
    private final List<Floor> floors;
    private final List<Elevator> elevators;
    private final List<Repairman> repairmen;

    private final Object globalRepairLock = new Object();
    private final Queue<RepairRequest> brokenElevators = new LinkedList<>();

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final CountDownLatch passengerLatch;
    private final List<Task> taskLog = Collections.synchronizedList(new ArrayList<>());

    private final ExecutorService elevatorPool;
    private final ExecutorService repairPool;

    private final double maxGlobalWeight = 2000.0; // Assume 2000kg limit for entire building
    private double currentGlobalWeight = 0;

    public Building(int numFloors,
                    int numGeneral, int numFaculty, int numFreight,
                    int numRepairmen,
                    double maxGeneral, double maxFaculty, double maxFreight,
                    long speedMs,
                    int totalPassengers) {

        this.passengerLatch = new CountDownLatch(totalPassengers);

        floors = new ArrayList<>();
        for (int i = 0; i < numFloors; i++) floors.add(new Floor(i));

        elevators = new ArrayList<>();
        for (int i = 0; i < numGeneral; i++)
            elevators.add(new Elevator(ElevatorType.GENERAL, maxGeneral, speedMs, floors, this, stopped));
        for (int i = 0; i < numFaculty; i++)
            elevators.add(new Elevator(ElevatorType.FACULTY, maxFaculty, speedMs, floors, this, stopped));
        for (int i = 0; i < numFreight; i++)
            elevators.add(new Elevator(ElevatorType.FREIGHT, maxFreight, speedMs, floors, this, stopped));

        Map<Long, Elevator> elevMap = new HashMap<>();
        for (Elevator e : elevators) elevMap.put(e.getId(), e);

        repairmen = new ArrayList<>();
        for (int i = 0; i < numRepairmen; i++)
            repairmen.add(new Repairman(i + 1, this, elevMap, stopped));

        elevatorPool = Executors.newFixedThreadPool(elevators.size());
        repairPool = Executors.newFixedThreadPool(Math.max(repairmen.size(), 1));
    }

    public synchronized boolean tryAddGlobalWeight(double w) {
        if (currentGlobalWeight + w > maxGlobalWeight) return false;
        currentGlobalWeight += w;
        return true;
    }

    public synchronized void removeGlobalWeight(double w) {
        currentGlobalWeight = Math.max(0, currentGlobalWeight - w);
    }

    public Object getGlobalRepairLock() {
        return globalRepairLock;
    }

    public void reportBroken(long elevId, int floor) {
        synchronized (globalRepairLock) {
            brokenElevators.add(new RepairRequest(elevId, floor));
            globalRepairLock.notifyAll(); // Wake up repairmen
        }
    }

    public RepairRequest getNextRepair() {
        synchronized (globalRepairLock) {
            return brokenElevators.poll();
        }
    }

    public void start() {
        repairmen.forEach(repairPool::submit);
        elevators.forEach(elevatorPool::submit);
    }

    public void addPassenger(Passenger p) {
        Thread t = new Thread(p);
        t.setDaemon(true);
        t.start();
    }

    public Elevator chooseElevatorFor(Passenger p, Long excludedElevatorId) {
        Elevator chosen = null;
        int minQueueSize = Integer.MAX_VALUE;

        for (Elevator e : elevators) {
            if (p.canUse(e.getType().getKey()) && !e.isBroken()) {
                if (excludedElevatorId != null && e.getId() == excludedElevatorId) continue;

                int qSize = 0;
                for (Floor f : floors) {
                    qSize += f.queueFor(e.getId()).size();
                }
                if (qSize < minQueueSize) {
                    minQueueSize = qSize;
                    chosen = e;
                }
            }
        }
        return chosen;
    }

    public Floor getFloor(int number) {
        return floors.get(number);
    }

    public void logCompletedTask(Task t) {
        taskLog.add(t);
    }

    public void passengerLeft() {
        passengerLatch.countDown();
    }

    public void shutdown() throws InterruptedException {
        System.out.println("\n[ساختمان]  در حال انتظار برای پایان تمام تسک‌ها...");
        passengerLatch.await();
        System.out.println("[ساختمان]  همه مسافران به مقصد رسیدند — خاموش‌سازی سیستم");
        stopped.set(true);

        for (Floor f : floors) {
            for (Elevator e : elevators) {
                f.queueFor(e.getId()).wakeAll();
            }
        }

        synchronized (globalRepairLock) {
            globalRepairLock.notifyAll();
        }

        elevatorPool.shutdown();
        repairPool.shutdown();
        elevatorPool.awaitTermination(10, TimeUnit.SECONDS);
        repairPool.awaitTermination(10, TimeUnit.SECONDS);
        printReport();
    }

    private void printReport() {
        System.out.println("\n══════════════════════════════════════════");
        System.out.println("           گزارش پایان روز");
        System.out.println("══════════════════════════════════════════");

        long totalTravelMs = 0;
        for (Elevator e : elevators) {
            totalTravelMs += e.getTotalTravelTimeMs();
        }
        System.out.printf("مجموع زمان سفر همه آسانسورها: %d میلی‌ثانیه%n", totalTravelMs);
        System.out.printf("تعداد تسک‌های انجام‌شده: %d%n%n", taskLog.size());

        for (Task t : taskLog)
            System.out.printf("  • تسک #%d  (اولویت: %s) — ✓ تکمیل%n", t.getId(), t.getPriority());
        System.out.println("══════════════════════════════════════════");
    }

}
