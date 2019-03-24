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

@Component
@Scope("prototype")
@Tablename("${entity.table.user}")
public class UserModel extends Queryable<UserModel> {

  @AutoIncrement
  @Fieldname("id")
  private Long id;

  @PrimaryKey
  @Fieldname("username")
  private String username;

  @Fieldname("password")
  private String password;

  @Fieldname("${entity.table.reservedField.userPath:userPath}")
  private String path;

  @Fieldname("createTime")
  private Date createTime;

  @Fieldname("status")
  private Integer status;

  @Fieldname("${entity.table.reservedField.createBy:createBy}")
  private Long createBy;

  @Exclude
  private List<UserGroupRoleModel> userGroupRoleModels;

  public List<UserGroupRoleModel> getUserGroupRoleModels() throws SQLException {

    if(userGroupRoleModels != null) {
      return userGroupRoleModels;
    }

    if(getId().compareTo(Long.valueOf(0)) < 1) {
      return userGroupRoleModels;
    }

    return getUserGroupRoleModels(this.id);
  }

  public List<UserGroupRoleModel> getUserGroupRoleModels(long userId) throws SQLException {

    UserGroupRoleModel model = new UserGroupRoleModel();
    model.setUserId(userId);

    userGroupRoleModels = model.where("[userId]=#{userId}").query();

    return userGroupRoleModels;
  }

  public Long getId() {
    return this.id;
  }

  public void setId(Long id) {
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

  public Date getCreateTime() {
    return this.createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Integer getStatus() {
    return this.status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Long getCreateBy() {
    return createBy;
  }

  public void setCreateBy(Long createBy) {
    this.createBy = createBy;
  }
}
