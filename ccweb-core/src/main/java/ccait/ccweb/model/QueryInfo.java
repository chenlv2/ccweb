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


import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.dynamic.DynamicClassBuilder;
import ccait.ccweb.enums.PrivilegeScope;
import ccait.generator.EntitesGenerator;
import entity.query.*;
import entity.query.core.DataSource;
import entity.query.core.DataSourceFactory;
import entity.query.enums.Function;
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
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.StaticVars.CURRENT_DATASOURCE;
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

    private PageInfo pageInfo;
    private List<ConditionInfo> conditionList;
    private List<SortInfo> sortList;
    private List<String> groupList;
    private List<FieldInfo> keywords;
    private List<TableInfo> joinTables;
    private List<SelectInfo> selectList;
    private Map<String, Object> data;

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }


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

        where = ensureWhereQuerable(where, fields, privilegeScope, entity, tablename, alias);

        return where;
    }


    /***
     * 获取查询条件
     * @param tableList
     * @return
     */
    public Where getWhereQuerableByJoin(List<TableInfo> tableList, On on) throws Exception {

        if(tableList == null) {
            throw new IllegalAccessException("condition can not be empty!!!");
        }

        Where result = on.where("1=1");
        for(TableInfo table : tableList) {
            table.setFields(EntityContext.getFields(table.getEntity()));

            Where where = (Where) ReflectionUtils.invoke(table.getEntity().getClass(), table.getEntity(), "where", "1=1");

            where = ensureWhereQuerable(where, table.getFields(), table.getPrivilegeScope(), table.getEntity(),
                    table.getTablename(), table.getAlias());

            String strWhere = where.toString().replaceAll("[\\s]*1=1[\\s,]*(OR|AND|or|and)", "");

            if(StringUtils.isEmpty(strWhere)) {
                continue;
            }

            if(result.toString().indexOf(strWhere) > -1) {
                continue;
            }

            result.and(strWhere);
        }

        return result;
    }

    public QueryableAction getSelectQuerable(GroupBy groupBy) {

        if(this.getSelectList() == null || this.getSelectList().size() < 1) {
            return groupBy;
        }

        List<String> list = new ArrayList<String>();
        for(SelectInfo info : this.getSelectList()) {
            if(StringUtils.isEmpty(info.getField())) {
                continue;
            }

            String field = info.getField();
            if(!Pattern.matches("^\\w[A-Za-z0-9_]*$", field)) {
                continue;
            }

            String regSqlInject = "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|"
                    + "(\\b(select|update|union|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)";

            if(Pattern.matches(regSqlInject, field)) {
                continue;
            }

            if(info.getFunction() == Function.NONE) {
                list.add(field);
            }

            else {
                list.add(String.format("%s(%s)", info.getFunction().getValue(), field));
            }
        }

        if(list.size() < 1) {
            return groupBy;
        }

        return groupBy.select(StringUtils.join(", ", list.toArray()));
    }

    public QueryableAction getSelectQuerable(OrderBy orderBy) {

        if(this.getSelectList() == null || this.getSelectList().size() < 1) {
            return orderBy;
        }

        List<String> list = new ArrayList<String>();
        for(SelectInfo info : this.getSelectList()) {
            if(StringUtils.isEmpty(info.getField())) {
                continue;
            }

            String field = info.getField();
            if(!Pattern.matches("^\\w[A-Za-z0-9_]*$", field)) {
                continue;
            }

            String regSqlInject = "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|"
                    + "(\\b(select|update|union|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)";

            if(Pattern.matches(regSqlInject, field)) {
                continue;
            }

            if(info.getFunction() == Function.NONE) {
                list.add(field);
            }

            else {
                list.add(String.format("%s(%s)", info.getFunction().getValue(), field));
            }
        }

        if(list.size() < 1) {
            return orderBy;
        }

        return orderBy.select(StringUtils.join(", ", list.toArray()));
    }

    public QueryableAction getSelectQuerable(Where where) {

        if(this.getSelectList() == null || this.getSelectList().size() < 1) {
            return where;
        }

        List<String> list = new ArrayList<String>();
        for(SelectInfo info : this.getSelectList()) {
            if(StringUtils.isEmpty(info.getField())) {
                continue;
            }

            String field = info.getField();
            if(!Pattern.matches("^\\w[A-Za-z0-9_]*$", field)) {
                continue;
            }

            String regSqlInject = "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|"
                    + "(\\b(select|update|union|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)";

            if(Pattern.matches(regSqlInject, field)) {
                continue;
            }

            if(info.getFunction() == Function.NONE) {
                list.add(field);
            }

            else {
                if(null == info.getFunction()) {
                    list.add(field);
                }

                else {
                    list.add(String.format("%s(%s)", info.getFunction().getValue(), field));
                }
            }
        }

        if(list.size() < 1) {
            return where;
        }

        return where.select(StringUtils.join(", ", list.toArray()));
    }

    private Where ensureWhereQuerable(Where where, List<Field> fields, PrivilegeScope privilegeScope, Object entity,
                                      String tablename, String alias) throws Exception {
        if(this.getConditionList() != null) {
            for(ConditionInfo info : this.getConditionList()) {
                Optional<Field> opt = fields.stream().filter(a->a.getName().equals(info.getName())).findFirst();
                if(!opt.isPresent()) {
                    continue;
                }

                if(info.getValue() == null ||
                        info.getValue().toString().trim().equals("")) {
                    continue;
                }

                Field fld = opt.get();
                String column = DBUtils.getSqlInjValue(fld.getName()).replaceAll("\\s", "");
                switch (info.getAlgorithm()) {
                    case LIKE:
                        where.and(String.format("[%s]", column) + " LIKE '%#{"+fld.getName()+"}%'");
                        break;
                    case START:
                        where.and(String.format("[%s]", column) + " LIKE '%#{"+fld.getName()+"}'");
                        break;
                    case END:
                        where.and(String.format("[%s]", column) + " LIKE '#{"+fld.getName()+"}'");
                        break;
                    default:
                        where.and(String.format("[%s]%s#{%s}", column, info.getAlgorithm().getValue(), fld.getName()));
                }
                ReflectionUtils.setFieldValue(entity, fld.getName(), cast(fld.getType(), info.getValue().toString()));
            }
        }

        if(this.getKeywords() != null && this.getKeywords().size() > 0) {

            StringBuffer sb = new StringBuffer();
            boolean isFirst = true;
            for(FieldInfo info : this.getKeywords()) {
                Optional<Field> opt = fields.stream().filter(a->a.getName().equals(info.getName())).findFirst();
                if(!opt.isPresent()) {
                    continue;
                }

                Field fld = opt.get();
                String column =fld.getName();

                if(info.getValue() == null || info.getValue().toString().trim().equals("")) {
                    continue;
                }

                if(!isFirst) {
                    sb.append(" OR ");
                }

                if(info.getValue().getClass().equals(String.class)) {

                    sb.append(String.format("[%s] LIKE '%s'", fld.getName(), "%"+ DBUtils.getSqlInjValue(info.getValue()) +"%"));
                }

                else {
                    sb.append(String.format("[%s]='%s'", fld.getName(), info.getValue()));
                }
                isFirst = false;
            }

            if(sb.length() > 0) {
                where.and(String.format("(%s)", sb.toString()));
            }
        }

        if(privilegeScope == null) {
            return where;
        }

        String datasourceId = (String) ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE);

        //控制查询权限
        DataSource dataSource = DataSourceFactory.getInstance().getDataSource(datasourceId);

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
            list.add(DBUtils.getSqlInjValue(group.trim()));
        }

        if(list.size() < 1) {
            return null;
        }

        return where.groupby(list.toArray(new String[]{}));
    }

    public OrderBy getOrderByQuerable(Where where) {

        if(this.getSortList() == null || this.getSortList().size() < 1) {
            return null;
        }

        List<String> list = new ArrayList<String>();
        for(SortInfo sort : this.getSortList()) {

            if(StringUtils.isEmpty(sort.getName().trim())) {
                continue;
            }
            list.add(String.format("[%s] %s", DBUtils.getSqlInjValue(sort.getName().trim()), sort.isDesc() ? "DESC" : "ASC"));
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
            list.add(String.format("[%s] %s", DBUtils.getSqlInjValue(sort.getName().trim()), sort.isDesc() ? "DESC" : "ASC"));
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

    public List<TableInfo> getJoinTables() {
        return joinTables;
    }

    public void setJoinTables(List<TableInfo> joinTables) {
        this.joinTables = joinTables;
    }

    public List<SelectInfo> getSelectList() {
        return selectList;
    }

    public void setSelectList(List<SelectInfo> selectList) {
        this.selectList = selectList;
    }
}
