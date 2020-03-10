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

import entity.query.Queryable;
import entity.query.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
@Tablename("${entity.table.user}")
public class UserModel extends Queryable<UserModel> {

  @AutoIncrement
  @Fieldname("id")
  private Integer id;

  @PrimaryKey
  @Fieldname("username")
  private String username;

  @Fieldname("password")
  private String password;

  @Fieldname("${entity.table.reservedField.userPath:userPath}")
  private String path;

  @Fieldname("${entity.table.reservedField.createOn:createOn}")
  private Date createOn;

  @Fieldname("${entity.table.reservedField.createBy:createBy}")
  private Integer createBy;

  @Fieldname("${entity.table.reservedField.modifyOn:modifyOn}")
  private Date modifyOn;

  @Fieldname("${entity.table.reservedField.modifyBy:modifyBy}")
  private Integer modifyBy;

  @Fieldname("status")
  private Integer status;

  @Exclude
  private String token;

  public Date getCreateOn() {
    return createOn;
  }

  public void setCreateOn(Date createOn) {
    this.createOn = createOn;
  }

  public Integer getCreateBy() {
    return createBy;
  }

  public void setCreateBy(Integer createBy) {
    this.createBy = createBy;
  }

  public Date getModifyOn() {
    return modifyOn;
  }

  public void setModifyOn(Date modifyOn) {
    this.modifyOn = modifyOn;
  }

  public Integer getModifyBy() {
    return modifyBy;
  }

  public void setModifyBy(Integer modifyBy) {
    this.modifyBy = modifyBy;
  }

  public void setUserGroupRoleModels(List<UserGroupRoleModel> userGroupRoleModels) {
    this.userGroupRoleModels = userGroupRoleModels;
  }

  @Exclude
  private List<UserGroupRoleModel> userGroupRoleModels;

  @Exclude
  private List<RoleModel> roleModels;

  public List<UserGroupRoleModel> getUserGroupRoleModels() throws SQLException {

    if(userGroupRoleModels != null) {
      return userGroupRoleModels;
    }

    if(getId().compareTo(Integer.valueOf(0)) < 1) {
      return userGroupRoleModels;
    }

    return getUserGroupRoleModels(this.id);
  }

  public List<UserGroupRoleModel> getUserGroupRoleModels(long userId) throws SQLException {

    UserGroupRoleModel model = new UserGroupRoleModel();
    model.setUserId(userId);

    userGroupRoleModels = model.where("[userId]=#{userId}").orderby("createOn desc").query();

    List<String> roleIdList = userGroupRoleModels.stream().filter(a-> a.getRoleId() != null)
            .map(b-> b.getRoleId().toString().replace("-", ""))
            .collect(Collectors.toList());

    RoleModel roleModel = new RoleModel();

    roleModels = roleModel.where("roleId in (%s)", roleIdList).query();

    return userGroupRoleModels;
  }

  public Integer getId() {
    return this.id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUsername() {
    return this.username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return this.password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPath() {
    return this.path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Integer getStatus() {
    return this.status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
