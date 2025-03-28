package cz.cas.lib.bankid_registrator.entities.patron;

// Possible operations: "bor-info", "bor-auth"
public enum PatronBorXOp {
    BOR_INFO("bor-info"),
    BOR_AUTH("bor-auth"),
    UPDATE_BOR("update-bor"),
    CREATE_ITEM("create-item"),
    HOLD_REQ_CANCEL("hold-req-cancel"),
    DELETE_ITEM("delete-item");

    private final String value;

    PatronBorXOp(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}