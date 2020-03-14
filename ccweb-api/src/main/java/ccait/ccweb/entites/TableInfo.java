package ccait.ccweb.entites;

import ccait.ccweb.enums.PrivilegeScope;
import entity.query.enums.JoinMode;

import java.lang.reflect.Field;
import java.util.List;

public class TableInfo {
    public String getTablename() {
        return tablename;
    }

    public void setTablename(String tablename) {
        this.tablename = tablename;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public Object getEntity() {
        return entity;
    }

    public void setEntity(Object entity) {
        this.entity = entity;
    }

    public PrivilegeScope getPrivilegeScope() {
        return privilegeScope;
    }

    public void setPrivilegeScope(PrivilegeScope privilegeScope) {
        this.privilegeScope = privilegeScope;
    }

    public JoinMode getJoinMode() {
        return joinMode;
    }

    public void setJoinMode(JoinMode joinMode) {
        this.joinMode = joinMode;
    }

    public List<ConditionInfo> getOnList() {
        return onList;
    }

    public void setOnList(List<ConditionInfo> onList) {
        this.onList = onList;
    }

    private String tablename;
    private String alias;
    private List<Field> fields;
    private Object entity;
    private PrivilegeScope privilegeScope;
    private JoinMode joinMode;
    private List<ConditionInfo> onList;

}
