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
import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.TriggerContext;
import ccait.ccweb.controllers.BaseController;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.enums.EventType;
import ccait.ccweb.enums.PrivilegeScope;
import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.model.*;
import ccait.ccweb.utils.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
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

import static ccait.ccweb.utils.StaticVars.*;

/**
 * @description 访问权限拦截器
 */
public class SecurityInterceptor implements HandlerInterceptor {

    @Value("${entity.security.admin.username:admin}")
    private String admin;

    @Value("${entity.ip.whiteList:}")
    private String whiteListText;

    @Value("${entity.ip.blackList:}")
    private String blackListText;

    @Value("${entity.security.encrypt.AES.publicKey:ccait}")
    private String aesPublicKey;

    @Value("${entity.auth.jwt.enable:false}")
    private boolean jwtEnable;

    @Value("${entity.auth.aes.enable:false}")
    private boolean aesEnable;

    @Value("${entity.auth.wechat.enable:false}")
    private boolean wechatEnable;

    private static final Logger log = LogManager.getLogger( SecurityInterceptor.class );

    private boolean hasUploadFile;

    @PostConstruct
    private void construct() {
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        return check(request, response, handler);
    }

    private boolean check(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if("yes".equals(response.getHeader("finish"))) {
            return true;
        }

        if(!request.getRequestURI().endsWith("/login")) {
            loginByToken(request, response);
        }

        if(request instanceof CCWebRequestWrapper) {

            Map<String, String> attrs = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            PermissionUtils.InitLocalMap initLocalMap = new PermissionUtils.InitLocalMap(response, attrs).invoke();
            if (initLocalMap.is()) return false;

            String currentTable = initLocalMap.getCurrentTable();

            if (!vaildUploadFilesByInsert((CCWebRequestWrapper) request, currentTable)) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                if(response.isCommitted()) {
                    response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "无效的上传文件格式!!!");
                }
                return false;
            }

            // 验证权限
            if (PermissionUtils.allowIp(request, whiteListText, blackListText) &&
                    this.hasPermission(handler, request.getMethod(), request, response, attrs, currentTable)) {
                return true;
            }

            //  null == request.getHeader("x-requested-with") TODO 暂时用这个来判断是否为ajax请求
            // 如果没有权限 则抛401异常 springboot会处理，跳转到 /error/401 页面
            response.setStatus(HttpStatus.FORBIDDEN.value());
            if(!response.isCommitted()) {
                response.sendError(HttpStatus.FORBIDDEN.value(), LangConfig.getInstance().get("has_not_privilege"));
            }
            throw new Exception(LangConfig.getInstance().get("has_not_privilege"));
        }
        else {
            return true;
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

    private void runTrigger(String table, String method, Map<String, String > attrs, String postString, HttpServletRequest request) throws Exception {

        switch (method.toUpperCase()) {
            case "GET":
                TriggerContext.exec(table, EventType.View, attrs.get("id"), request);
                break;
            case "POST":

                if(PermissionUtils.tablePattern.matcher(request.getRequestURI()).find()) {
                    List<ColumnInfo> columns = FastJsonUtils.toList(postString, ColumnInfo.class);
                    TriggerContext.exec(table, EventType.BuildTable, columns, request);
                    break;
                }

                else if(PermissionUtils.viewPattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = FastJsonUtils.convertJsonToObject(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.BuildTable, queryInfo, request);
                    break;
                }

                else if(PermissionUtils.uploadPattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = FastJsonUtils.convertJsonToObject(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.Upload, queryInfo, request);
                    break;
                }

                else if(PermissionUtils.importPattern.matcher(request.getRequestURI()).find()) {
                    TriggerContext.exec(table, EventType.Import, postString.getBytes("ISO-8859-1"), request);
                    break;
                }

                else if(PermissionUtils.exportPattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = FastJsonUtils.convertJsonToObject(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.Export, queryInfo, request);
                    break;
                }

                else if(PermissionUtils.updatePattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = FastJsonUtils.convertJsonToObject(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.Update, queryInfo, request);
                    break;
                }

                else if(PermissionUtils.deletePattern.matcher(request.getRequestURI()).find()) {
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
                    TriggerContext.exec(table, EventType.Upload, postString.getBytes("ISO-8859-1"), request);
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

    /**
     * 是否有权限
     *
     * @param handler
     * @return
     */
    private boolean hasPermission(Object handler, String method, HttpServletRequest request, HttpServletResponse response, Map<String, String> attrs, String table) throws Exception {
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
                x = canAccessTable(method, (CCWebRequestWrapper) request, response, requiredPermission, attrs, table);
            }

            else {
                CCWebRequestWrapper requestWarpper = (CCWebRequestWrapper)request;
                QueryInfo queryInfo = FastJsonUtils.convertJsonToObject((requestWarpper).getRequestPostString(), QueryInfo.class);
                List<String> tableList = new ArrayList<String>();
                if(queryInfo != null && queryInfo.getJoinTables() != null && queryInfo.getJoinTables().size() > 0) {
                    for (int i = 0; i < queryInfo.getJoinTables().size(); i++) {
                        if(StringUtils.isEmpty(queryInfo.getJoinTables().get(i).getTablename())) {
                            continue;
                        }
                        x = x && canAccessTable(method, (CCWebRequestWrapper) request, response, requiredPermission, attrs, queryInfo.getJoinTables().get(i).getTablename());
                    }
                }
            }

            return x;
        }
        return true;
    }

    private Boolean canAccessTable(String method, CCWebRequestWrapper request, HttpServletResponse response, AccessCtrl requiredPermission, Map<String, String> attrs, String table) throws Exception {

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

        String message = null;
        if(aclList == null || aclList.size() < 1) {

            if(method.equals("GET") || (method.equals("POST") && !request.getRequestURI().endsWith("/delete") &&
                    !request.getRequestURI().endsWith("/upload") && !request.getRequestURI().endsWith("/update") &&
                    !request.getRequestURI().endsWith("/import"))) {
                log.info(String.format(LOG_PRE_SUFFIX + "表[%s]没有设置权限，允许查询！", table));

                if(user != null) {
                    if (!checkPrivilege(table, user, aclList, method, attrs, request.getRequestPostString(), request, response)) {
                        message = String.format(LangConfig.getInstance().get("has_not_privilege_for_this_table"), user.getUsername(), table, method);
                        log.warn(LOG_PRE_SUFFIX + message);
                        throw new Exception(message);
                    }
                }
                return true;
            }

            else {
                message = String.format(LangConfig.getInstance().get("table_no_set_acl_can_not_be_edit"), table);
                log.warn(LOG_PRE_SUFFIX + message);
                throw new Exception(message);
            }
        }

        List<UUID> groupIds = aclList.stream().map(a->a.getGroupId()).collect(Collectors.toList());

        GroupModel requiredPermissionGroup = new GroupModel();
        requiredPermissionGroup.setGroupName(requiredPermission.groupName());
        requiredPermissionGroup = requiredPermissionGroup.where("[groupName]=#{groupName}").first();

        if(requiredPermissionGroup != null && requiredPermissionGroup.getGroupId() != null &&
                !groupIds.contains(requiredPermissionGroup.getGroupId()) ) { //不属于指定组
            message = String.format(LangConfig.getInstance().get("this_group_can_not_edit_table"), requiredPermission.groupName(), table);
            log.warn(LOG_PRE_SUFFIX + message);
            throw new Exception(message);
        }

        if(user == null) {
            log.warn(LOG_PRE_SUFFIX + "用户未登录！！！");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            if(!response.isCommitted()) {
                response.sendError(HttpStatus.UNAUTHORIZED.value());
            }
            return false;
        }

        boolean hasGroup = false;
        for (UUID groupId : groupIds) {
            Optional<UserGroupRoleModel> opt = user.getUserGroupRoleModels().stream()
                    .filter(a -> a.getGroup() == null || a.getGroup().getGroupId().equals(groupId)).findAny();

            if(opt != null && opt.isPresent()) {
                hasGroup = true;
                break;
            }
        }

        Optional<AclModel> optional = aclList.stream().filter(a->a.getGroupId() == null).findAny();
        boolean canAllAccess = (optional == null ? false : optional.isPresent());
        if( aclList.size() > 0 && !canAllAccess) {
            if(!hasGroup) {
                message = String.format(LangConfig.getInstance().get("user_has_not_group_for_acl_table"), user.getUsername(), table);
                log.warn(LOG_PRE_SUFFIX + message);
                throw new Exception(message);
            }
        }

        if(!checkPrivilege(table, user, aclList, method, attrs, request.getRequestPostString(), request, response)){
            message = String.format(LangConfig.getInstance().get("has_not_privilege_for_this_table"), user.getUsername(), table, method);
            log.warn(LOG_PRE_SUFFIX + message);
            throw new Exception(message);
        }

        return true;
    }


    private boolean checkPrivilege(String table, UserModel user, List<AclModel> aclList, String method, Map<String, String> attrs,
                                   String postString, HttpServletRequest request, HttpServletResponse response) throws Exception {

        String privilegeWhere = null;
        switch (method.toUpperCase()) {
            case "GET":
                if(Pattern.matches("^/(?i)(as)?(?i)api/(?i)download/[^/]+/[^/]+/[^/]+$", request.getRequestURI())) {
                    privilegeWhere = "canDownload=1";
                }

                else if(Pattern.matches("^/(?i)(as)?(?i)api/(?i)preview/[^/]+/[^/]+/[^/]+$", request.getRequestURI())) {
                    privilegeWhere = "canPreview=1";
                }

                else if(Pattern.matches("^/(?i)(as)?(?i)api/(?i)play/[^/]+/[^/]+/[^/]+$", request.getRequestURI())) {
                    privilegeWhere = "canPlayVideo=1";
                }

                else {
                    privilegeWhere = "(canQuery=1 OR canList=1 OR canView=1 OR canDecrypt=1)";
                }
                break;
            case "POST":

                if(PermissionUtils.tablePattern.matcher(request.getRequestURI()).find()) {
                    break;
                }

                else if(PermissionUtils.updatePattern.matcher(request.getRequestURI()).find()) {
                    privilegeWhere = "canUpdate=1";
                }

                else if(PermissionUtils.deletePattern.matcher(request.getRequestURI()).find()) {
                    privilegeWhere = "canDelete=1";
                }

                else if(PermissionUtils.uploadPattern.matcher(request.getRequestURI()).find()) {
                    privilegeWhere = "canUpload=1";
                    break;
                }

                else if(PermissionUtils.importPattern.matcher(request.getRequestURI()).find()) {
                    privilegeWhere = "canImport=1";
                    break;
                }

                else if(PermissionUtils.exportPattern.matcher(request.getRequestURI()).find()) {
                    privilegeWhere = "canExport=1";
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
        List<String> roleIdList = user.getUserGroupRoleModels().stream()
                .map(a->a.getRoleId().toString().replace("-","")).collect(Collectors.toList());

        roleIdList = roleIdList.stream().collect(
                Collectors.collectingAndThen(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(o -> o))), ArrayList::new));

        List<UUID> groupIds = aclList.stream().filter(o-> o.getGroupId() != null).map(b->b.getGroupId()).collect(Collectors.toList());
        groupIds = groupIds.stream().collect(
                Collectors.collectingAndThen(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(o -> o))), ArrayList::new));

        List<PrivilegeModel> privilegeList = new ArrayList<PrivilegeModel>();
        if(roleIdList.size() > 0) {
            String roleWhere = String.format("[roleId] in ('%s')", String.join("','", roleIdList));
            privilegeList = aclList.size() > 0 ? privilege.where(roleWhere).and(privilegeWhere)
                    .and(String.format("(groupId in ('%s') OR groupId IS NULL OR groupId='')", String.join("','",
                            groupIds.stream().filter(o->o != null).map(a -> a.toString().replace("-", ""))
                                    .collect(Collectors.toList()))))
                    .query()
                    : privilege.where(roleWhere).and(privilegeWhere)
                    .query();
        }

        else {
            privilegeList = aclList.size() > 0 ? privilege.where(privilegeWhere)
                    .and(String.format("(groupId in ('%s') OR groupId IS NULL OR groupId='')", String.join("','",
                            groupIds.stream().filter(o->o != null).map(a -> a.toString().replace("-", ""))
                                    .collect(Collectors.toList()))))
                    .query()
                    : privilege.where(privilegeWhere)
                    .query();
        }

        boolean result = false;
        PrivilegeScope currentMaxScope = PrivilegeScope.DENIED;
        for (UserGroupRoleModel groupRole : user.getUserGroupRoleModels()) {
            Optional<PrivilegeModel> opt = privilegeList.stream().filter(a -> a.getRoleId().equals(groupRole.getRoleId()) &&
                    (a.getGroupId()==null || a.getGroupId().equals(groupRole.getGroupId())))
                    .max(Comparator.comparing(b->b.getScope().getCode()));

            if(opt != null && opt.isPresent()) {

                if(opt.get().getScope().getCode() > currentMaxScope.getCode()) {
                    currentMaxScope = opt.get().getScope(); //求出最大权限
                }

                result = true;
            }
        }

        ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + table, currentMaxScope);

        return  result;
    }

    private void loginByToken(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String token = request.getHeader("Authorization");
        if(StringUtils.isEmpty(token)) {
            return;
        }

        if(wechatEnable) {
            /*** TODO ***/
        }

        String userId = null;
        if(jwtEnable) {
            try {
                userId = JWT.decode(token).getClaim("id").asString();

                UserModel user = new UserModel();
                user.setId(Integer.parseInt(userId));
                user = user.where("id=#{id}").first();
                if (user == null) {
                    throw new RuntimeException("非法用户！");
                }
                Boolean verify = JwtUtils.isVerify(token, user);
                if (!verify) {
                    throw new RuntimeException("非法访问！");
                }

                BaseController.login(user.getUsername(), user.getPassword(), request, response);
                response.setHeader("token", token);

                return;
            } catch (JWTDecodeException e) {
                log.error(e);
            }
        }

        if(aesEnable) {
            try {
                token = EncryptionUtil.decryptByAES(token, aesPublicKey);
                if (StringUtils.isEmpty(token)) {
                    throw new RuntimeException("fail to get the token!!!");
                }

                String username = token.substring(0, token.length() - 32);
                String vaildCode = token.substring(token.length() - 32);

                UserModel user = new UserModel();
                user.setUsername(username);
                user = user.where("username=#{username}").first();
                if(user == null) {
                    throw new RuntimeException("fail to get the token!!!");
                }

                String vaildCode2 = EncryptionUtil.md5(EncryptionUtil.encryptByAES(user.getId().toString(), user.getUsername() + aesPublicKey), "UTF-8");
                if(!vaildCode2.equals(vaildCode)) {
                    throw new RuntimeException("fail to get the token!!!");
                }

                BaseController.login(request, user);
                response.setHeader("token", token);

                return;
            }

            catch (Exception e) {
                log.error(e);
            }
        }
    }

    private boolean vaildUploadFilesByInsert(CCWebRequestWrapper request, String table) throws Exception {

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

        if(request.getRequestURI().toLowerCase().endsWith("/import") ||
                request.getRequestURI().toLowerCase().endsWith("/upload")) { //上传和导入接口不校验

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
                        throw new IOException(LangConfig.getInstance().get("can_not_supported_file_type"));
                    }
                }

                if (configMap.get("maxSize") != null) {
                    if (fileBytes.length > 1024 * 1024 * Integer.parseInt(configMap.get("maxSize").toString())) {
                        throw new IOException(LangConfig.getInstance().get("upload_field_to_be_long"));
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
}
