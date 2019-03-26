/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.interceptor;

import ccait.ccweb.annotation.AccessCtrl;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.TriggerContext;
import ccait.ccweb.controllers.BaseController;
import ccait.ccweb.enums.EventType;
import ccait.ccweb.enums.PrivilegeScope;
import ccait.ccweb.filter.RequestWrapper;
import ccait.ccweb.model.*;
import ccait.ccweb.utils.FastJsonUtils;
import entity.query.ColumnInfo;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.NetworkUtils.getClientIp;
import static ccait.ccweb.utils.StaticVars.*;

/**
 * @description 访问权限拦截器
 */
public class SecurityInterceptor implements HandlerInterceptor {

    @Value("${entity.security.admin.username:admin}")
    private String admin;

    @Value("${entity.security.admin.password:}")
    private String password;

    @Value("${entity.ip.whiteList:}")
    private String whiteListText;

    @Value("${entity.ip.blackList:}")
    private String blackListText;

    @Value("${entity.table.maxJoin:5}")
    private int maxJoin;

//    @Value(TABLE_USER)
//    private String userTablename;

    private static final Logger log = LogManager.getLogger( SecurityInterceptor.class );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        request.getSession().setAttribute(request.getSession().getId() +
                CURRENT_MAX_PRIVILEGE_SCOPE, PrivilegeScope.DENIED);

        // 验证权限
        if (allowIp(request) && this.hasPermission(handler, request.getMethod(), request)) {
            return true;
        }

        //  null == request.getHeader("x-requested-with") TODO 暂时用这个来判断是否为ajax请求
        // 如果没有权限 则抛403异常 springboot会处理，跳转到 /error/403 页面
        response.sendError(HttpStatus.FORBIDDEN.value(), "没有足够的权限访问请求的内容");
        return false;
    }

    private boolean allowIp(HttpServletRequest request) {

        List<String> whiteList = StringUtils.splitString2List(whiteListText, ",");
        List<String> blackList = StringUtils.splitString2List(blackListText, ",");

        RequestWrapper rw = (RequestWrapper)request;

        if(StringUtils.isEmpty(getClientIp(request))) {
            return false;
        }

        if(whiteList.size() > 0) {
            if (whiteList.contains(getClientIp(request))) {
                return true;
            }

            else {
                return false;
            }
        }

        if(blackList.size() > 0) {
            if (blackList.contains(getClientIp(request))) {
                return false;
            }
        }

        return true;
    }

    /**
     * 是否有权限
     *
     * @param handler
     * @return
     */
    private boolean hasPermission(Object handler, String method, HttpServletRequest request) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 获取方法上的注解
            AccessCtrl requiredPermission = handlerMethod.getMethod().getAnnotation(AccessCtrl.class);
            // 如果方法上的注解为空 则获取类的注解
            if (requiredPermission == null) {
                requiredPermission = handlerMethod.getMethod().getDeclaringClass().getAnnotation(AccessCtrl.class);
            }

            boolean x = true;
            Map<String, String > attrs = (Map<String, String>) request.getAttribute(VARS_PATH);
            String table = attrs.get("table");
            if(StringUtils.isNotEmpty(table)) {
                x = canAccessTable(method, request, requiredPermission, attrs, table);
            }

            else {
                List<String> tableList = new ArrayList<String>();
                for(int i=0; i<maxJoin; i++) {

                    if(!x) {
                        break;
                    }

                    String key = String.format("table%s", i);
                    if(StringUtils.isEmpty(attrs.get(key))) {
                        continue;
                    }

                    table = attrs.get(key);
                    x = x && canAccessTable(method, request, requiredPermission, attrs, table);
                }
            }

            return x;
        }
        return true;
    }

    private Boolean canAccessTable(String method, HttpServletRequest request, AccessCtrl requiredPermission, Map<String, String> attrs, String table) throws Exception {
        String postString = BaseController.getRequestPostString(request);

        ApplicationContext.getThreadLocalMap().put(CURRENT_TABLE, table);

        if(requiredPermission == null) {
            return true;
        }

        //先执行触发器
        runTrigger(table, method, attrs, postString, request);

        // 如果标记了注解，则判断权限
        if (requiredPermission != null) {

            // redis或数据库或session 中获取该用户的权限信息 并判断是否有权限
            UserModel user = (UserModel)request.getSession().getAttribute(request.getSession().getId() + LOGIN_KEY);

            if( user != null && user.getUsername().equals(admin) ) { //超级管理员
                log.info(String.format(LOG_PRE_SUFFIX + "超级管理员访问表[%s]，操作：%s！", table, method));

                request.getSession().setAttribute(request.getSession().getId() +
                        CURRENT_MAX_PRIVILEGE_SCOPE, PrivilegeScope.ALL);
                return true;
            }

            if(Pattern.matches("^/api/[^/]+/build$", request.getRequestURI())) {
                return false;
            }

            AclModel acl = new AclModel();
            acl.setTableName(table);
            List<AclModel> aclList = acl.where("[tableName]=#{tableName}").query();
            if(aclList == null || aclList.size() < 1) {

                if(method.equals("GET") || method.equals("POST")) {
                    log.info(String.format(LOG_PRE_SUFFIX + "表[%s]没有设置权限，允许查询！", table));
                    return true;
                }

                log.info(String.format(LOG_PRE_SUFFIX + "表[%s]没有设置权限，不允许增删改！", table));
                return false;
            }

            List<UUID> groupIds = aclList.stream().map(a->a.getGroupId()).collect(Collectors.toList());

            if(StringUtils.isNotEmpty(requiredPermission.groupName()) &&
                    !groupIds.contains(requiredPermission.groupName()) ) { //不属于指定组
                log.warn(String.format(LOG_PRE_SUFFIX + "表[%s]的群组不属于%s！", table, requiredPermission.groupName()));
                return false;
            }

            if(user == null) {
                log.warn(LOG_PRE_SUFFIX + "用户未登录！！！");
                return false;
            }

            boolean hasGroup = false;
            for (UUID groupId : groupIds) {
                Optional<UserGroupRoleModel> opt = user.getUserGroupRoleModels().stream()
                        .filter(a -> a.getGroup().getGroupId().equals(groupId)).findAny();

                if(opt != null && opt.isPresent()) {
                    hasGroup = true;
                    break;
                }
            }

            if( !hasGroup ) {
                log.warn(String.format(LOG_PRE_SUFFIX + "用户[%s]的群组于表%s中不存在！", user.getUsername(), table));
                return false;
            }

            List<UUID> aclIds = aclList.stream().map(a->a.getAclId()).collect(Collectors.toList());
            if(!checkPrivilege(user, aclIds, method, attrs, postString, request)){
                log.warn(String.format(LOG_PRE_SUFFIX + "用户[%s]对表%s没有%s的操作权限！", user.getUsername(), table, method));
                return false;
            }

        }
        return null;
    }

    private boolean checkPrivilege(UserModel user, List<UUID> aclIds, String method, Map<String, String > attrs,
                                   String postString, HttpServletRequest request) throws Exception {

        String privilegeWhere = null;
        switch (method.toUpperCase()) {
            case "GET":
                privilegeWhere = "(canQuery=1 OR canList=1 OR canView=1 OR canDecrypt=1)";
                break;
            case "POST":
                if(Pattern.matches("^/api/[^/]+/build$", request.getRequestURI())) {
                    break;
                }

                QueryInfo queryInfo = FastJsonUtils.convert(postString, QueryInfo.class);
                if(queryInfo.getKeywords().size() > 0 || queryInfo.getConditionList().size() > 0) {
                    privilegeWhere = "canQuery=1";
                }

                else {
                    privilegeWhere = "canList=1";
                }
                break;
            case "PUT":
                if(StringUtils.isEmpty(attrs.get("id"))) {
                    privilegeWhere = "canAdd=1";
                }

                else {
                    privilegeWhere = "canUpdate=1";
                }
                break;
            case "DELETE":
                privilegeWhere = "canDelete=1";
                break;
        }

        if(StringUtils.isEmpty(privilegeWhere)) {
            return false;
        }

        PrivilegeModel privilege = new PrivilegeModel();
        List<PrivilegeModel> privilegeList = privilege.where("[roleId]=#{RoleId}").and(privilegeWhere)
                .and(String.format("aclId in ('%s')", String.join("','",
                        aclIds.stream().map(a->a.toString().replace("-", ""))
                        .collect(Collectors.toList())))).query();


        boolean result = false;
        PrivilegeScope currentMaxScope = PrivilegeScope.DENIED;
        for (UserGroupRoleModel groupRole : user.getUserGroupRoleModels()) {
            Optional<PrivilegeModel> opt = privilegeList.stream().filter(a -> a.getRoleId().equals(groupRole.getRoleId()) &&
                    a.getGroupId().equals(groupRole.getGroupId())).max(Comparator.comparing(b->b.getScope().getCode()));

            if(opt != null && opt.isPresent()) {

                if(opt.get().getScope().getCode() > currentMaxScope.getCode()) {
                    currentMaxScope = opt.get().getScope(); //求出最大权限
                }

                result = true;
            }
        }

        request.getSession().setAttribute(request.getSession().getId() +
                CURRENT_MAX_PRIVILEGE_SCOPE, currentMaxScope);

        if(currentMaxScope.equals(PrivilegeScope.DENIED)) {
            return false;
        }

        return  result;
    }

    private void runTrigger(String table, String method, Map<String, String > attrs, String postString, HttpServletRequest request) throws Exception {

        switch (method.toUpperCase()) {
            case "GET":
                TriggerContext.exec(table, EventType.View, attrs.get("id"), request);
                break;
            case "POST":
                if(Pattern.matches("^/api/[^/]+/build$", request.getRequestURI())) {
                    List<ColumnInfo> columns = FastJsonUtils.toList(postString, ColumnInfo.class);
                    TriggerContext.exec(table, EventType.BuildTable, columns, request);
                    break;
                }

                QueryInfo queryInfo = FastJsonUtils.convert(postString, QueryInfo.class);
                if(queryInfo.getKeywords().size() > 0 || queryInfo.getConditionList().size() > 0) {
                    TriggerContext.exec(table, EventType.Query, queryInfo, request);
                }

                else {
                    TriggerContext.exec(table, EventType.List, queryInfo, request);
                }
                break;
            case "PUT":
                Map data = FastJsonUtils.convert(postString, Map.class);
                if(StringUtils.isEmpty(attrs.get("id"))) {
                    TriggerContext.exec(table, EventType.Insert, data, request);
                }

                else {
                    TriggerContext.exec(table, EventType.Update, data, request);
                }
                break;
            case "DELETE":
                if(StringUtils.isNotEmpty(attrs.get("id"))) {
                    TriggerContext.exec(table, EventType.Delete, attrs.get("id"), request);
                }

                else {
                    TriggerContext.exec(table, EventType.Delete, FastJsonUtils.convert(postString, List.class), request);
                }
                break;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // TODO
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // TODO
    }
}
