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
import ccait.ccweb.enums.EventType;
import ccait.ccweb.enums.PrivilegeScope;
import ccait.ccweb.filter.RequestWrapper;
import ccait.ccweb.model.*;
import ccait.ccweb.utils.FastJsonUtils;
import ccait.ccweb.utils.UploadUtils;
import entity.query.ColumnInfo;
import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import sun.misc.BASE64Encoder;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    private Integer maxJoin;

    @Value("${entity.datasource:}")
    private String datasourceString;

    private static final Logger log = LogManager.getLogger( SecurityInterceptor.class );

    private boolean hasUploadFile;

    @PostConstruct
    private void construct() {
        admin = ApplicationConfig.getInstance().get("${entity.security.admin.username}", admin);
        password = ApplicationConfig.getInstance().get("${entity.security.admin.password}", password);
        maxJoin = Integer.parseInt(ApplicationConfig.getInstance().get("${entity.table.maxJoin}", maxJoin.toString()));
        datasourceString = ApplicationConfig.getInstance().get("${entity.datasource}", datasourceString);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if("yes".equals(response.getHeader("finish"))) {
            return true;
        }

        if(request instanceof RequestWrapper) {

            Map<String, String> attrs = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            InitLocalMap initLocalMap = new InitLocalMap(response, attrs).invoke();
            if (initLocalMap.is()) return false;

            String currentTable = initLocalMap.getCurrentTable();

            if (!vaildForUploadFiles((RequestWrapper) request, currentTable)) {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "无效的上传文件格式!!!");
                return false;
            }

            // 验证权限
            if (allowIp(request) &&
                    this.hasPermission(handler, request.getMethod(), request, attrs, currentTable)) {
                return true;
            }

            //  null == request.getHeader("x-requested-with") TODO 暂时用这个来判断是否为ajax请求
            // 如果没有权限 则抛403异常 springboot会处理，跳转到 /error/403 页面
            // response.sendError(HttpStatus.FORBIDDEN.value(), "没有足够的权限访问请求的内容");
            throw new Exception("没有足够的权限访问请求的内容");
        }
        else {
            return true;
        }
    }

    private boolean vaildForUploadFiles(RequestWrapper request, String table) throws Exception {

        if(request.getParameters() == null) {
            return true;
        }

        if(!(request.getParameters() instanceof HashMap)) {
            return true;
        }

        HashMap<String, Object> data = (HashMap) request.getParameters();
        List<Map.Entry<String, Object>> files = data.entrySet().stream()
                .filter(a -> a.getValue() instanceof byte[]).collect(Collectors.toList());
        if(files == null || files.size() < 1) {
            return true;
        }

        hasUploadFile = true;

        List<String> urlList = StringUtils.splitString2List(request.getRequestURI(), "/");
        if(urlList.get(urlList.size() - 1).toLowerCase() == "import") {
            return true;
        }

        String currentDatasource = "default";
        if(ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE) != null) {
            currentDatasource = ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE).toString();
        }

        Map<String, Object> uploadConfigMap = ApplicationConfig.getInstance().getMap(String.format("entity.upload.%s.%s", currentDatasource, table));
        if(uploadConfigMap == null || uploadConfigMap.size() < 1) {
            return false;
        }

        for(Map.Entry<String, Object> fileEntry : files) {

            String tempKey = String.format("%s_upload_filename", fileEntry.getKey());
            String filename = data.get(tempKey).toString();
            String[] arr = filename.split("\\.");
            String extName = arr[arr.length - 1];

            byte[] fileBytes = (byte[]) fileEntry.getValue();
            //MagicMatch mimeMatcher = Magic.getMagicMatch(fileBytes, true);
            String mimeType = UploadUtils.getMIMEType(extName); //mimeMatcher.getMimeType();

            if(StringUtils.isEmpty(mimeType)) {
                continue;
            }

            if(uploadConfigMap.containsKey(fileEntry.getKey()) && uploadConfigMap.get(fileEntry.getKey()) != null) {
                Map<String, Object> configMap = (Map<String, Object>) uploadConfigMap.get(fileEntry.getKey());
                if (configMap.get("mimeType") != null) {
                    if (!StringUtils.splitString2List(configMap.get("mimeType").toString(), ",").stream()
                            .filter(a -> extName.equalsIgnoreCase(a.toString().trim()))
                            .findAny().isPresent()) {
                        throw new Exception("Can not supported file type!!!");
                    }
                }

                if (configMap.get("maxSize") != null) {
                    if (fileBytes.length > 1024 * 1024 * Integer.parseInt(configMap.get("maxSize").toString())) {
                        throw new Exception("Upload field to be long!!!");
                    }
                }

                String value = null;
                if(configMap.get("path") != null) {
                    String root = configMap.get("path").toString();
                    if(root.lastIndexOf("/") == root.length() - 1 ||
                            root.lastIndexOf("\\") == root.length() - 1) {
                        root = root.substring(0, root.length() - 2);
                    }
                    root = String.format("%s/%s/%s/%s", root, currentDatasource, table, fileEntry.getKey());

                    value = UploadUtils.upload(root, filename, fileBytes);
                }

                else {

                    //返回Base64编码过的字节数组字符串
                    value = String.format("%s::%s::%s|::|%s", mimeType, "jpg", filename, new BASE64Encoder().encode(fileBytes));
                }

                data.put(fileEntry.getKey(), value);
                data.remove(tempKey);
            }
        }

        request.setPostParameter(data);

        return true;
    }

    private boolean allowIp(HttpServletRequest request) {

        whiteListText = ApplicationConfig.getInstance().get("${entity.ip.whiteList}", whiteListText);
        blackListText = ApplicationConfig.getInstance().get("${entity.ip.blackList}", blackListText);

        log.info("whiteList: " + whiteListText);
        log.info("blackList: " + blackListText);

        List<String> whiteList = StringUtils.splitString2List(whiteListText, ",");
        List<String> blackList = StringUtils.splitString2List(blackListText, ",");

        String accessIP = getClientIp(request);
        log.info("access ip =====>>> " + accessIP);
        if(StringUtils.isEmpty(accessIP)) {
            return false;
        }

        if(whiteList.size() > 0) {
            if (whiteList.contains(accessIP)) {
                return true;
            }

            else {
                return false;
            }
        }

        if(blackList.size() > 0) {
            if (blackList.contains(accessIP)) {
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
    private boolean hasPermission(Object handler, String method, HttpServletRequest request, Map<String, String> attrs, String table) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 获取方法上的注解
            AccessCtrl requiredPermission = handlerMethod.getMethod().getAnnotation(AccessCtrl.class);
            // 如果方法上的注解为空 则获取类的注解
            if (requiredPermission == null) {
                requiredPermission = handlerMethod.getMethod().getDeclaringClass().getAnnotation(AccessCtrl.class);
            }

            boolean x = true;
            if(StringUtils.isNotEmpty(table)) {
                x = canAccessTable(method, (RequestWrapper) request, requiredPermission, attrs, table);
            }

            else {
                RequestWrapper requestWarpper = (RequestWrapper)request;
                QueryInfo queryInfo = FastJsonUtils.convertJsonToObject((requestWarpper).getRequestPostString(), QueryInfo.class);
                List<String> tableList = new ArrayList<String>();
                if(queryInfo != null && queryInfo.getJoinTables() != null && queryInfo.getJoinTables().size() > 0) {
                    for (int i = 0; i < queryInfo.getJoinTables().size(); i++) {
                        if(StringUtils.isEmpty(queryInfo.getJoinTables().get(i).getTablename())) {
                            continue;
                        }
                        x = x && canAccessTable(method, (RequestWrapper) request, requiredPermission, attrs, queryInfo.getJoinTables().get(i).getTablename());
                    }
                }
            }

            return x;
        }
        return true;
    }

    private Boolean canAccessTable(String method, RequestWrapper request, AccessCtrl requiredPermission, Map<String, String> attrs, String table) throws Exception {

        if(requiredPermission == null) {
            ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + table, PrivilegeScope.ALL);
            return true;
        }

        //先执行触发器
        runTrigger(table, method, attrs, request.getRequestPostString(), request);

        // redis或数据库或session 中获取该用户的权限信息 并判断是否有权限
        UserModel user = (UserModel)request.getSession().getAttribute(request.getSession().getId() + LOGIN_KEY);

        if( user != null && user.getUsername().equals(admin) ) { //超级管理员
            log.info(String.format(LOG_PRE_SUFFIX + "超级管理员访问表[%s]，操作：%s！", table, method));
            ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + table, PrivilegeScope.ALL);
            return true;
        }

        if(Pattern.matches("^/(?i)(as)?(?i)api/[^/]+(/[^/]+)?/(?i)build$", request.getRequestURI())) {
            return false;
        }

        AclModel acl = new AclModel();
        acl.setTableName(table);
        List<AclModel> aclList = acl.where("[tableName]=#{tableName}").query();
        if(aclList == null || aclList.size() < 1) {

            if(method.equals("GET") || method.equals("POST") || (user != null && user.getUsername().equals(admin)) ) {
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
        if(!checkPrivilege(table, user, aclIds, method, attrs, request.getRequestPostString(), request)){
            log.warn(String.format(LOG_PRE_SUFFIX + "用户[%s]对表%s没有%s的操作权限！", user.getUsername(), table, method));
            return false;
        }

        return null;
    }

    private boolean checkPrivilege(String table, UserModel user, List<UUID> aclIds, String method, Map<String, String > attrs,
                                   String postString, HttpServletRequest request) throws Exception {

        String privilegeWhere = null;
        switch (method.toUpperCase()) {
            case "GET":
                if(Pattern.matches("^/(?i)(as)?(?i)api/(?i)download/[^/]+/[^/]+/[^/]+$", request.getRequestURI())) {
                    privilegeWhere = "canDownload=1 AND (canQuery=1 OR canList=1 OR canView=1 OR canDecrypt=1)";
                }

                else if(Pattern.matches("^/(?i)(as)?(?i)api/(?i)preview/[^/]+/[^/]+/[^/]+$", request.getRequestURI())) {
                    privilegeWhere = "canPreview=1 AND (canQuery=1 OR canList=1 OR canView=1 OR canDecrypt=1)";
                }

                else {
                    privilegeWhere = "(canQuery=1 OR canList=1 OR canView=1 OR canDecrypt=1)";
                }
                break;
            case "POST":
                if(Pattern.matches("^/(?i)(as)?(?i)api/[^/]+(/[^/]+)?/(?i)build$", request.getRequestURI())) {
                    break;
                }

                if(Pattern.matches("^/(?i)(as)?(?i)api/[^/]+(/[^/]+)?/(?i)update$", request.getRequestURI())) {
                    privilegeWhere = "canUpdate=1";
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
                String[] arr = request.getRequestURI().split("/");
                if(StringUtils.isEmpty(attrs.get("id")) && arr[arr.length - 1].toLowerCase() != "update") {
                    privilegeWhere = "canAdd=1";
                }

                else {
                    privilegeWhere = "canUpdate=1";
                }

                if(hasUploadFile) {
                    privilegeWhere += " AND canUpload=1";
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
                .and(String.format("(aclId in ('%s') OR aclId IS NULL OR aclId='')", String.join("','",
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

        ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + table, currentMaxScope);

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
                Pattern tablePattern = Pattern.compile("^/(api|asyncapi)(/[^/]+){1,2}/build/table$", Pattern.CASE_INSENSITIVE);
                Pattern viewPattern = Pattern.compile("^/(api|asyncapi)(/[^/]+){1,2}/build/view$", Pattern.CASE_INSENSITIVE);
                Pattern uploadPattern = Pattern.compile("^/(api|asyncapi)(/[^/]+)/upload(/[^/]+){2}$", Pattern.CASE_INSENSITIVE);
                Pattern updatePattern = Pattern.compile("^/(api|asyncapi)(/[^/]+){1,2}/update$", Pattern.CASE_INSENSITIVE);
                Pattern deletePattern = Pattern.compile("^/(api|asyncapi)(/[^/]+){1,2}/delete$", Pattern.CASE_INSENSITIVE);

                if(tablePattern.matcher(request.getRequestURI()).find()) {
                    List<ColumnInfo> columns = FastJsonUtils.toList(postString, ColumnInfo.class);
                    TriggerContext.exec(table, EventType.BuildTable, columns, request);
                    break;
                }

                else if(viewPattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = FastJsonUtils.convertJsonToObject(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.BuildTable, queryInfo, request);
                    break;
                }

                else if(uploadPattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = FastJsonUtils.convertJsonToObject(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.Upload, queryInfo, request);
                    break;
                }

                else if(updatePattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = FastJsonUtils.convertJsonToObject(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.Update, queryInfo, request);
                    break;
                }

                else if(deletePattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = FastJsonUtils.convertJsonToObject(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.Delete, queryInfo, request);
                    break;
                }

                QueryInfo queryInfo = FastJsonUtils.convertJsonToObject(postString, QueryInfo.class);
                if((queryInfo.getKeywords() != null && queryInfo.getKeywords().size() > 0) ||
                        (queryInfo.getConditionList() != null && queryInfo.getConditionList().size() > 0)) {
                    TriggerContext.exec(table, EventType.Query, queryInfo, request);
                }

                else {
                    TriggerContext.exec(table, EventType.List, queryInfo, request);
                }
                break;
            case "PUT":
                Map data = FastJsonUtils.convert(postString, Map.class);
                if(StringUtils.isEmpty(attrs.get("id"))) {
                    if(data != null) {
                        TriggerContext.exec(table, EventType.Insert, data, request);
                    }

                    else {
                        List<Map<String, Object>> params = FastJsonUtils.convert(postString, List.class);
                        TriggerContext.exec(table, EventType.Insert, params, request);
                    }
                }

                else {
                    TriggerContext.exec(table, EventType.Update, data, request);
                }

                if(hasUploadFile) {
                    TriggerContext.exec(table, EventType.Upload, ((RequestWrapper)request).getParameters(), request);
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

    private class InitLocalMap {
        private boolean myResult;
        private HttpServletResponse response;
        private Map<String, String> attrs;
        private String currentTable;

        public InitLocalMap(HttpServletResponse response, Map<String, String> attrs) {
            this.response = response;
            this.attrs = attrs;
        }

        boolean is() {
            return myResult;
        }

        public String getCurrentTable() {
            return currentTable;
        }

        public InitLocalMap invoke() throws IOException {
            String datasource = null;
            currentTable = null;

            if(attrs != null) {
                datasource = attrs.get("datasource");
                if(!"join".equalsIgnoreCase(attrs.get("table"))) {
                    currentTable = attrs.get("table");
                }
            }

            if(StringUtils.isNotEmpty(datasource)){
                final String ds = datasource;
                List<String> datasourceList = StringUtils.splitString2List(datasourceString, ",");
                Optional<String> opt = datasourceList.stream()
                        .filter(a -> a.toLowerCase().equals(ds.toLowerCase())).findAny();
                if(opt == null || !opt.isPresent()) {
                    response.sendError(HttpStatus.NOT_FOUND.value(), "没有找到匹配的url");
                    myResult = true;
                    return this;
                }
            }

            ApplicationContext.getThreadLocalMap().put(CURRENT_DATASOURCE, datasource);
            if(StringUtils.isNotEmpty(currentTable)) {
                ApplicationContext.getThreadLocalMap().put(CURRENT_TABLE, currentTable);
                ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + currentTable, PrivilegeScope.DENIED);
            }
            myResult = false;
            return this;
        }
    }
}
