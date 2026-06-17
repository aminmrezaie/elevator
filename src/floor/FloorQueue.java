package floor;

import passenger.Passenger;

import java.util.ArrayList;
import java.util.List;

public class FloorQueue {
    private final List<Passenger> queue = new ArrayList<>();

    public synchronized void enqueue(Passenger p) {
        queue.add(p);
        notifyAll();
    }

    public synchronized Passenger dequeue() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        int idx = selectFair();
        return queue.remove(idx);
    }

    public synchronized Passenger tryDequeue() {
        if (queue.isEmpty()) return null;
        return queue.remove(selectFair());
    }

    public synchronized Passenger peekFair() {
        if (queue.isEmpty()) return null;
        return queue.get(selectFair());
    }

    public synchronized boolean remove(Passenger p) {
        return queue.remove(p);
    }

    public synchronized int size() {
        return queue.size();
    }

    private int selectFair() {
        int best = 0;
        for (int i = 1; i < queue.size(); i++) {
            Passenger p = queue.get(i);
            Passenger curr = queue.get(best);

            if (p.isSenior() && !curr.isSenior()) {
                best = i;
                continue;
            }
            if (!p.isSenior() && curr.isSenior()) {
                continue;
            }

            if (p.getTask().getPriority().getValue() > curr.getTask().getPriority().getValue()) {
                best = i;
                continue;
            }
            if (p.getTask().getPriority().getValue() < curr.getTask().getPriority().getValue()) {
                continue;
            }

            if (p.getRole().rank() > curr.getRole().rank()) {
                best = i;
            }
        }
        return best;
    }

    public synchronized void wakeAll() {
        notifyAll();
    }

}
