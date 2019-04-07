package ccait.ccweb.model;


import entity.query.Queryable;
import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.annotation.Tablename;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.UUID;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;

@Component
@Scope("prototype")
@Tablename("${entity.table.userGroupRole}")
public class UserGroupRoleModel extends Queryable<UserGroupRoleModel> {

    private static final Logger log = LogManager.getLogger( UserGroupRoleModel.class );

    @PrimaryKey
    @Fieldname("userGroupRoleId")
    private String userGroupRoleId;

    @Fieldname("userId")
    private Long userId;

    @Fieldname("groupId")
    private UUID groupId;

    @Fieldname("roleId")
    private UUID roleId;

    @Fieldname("${entity.table.reservedField.userPath:userPath}")
    private String path;

    private GroupModel group;
    public GroupModel getGroup() {

        if(group != null) {
            return group;
        }

        if(this.groupId == null) {
            return group;
        }

        group = new GroupModel();
        group.setGroupId(this.groupId);

        try {
            group = group.where("[userId]=#{userId}").first();
        } catch (SQLException e) {
            log.error(LOG_PRE_SUFFIX + e.getMessage(), e);
        }

        return group;
    }

    private RoleModel role;
    public RoleModel getRole() {

        if(role != null) {
            return role;
        }

        if(this.roleId == null) {
            return role;
        }

        role = new RoleModel();
        role.setRoleId(this.roleId);

        try {
            role = role.where("[userId]=#{userId}").first();
        } catch (SQLException e) {
            log.error(LOG_PRE_SUFFIX + e.getMessage(), e);
        }

        return role;
    }

    public String getUserGroupRoleId() {
        return userGroupRoleId;
    }

    public void setUserGroupRoleId(String userGroupRoleId) {
        this.userGroupRoleId = userGroupRoleId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}
