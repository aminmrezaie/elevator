package elevator;

public class RepairRequest {
    public final long elevId;
    public final int floor;

    public RepairRequest(long elevId, int floor) {
        this.elevId = elevId;
        this.floor = floor;
    }
}
