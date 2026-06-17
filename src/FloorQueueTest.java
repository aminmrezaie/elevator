import floor.FloorQueue;
import passenger.Passenger;
import passenger.PassengerRole;
import task.Task;

public class FloorQueueTest {

    private static class MockBuilding extends building.Building {
        public MockBuilding() {
            super(10, 1, 0, 0, 0, 1000, 1000, 1000, 100, 0);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== اجرای تست‌های FloorQueue ===");
        testFairSelection();
    }

    public static void testFairSelection() {
        FloorQueue queue = new FloorQueue();
        MockBuilding building = new MockBuilding();

        Passenger p1 = new Passenger(PassengerRole.STUDENT, 0, 5, 20, 70, 0, Task.Priority.LOW, building);

        Passenger p2 = new Passenger(PassengerRole.STUDENT, 0, 5, 65, 70, 0, Task.Priority.LOW, building);

        Passenger p3 = new Passenger(PassengerRole.PROFESSOR, 0, 5, 40, 70, 0, Task.Priority.HIGH, building);

        queue.enqueue(p1);
        queue.enqueue(p2);
        queue.enqueue(p3);

        Passenger peeked = queue.peekFair();

        if (peeked == p2) {
            System.out.println("تست 1 (اولویت سالمند): پاس شد ✓");
        } else {
            System.out.println("تست 1 (اولویت سالمند): ناموفق ✗");
        }

        queue.remove(p2);

        peeked = queue.peekFair();
        if (peeked == p3) {
            System.out.println("تست 2 (اولویت تسک): پاس شد ✓");
        } else {
            System.out.println("تست 2 (اولویت تسک): ناموفق ✗");
        }

        System.out.println("پایان تست‌های FloorQueue.");
    }
}
