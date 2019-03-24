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

import ccait.ccweb.enums.PrivilegeScope;
import entity.query.Queryable;
import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.annotation.Tablename;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Scope("prototype")
@Tablename("${entity.table.privilege}")
public class PrivilegeModel extends Queryable<PrivilegeModel> {
  @PrimaryKey
  @Fieldname("privilegeId")
  private UUID privilegeId;

  @Fieldname("groupId")
  private UUID groupId;

  @Fieldname("roleId")
  private UUID roleId;

  @Fieldname("aclId")
  private UUID aclId;

  @Fieldname("canAdd")
  private Integer canAdd;

  @Fieldname("canDelete")
  private Integer canDelete;

  @Fieldname("canUpdate")
  private Integer canUpdate;

  @Fieldname("canQuery")
  private Integer canQuery;

  @Fieldname("scope")
  private PrivilegeScope scope;

  public UUID getPrivilegeId() {
    return this.privilegeId;
  }

  public void setPrivilegeId(UUID privilegeId) {
    this.privilegeId = privilegeId;
  }

  public UUID getRoleId() {
    return this.roleId;
  }

  public void setRoleId(UUID roleId) {
    this.roleId = roleId;
  }

  public UUID getAclId() {
    return this.aclId;
  }

  public void setAclId(UUID aclId) {
    this.aclId = aclId;
  }

  public Integer getCanAdd() {
    return this.canAdd;
  }

  public void setCanAdd(Integer canAdd) {
    this.canAdd = canAdd;
  }

  public Integer getCanDelete() {
    return this.canDelete;
  }

  public void setCanDelete(Integer canDelete) {
    this.canDelete = canDelete;
  }

  public Integer getCanUpdate() {
    return this.canUpdate;
  }

  public void setCanUpdate(Integer canUpdate) {
    this.canUpdate = canUpdate;
  }

  public Integer getCanQuery() {
    return this.canQuery;
  }

  public void setCanQuery(Integer canQuery) {
    this.canQuery = canQuery;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public void setGroupId(UUID groupId) {
    this.groupId = groupId;
  }

  public PrivilegeScope getScope() {
    return scope;
  }

  public void setScope(PrivilegeScope scope) {
    this.scope = scope;
  }
}
