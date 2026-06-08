package elevator;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Repairman implements Runnable {

    private final int id;
    private final BlockingQueue<RepairRequest> repairQueue;
    private final Map<Long, Elevator> elevators;
    private final AtomicBoolean stopped;

    public Repairman(int id,
                     BlockingQueue<RepairRequest> repairQueue,
                     Map<Long, Elevator> elevators,
                     AtomicBoolean stopped) {
        this.id          = id;
        this.repairQueue = repairQueue;
        this.elevators   = elevators;
        this.stopped     = stopped;
    }
    @Override
    public void run() {
        System.out.printf("[تعمیرکار %d] 🔧 آماده به کار%n", id);
        while (!stopped.get() || !repairQueue.isEmpty()) {
            try {
                RepairRequest req = repairQueue.poll(200, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (req == null) continue;

                Elevator elev = elevators.get(req.elevId);
                if (elev == null) continue;

                System.out.printf("[تعمیرکار %d] 🔨 در حال تعمیر آسانسور %d در طبقه %d...%n",
                        id, req.elevId, req.floor);
                Thread.sleep(1500);
                elev.markRepaired();
                System.out.printf("[تعمیرکار %d] ✅ آسانسور %d تعمیر شد%n", id, req.elevId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.printf("[تعمیرکار %d] 🛑 پایان کار%n", id);
    }
}
