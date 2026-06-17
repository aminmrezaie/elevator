package task;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class Task {
    public enum Status {
        PENDING,
        IN_PROGRESS,
        DONE
    }
    public enum Priority {
        LOW(1),
        MEDIUM(2),
        HIGH(3);

        private final int value;
        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static final AtomicLong counter = new AtomicLong(0);

    private final long id;
    private final int priority;
    private volatile Status status;
    private final CountDownLatch latch = new CountDownLatch(1);

    public Task(int priority) {
        this.id = counter.incrementAndGet();
        this.priority = priority;
        this.status = Status.PENDING;
    }

    public void start() {
        this.status = Status.IN_PROGRESS;
    }

    public void complete() {
        this.status = Status.DONE;
        latch.countDown();
        System.out.printf("[تسک #%d]  تسک با اولویت %d تکمیل شد%n", id, priority);
    }

    public void await() throws InterruptedException {
        latch.await();
    }

    public long getId() {
        return id;
    }

    public int getPriority() {
        return priority;
    }

    public Status getStatus() {
        return status;
    }
}
