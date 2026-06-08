package passenger;

public enum PassengerRole {
    STUDENT("دانشجو"),
    PROFESSOR("استاد"),
    STAFF("کارمند"),
    PORTER("باربر"),
    REPAIRMAN("تعمیرکار");
    private final String label;

    PassengerRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public int rank() {
        if (this == PROFESSOR) return 3;
        if (this == STAFF) return 2;
        if (this == STUDENT) return 1;
        return 0;
    }

}
