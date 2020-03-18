package ccait.ccweb.websocket;


public enum SendMode {

    ALL(0, "ALL"),
    USER(1, "USER"),
    GROUP(2, "GROUP"),
    ROLE(3, "ROLE"),
    ;

    private int code;
    private String value;

    SendMode(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
