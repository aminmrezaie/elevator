package elevator;

public enum ElevatorType {
    GENERAL("همگانی", "general"),
    FACULTY("دانشگاهی", "faculty"),
    FREIGHT("باربری", "freight");

    private final String label;
    private final String key;

    ElevatorType(String label, String key) {
        this.label = label;
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public String getKey() {
        return key;
    }

}
