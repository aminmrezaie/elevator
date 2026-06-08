package floor;

import passenger.Passenger;

import java.util.HashMap;
import java.util.Map;

public class Floor {

    private final int number;
    private final Map<Long, FloorQueue> queues = new HashMap<>();

    public Floor(int number) {
        this.number = number;
    }

    public synchronized void registerElevator(long elevId) {
        queues.put(elevId, new FloorQueue());
    }

    public synchronized FloorQueue queueFor(long elevId) {
        return queues.get(elevId);
    }

    public void enqueueFor(long elevId, Passenger p) {
        FloorQueue q = queueFor(elevId);
        if (q != null) q.enqueue(p);
    }

    public int getNumber() {
        return number;
    }

}
