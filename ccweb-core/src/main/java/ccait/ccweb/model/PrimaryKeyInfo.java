/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.model;

import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.core.ApplicationConfig;

import java.lang.reflect.Field;

public class PrimaryKeyInfo {
    public PrimaryKey getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(PrimaryKey primaryKey) {
        this.primaryKey = primaryKey;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    private PrimaryKey primaryKey;
    private Field field;

    public String getSetter() {
        if(this.primaryKey == null || this.field == null) {
            return null;
        }

        return "set" + getField().getName().substring(0, 1).toUpperCase() + getField().getName().substring(1);
    }

    public String getColumnName() {
        if(this.field == null) {
            return null;
        }

        Fieldname ann = this.field.getAnnotation(Fieldname.class);
        if(ann == null) {
            return this.field.getName();
        }

        return ApplicationConfig.getInstance().get(ann.value());
    }
}
