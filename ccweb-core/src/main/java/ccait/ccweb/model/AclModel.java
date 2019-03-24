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

import java.util.UUID;

@Component
@Scope("prototype")
@Tablename("${entity.table.acl}")
public class AclModel extends Queryable<AclModel> {
  @PrimaryKey
  @Fieldname("aclId")
  private UUID aclId;

  @Fieldname("groupId")
  private UUID groupId;

  @Fieldname("tableName")
  private String tableName;

  public UUID getAclId() {
    return this.aclId;
  }

  public void setAclId(UUID aclId) {
    this.aclId = aclId;
  }

  public UUID getGroupId() {
    return this.groupId;
  }

  public void setGroupId(UUID groupId) {
    this.groupId = groupId;
  }

  public String getTableName() {
    return this.tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }
}
