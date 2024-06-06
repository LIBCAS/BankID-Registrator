package cz.cas.lib.bankid_registrator.entities.patron;

public enum PatronStatus {
    STATUS_03("03", "Institute Staff", 1095, "", ""),
    STATUS_04("04", "User biannual", 183, "44", "46"),
    STATUS_07("07", "Honorary membership", 1095, "", ""),
    STATUS_08("08", "VIP", 18250, "", ""),
    STATUS_09("09", "ILL Borrower PRAGUE", 1825, "", ""),
    STATUS_10("10", "Retirees", 365, "44", "46"),
    STATUS_11("11", "ILL Borrower", 1825, "", ""),
    STATUS_15("15", "Retirees-study user", 365, "44", "46"),
    STATUS_16("16", "User", 365, "45", "47"),
    STATUS_23("23", "Institute Staff - study", 365, "", ""),
    STATUS_94("94", "Study user + net annual", 365, "45", "47"),
    STATUS_95("95", "Study user + net biannual", 183, "44", "46"),
    STATUS_96("96", "Remote access - Institute Staff", 1095, "", ""),
    STATUS_97("97", "Remote access", 365, "45", "47");

    private final String id;
    private final String name;
    private final int membershipLength;
    private final String registrationItemStatusId;
    private final String renewalItemStatusId;

    PatronStatus(String id, String name, int membershipLength, String registrationItemStatusId, String renewalItemStatusId) {
        this.id = id;
        this.name = name;
        this.membershipLength = membershipLength;
        this.registrationItemStatusId = registrationItemStatusId;
        this.renewalItemStatusId = renewalItemStatusId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMembershipLength() {
        return membershipLength;
    }

    public String getRegistrationItemStatusId() {
        return registrationItemStatusId;
    }

    public String getRenewalItemStatusId() {
        return renewalItemStatusId;
    }

    public static PatronStatus getById(String id) {
        for (PatronStatus status : values()) {
            if (status.getId().equals(id)) {
                return status;
            }
        }
        return null;
    }

    public static PatronStatus getByName(String name) {
        for (PatronStatus status : values()) {
            if (status.getName().equals(name)) {
                return status;
            }
        }
        return null;
    }
}