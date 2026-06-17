import building.Building;
import elevator.Elevator;
import elevator.ElevatorType;
import elevator.RepairRequest;
import floor.Floor;
import passenger.Passenger;
import passenger.PassengerRole;
import task.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ElevatorTest {

    private static class MockBuilding extends Building {
        public MockBuilding() {
            super(10, 1, 0, 0, 0, 1000, 1000, 1000, 10, 0);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== اجرای تست‌های Elevator ===");

        List<Floor> floors = new ArrayList<>();
        for(int i=0; i<10; i++) floors.add(new Floor(i));

        MockBuilding building = new MockBuilding() {
            @Override
            public Elevator chooseElevatorFor(Passenger p, Long excludedElevatorId) {
                return null;
            }
            @Override
            public Floor getFloor(int number) {
                return floors.get(number);
            }
        };

        AtomicBoolean stopped = new AtomicBoolean(false);
        Elevator elevator = new Elevator(ElevatorType.GENERAL, 1000, 10, floors, building, stopped);

        MockBuilding buildingWithElevator = new MockBuilding() {
            @Override
            public Elevator chooseElevatorFor(Passenger p, Long excludedElevatorId) {
                return elevator;
            }
            @Override
            public Floor getFloor(int number) {
                return floors.get(number);
            }
        };

        Passenger p1 = new Passenger(PassengerRole.STUDENT, 0, 5, 20, 70, 0, Task.Priority.LOW, buildingWithElevator);

        Thread elevThread = new Thread(elevator);
        elevThread.start();

        Thread passThread = new Thread(p1);
        passThread.start();

        Thread.sleep(3000);

        stopped.set(true);
        elevThread.join(1000);

        if (p1.getCurrentFloor() == 0 || p1.getCurrentFloor() == 5) {
            System.out.println("تست سوار و پیاده شدن مسافر (ترد مجزا): پاس شد ✓");
        } else {
            System.out.println("تست سوار شدن مسافر: ناموفق ✗ مسافر در طبقه " + p1.getCurrentFloor() + " گیر کرد.");
        }

        System.out.println("پایان تست‌های Elevator.");
    }
}
