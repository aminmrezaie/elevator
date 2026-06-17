package elevator;

import building.Building;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Repairman implements Runnable {

    private final int id;
    private final Building building;
    private final Map<Long, Elevator> elevators;
    private final AtomicBoolean stopped;

    public Repairman(int id,
                     Building building,
                     Map<Long, Elevator> elevators,
                     AtomicBoolean stopped) {
        this.id = id;
        this.building = building;
        this.elevators = elevators;
        this.stopped = stopped;
    }

    @Override
    public void run() {
        System.out.printf("[تعمیرکار %d]  آماده به کار%n", id);
        Object lock = building.getGlobalRepairLock();

        while (!stopped.get()) {
            RepairRequest req;
            synchronized (lock) {
                req = building.getNextRepair();
                if (req == null) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
            }

            Elevator elev = elevators.get(req.elevId);
            if (elev == null) continue;

            System.out.printf("[تعمیرکار %d]  در حال تعمیر آسانسور %d در طبقه %d...%n",
                    id, req.elevId, req.floor);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            elev.markRepaired();
            System.out.printf("[تعمیرکار %d]  آسانسور %d تعمیر شد%n", id, req.elevId);
        }
        System.out.printf("[تعمیرکار %d]  پایان کار%n", id);
    }
}
