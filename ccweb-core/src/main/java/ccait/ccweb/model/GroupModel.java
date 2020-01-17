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
import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.annotation.Tablename;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
@Scope("prototype")
@Tablename("${entity.table.group}")
public class GroupModel extends Queryable<GroupModel> {
  @PrimaryKey
  @Fieldname("groupId")
  private UUID groupId;

  @Fieldname("groupName")
  private String groupName;

  @Fieldname("description")
  private String description;

  @Fieldname("${entity.table.reservedField.userPath:userPath}")
  private String path;

  public Date getCreateOn() {
    return createOn;
  }

  public void setCreateOn(Date createOn) {
    this.createOn = createOn;
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

  @Fieldname("${entity.table.reservedField.createOn:createOn}")
  private Date createOn;

  @Fieldname("${entity.table.reservedField.createBy:createBy}")
  private Integer createBy;

  @Fieldname("${entity.table.reservedField.modifyOn:modifyOn}")
  private Date modifyOn;

  @Fieldname("${entity.table.reservedField.modifyBy:modifyBy}")
  private Integer modifyBy;

  public UUID getGroupId() {
    return this.groupId;
  }

  public void setGroupId(UUID groupId) {
    this.groupId = groupId;
  }

  public String getGroupName() {
    return this.groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getCreateBy() {
    return createBy;
  }

  public void setCreateBy(Integer createBy) {
    this.createBy = createBy;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
