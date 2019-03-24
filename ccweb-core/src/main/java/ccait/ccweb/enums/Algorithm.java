/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.enums;


public enum Algorithm {

    OR(0, " OR "),
    AND(1, " AND "),
    EQ(2, "="),
    GT(3, ">"),
    LT(4, "<"),
    GTEQ(5, ">="),
    LTEQ(6, "<="),
    NOT(7, "<>"),
    NOTEQ(8, "!="),
    LIKE(9, " LIKE "),
    ;

    private int code;
    private String value;

    Algorithm(int code, String value) {
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
