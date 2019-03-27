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


import ccait.ccweb.context.EntityContext;
import ccait.ccweb.enums.PrivilegeScope;
import ccait.generator.EntitesGenerator;
import entity.query.GroupBy;
import entity.query.OrderBy;
import entity.query.QueryableAction;
import entity.query.Where;
import entity.query.core.ConnectionFactory;
import entity.query.core.DataSource;
import entity.tool.util.DBUtils;
import entity.tool.util.ReflectionUtils;
import entity.tool.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.StaticVars.LOGIN_KEY;
import static entity.tool.util.StringUtils.cast;
import static entity.tool.util.StringUtils.join;

@Component
public class QueryInfo implements Serializable {


    private static QueryInfo context;
    @PostConstruct
    public void init() {
        context = this;
        context.request = this.request;
        // 初使化时将已静态化的request实例化
    }

    @Autowired
    protected HttpServletRequest request;

    @Value("${entity.table.reservedField.userPath:userPath}")
    private String userPathField;

    @Value("${entity.table.reservedField.createBy:createBy}")
    private String createByField;

    @Value("${entity.table.reservedField.groupId:groupId}")
    private String groupIdField;


    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    public List<ConditionInfo> getConditionList() {
        return conditionList;
    }

    public void setConditionList(List<ConditionInfo> conditionList) {
        this.conditionList = conditionList;
    }

    public List<SortInfo> getSortList() {
        return sortList;
    }

    public void setSortList(List<SortInfo> sortList) {
        this.sortList = sortList;
    }

    public List<String> getGroupList() {
        return groupList;
    }

    public void setGroupList(List<String> groupList) {
        this.groupList = groupList;
    }

    public List<FieldInfo> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<FieldInfo> keywords) {
        this.keywords = keywords;
    }

    public List<ConditionInfo> getOn() {
        return on;
    }

    public void setOn(List<ConditionInfo> on) {
        this.on = on;
    }

    private PageInfo pageInfo;
    private List<ConditionInfo> conditionList;
    private List<SortInfo> sortList;
    private List<String> groupList;
    private List<FieldInfo> keywords;
    private List<ConditionInfo> on;

    /***
     * 获取查询条件
     * @param entity
     * @return
     */
    public Where getWhereQuerable(String tablename, Object entity, PrivilegeScope privilegeScope) throws Exception {
        return getWhereQuerable(tablename, null, entity, privilegeScope);
    }
    
    /***
     * 获取查询条件
     * @param entity
     * @return
     */
    public Where getWhereQuerable(String tablename, String alias, Object entity, PrivilegeScope privilegeScope) throws Exception {

        List<Field> fields = EntityContext.getFields(entity);

        Where where = (Where) ReflectionUtils.invoke(entity.getClass(), entity, "where", "1=1");

        if(this.getConditionList() != null) {
            for(ConditionInfo info : this.getConditionList()) {
                Optional<Field> opt = fields.stream().filter(a->a.getName().equals(info.getName())).findFirst();
                if(!opt.isPresent()) {
                    continue;
                }

                Field fld = opt.get();
                String column =fld.getName();
                where.and(String.format("[%s]%s#{%s}", column, info.getAlgorithm().getValue(), fld.getName()));
                ReflectionUtils.setFieldValue(entity, fld.getName(), cast(fld.getType(), info.getValue().toString()));
            }
        }

        if(this.getKeywords() != null && this.getKeywords().size() > 0) {

            StringBuffer sb = new StringBuffer();
            int i = 0;
            for(FieldInfo info : this.getKeywords()) {
                Optional<Field> opt = fields.stream().filter(a->a.getName().equals(info.getName())).findFirst();
                if(!opt.isPresent()) {
                    continue;
                }

                Field fld = opt.get();
                String column =fld.getName();

                if(i > 0) {
                    sb.append(" OR ");
                }

                if(info.getValue().getClass().equals(String.class)) {

                    sb.append(String.format("[%s] LIKE %s", fld.getName(), "%"+ DBUtils.getSqlInjValue(info.getValue()) +"%"));
                }

                else {
                    sb.append(String.format("[%s]='%s'", fld.getName(), info.getValue()));
                }
                i++;
            }

            where.and(String.format("(%s)", sb.toString()));
        }

        if(privilegeScope == null) {
            return where;
        }

        //控制查询权限
        DataSource dataSource = ConnectionFactory.getDataSource(entity.getClass());

        UserModel user = (UserModel)context.request.getSession().getAttribute(context.request.getSession().getId() + LOGIN_KEY);

        String createByFieldString = String.format("[%s]", context.createByField);
        String userPathFieldString = String.format("[%s]", context.userPathField);
        String groupIdFieldString = String.format("[%s]", context.groupIdField);

        if(StringUtils.isNotEmpty(alias)) {
            createByFieldString = String.format("[%s].[%s]", alias, context.createByField);
            userPathFieldString = String.format("[%s].[%s]", alias, context.userPathField);
            groupIdFieldString = String.format("[%s].[%s]", alias, context.groupIdField);
        }

        switch(privilegeScope) {
            case DENIED:
                where = where.and("1=2");
                break;
            case SELF:
                if(!EntitesGenerator.hasColumn(dataSource.getId(), tablename, createByFieldString)) {
                    where = where.and(String.format("%s=%s", createByFieldString, user.getId()));
                    break;
                }

                if(!EntitesGenerator.hasColumn(dataSource.getId(), tablename, userPathFieldString)) {
                    where = where.and(String.format("%s='%s'", userPathFieldString, user.getPath()));
                    break;
                }

                break;
            case CHILD:

                if(!EntitesGenerator.hasColumn(dataSource.getId(), tablename, userPathFieldString)) {
                    where = where.and(String.format("%s LIKE '%s'", userPathFieldString, user.getPath() + "%"));
                    break;
                }

                break;
            case PARENT_AND_CHILD:
                if(!EntitesGenerator.hasColumn(dataSource.getId(), tablename, userPathFieldString)) {
                    String parentPath = user.getPath().substring(0, user.getPath().lastIndexOf("/"));
                    where = where.and(String.format("%s LIKE '%s'", userPathFieldString, parentPath + "%"));
                }
                break;
            case GROUP:

                if(!EntitesGenerator.hasColumn(dataSource.getId(), tablename, context.groupIdField)) {
                    break;
                }

                List<String> groupIdList = user.getUserGroupRoleModels().stream()
                        .map(a->a.getGroupId().toString().replace("-", ""))
                        .collect(Collectors.toList());

                //查询非公开数据
                where = where.and(String.format("%s is not null AND %s in ('%s')",
                        groupIdFieldString, groupIdFieldString, join("', '", groupIdList)));
                break;
        }

        return where;
    }

    public Where getWhereQueryableById(Object entity, String id) throws Exception {
        PrimaryKeyInfo pk = EntityContext.getPrimaryKeyInfo(entity);
        if(pk == null) {
            throw new Exception("Can not find primary key!!!");
        }

        ReflectionUtils.invoke(entity.getClass(), entity, pk.getSetter(), cast(pk.getField().getType(), id));

        Where where = (Where) ReflectionUtils.invoke(entity.getClass(), entity, "where",
                String.format("[%s]=#{%s}", pk.getColumnName(), pk.getField().getName()));

        return where;
    }

    public GroupBy getGroupByQuerable(Where where) {
        if(this.getGroupList() == null || this.getGroupList().size() < 1) {
            return null;
        }

        List<String> list = new ArrayList<String>();
        for(String group : this.getGroupList()) {

            if(StringUtils.isEmpty(group.trim())) {
                continue;
            }
            list.add(group.trim());
        }

        if(list.size() < 1) {
            return null;
        }

        return where.groupby(list.toArray(new String[]{}));
    }

    public QueryableAction getOrderByQuerable(Where where) {

        if(this.getSortList() == null || this.getSortList().size() < 1) {
            return where;
        }

        List<String> list = new ArrayList<String>();
        for(SortInfo sort : this.getSortList()) {

            if(StringUtils.isEmpty(sort.getName().trim())) {
                continue;
            }
            list.add(String.format("[%s] %s", sort.getName().trim(), sort.isDesc() ? "DESC" : "ASC"));
        }

        if(list.size() < 1) {
            return null;
        }

        return where.orderby(list.toArray(new String[]{}));
    }

    public OrderBy getOrderByQuerable(GroupBy groupBy) {

        if(groupBy == null) {
            return null;
        }

        if(this.getSortList() == null || this.getSortList().size() < 1) {
            return null;
        }

        List<String> list = new ArrayList<String>();
        for(SortInfo sort : this.getSortList()) {

            if(StringUtils.isEmpty(sort.getName().trim())) {
                continue;
            }
            list.add(String.format("[%s] %s", sort.getName().trim(), sort.isDesc() ? "DESC" : "ASC"));
        }

        if(list.size() < 1) {
            return null;
        }

        return groupBy.orderby(list.toArray(new String[]{}));
    }

    public int getSkip() {

        if(this.getPageInfo().getPageIndex() > 0 && getPageInfo().getPageSize() > 0) {
            return this.getPageInfo().getPageIndex() * this.getPageInfo().getPageSize() - this.getPageInfo().getPageSize();
        }

        return 0;
    }

    public Connection getConnection(Object entity) {
        return (Connection) ReflectionUtils.invoke(entity.getClass(), entity, "connection");
    }
}
