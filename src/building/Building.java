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

    private final BlockingQueue<RepairRequest> repairQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final CountDownLatch passengerLatch;
    private final List<Task> taskLog = Collections.synchronizedList(new ArrayList<>());

    private final ExecutorService elevatorPool;
    private final ExecutorService repairPool;

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
            elevators.add(new Elevator(ElevatorType.GENERAL, maxGeneral, speedMs, floors, repairQueue, stopped));
        for (int i = 0; i < numFaculty; i++)
            elevators.add(new Elevator(ElevatorType.FACULTY, maxFaculty, speedMs, floors, repairQueue, stopped));
        for (int i = 0; i < numFreight; i++)
            elevators.add(new Elevator(ElevatorType.FREIGHT, maxFreight, speedMs, floors, repairQueue, stopped));

        Map<Long, Elevator> elevMap = new HashMap<>();
        for (Elevator e : elevators) elevMap.put(e.getId(), e);

        repairmen = new ArrayList<>();
        for (int i = 0; i < numRepairmen; i++)
            repairmen.add(new Repairman(i + 1, repairQueue, elevMap, stopped));

        elevatorPool = Executors.newFixedThreadPool(elevators.size());
        repairPool = Executors.newFixedThreadPool(Math.max(repairmen.size(), 1));
    }

    public void start() {
        repairmen.forEach(repairPool::submit);
        elevators.forEach(elevatorPool::submit);
    }

    public void addPassenger(Passenger p) {
        Elevator chosen = null;
        for (Elevator e : elevators) {
            if (p.canUse(e.getType().getKey())) {
                chosen = e;
                break;
            }
        }

        if (chosen == null) {
            System.out.printf("[ساختمان]   آسانسور مناسبی برای %s پیدا نشد%n", p.getName());
            passengerLatch.countDown();
            return;
        }

        floors.get(p.getSrcFloor()).enqueueFor(chosen.getId(), p);
        System.out.printf("[مسافر]  %s منتظر در طبقه %d → مقصد طبقه %d%n",
                p.getName(), p.getSrcFloor(), p.getDstFloor());

        final Elevator finalChosen = chosen;
        Thread t = new Thread(() -> {
            try {
                p.getTask().await();
                taskLog.add(p.getTask());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                passengerLatch.countDown();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void shutdown() throws InterruptedException {
        System.out.println("\n[ساختمان]  در حال انتظار برای پایان تمام تسک‌ها...");
        passengerLatch.await();
        System.out.println("[ساختمان]  همه مسافران به مقصد رسیدند — خاموش‌سازی سیستم");
        stopped.set(true);
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
        System.out.printf("تعداد تسک‌های انجام‌شده: %d%n%n", taskLog.size());
        for (Task t : taskLog)
            System.out.printf("  • تسک #%d  (اولویت: %d) — ✓ تکمیل%n", t.getId(), t.getPriority());
        System.out.println("══════════════════════════════════════════");
    }

}
