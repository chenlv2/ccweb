/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.controllers;


import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.context.IndexingContext;
import ccait.ccweb.context.TriggerContext;
import ccait.ccweb.dynamic.DynamicClassBuilder;
import ccait.ccweb.enums.*;
import ccait.ccweb.listener.ExcelListener;
import ccait.ccweb.model.*;
import ccait.ccweb.utils.*;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.fastjson.JSONObject;
import entity.query.*;
import entity.query.annotation.PrimaryKey;
import entity.query.core.ApplicationConfig;
import entity.tool.util.*;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.apache.http.HttpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xslf.usermodel.*;
import org.jcodec.api.JCodecException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import sun.misc.BASE64Decoder;

import javax.annotation.PostConstruct;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ccait.ccweb.dynamic.DynamicClassBuilder.smallHump;
import static ccait.ccweb.utils.StaticVars.*;
import static entity.tool.util.StringUtils.cast;
import static entity.tool.util.StringUtils.join;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

public abstract class BaseController {

    private static final Logger log = LogManager.getLogger(BaseController.class);

    @Value("${entity.auth.jwt.millis:600000}")
    private static long jwtMillis;

    @Value("${entity.auth.jwt.enable:false}")
    private static boolean jwtEnable;

    public ResponseData<Object> RMessage;

    @Autowired
    protected HttpServletRequest request;

    @Autowired
    protected HttpServletResponse response;

    @Autowired
    private QueryInfo queryInfo;

    @Autowired
    private IndexingContext indexingContext;

    @Value("${entity.queryable.ignoreTotalCount:true}")
    protected boolean ignoreTotalCount;

    @Value("${entity.table.reservedField.userPath:userPath}")
    protected String userPathField;

    @Value("${entity.table.reservedField.createBy:createBy}")
    protected String createByField;

    @Value("${entity.table.reservedField.groupId:groupId}")
    protected String groupIdField;


    @Value("${entity.page.maxSize}")
    protected Integer maxPageSize;

    @Value("${entity.security.encrypt.MD5.fields:}")
    private String md5Fields;

    @Value("${entity.security.encrypt.MD5.publicKey:ccait}")
    private String md5PublicKey;

    @Value("${entity.security.encrypt.MAC.fields:}")
    private String macFields;

    @Value("${entity.security.encrypt.MAC.publicKey:ccait}")
    private String macPublicKey;

    @Value("${entity.security.encrypt.AES.fields:}")
    private String aesFields;

    @Value("${entity.security.encrypt.AES.publicKey:ccait}")
    private String aesPublicKey;

    @Value("${entity.security.encrypt.SHA.fields:}")
    private String shaFields;

    @Value("${entity.security.encrypt.SHA.publicKey:ccait}")
    private String shaPublicKey;

    @Value("${entity.encoding:UTF-8}")
    private String encoding;

    @Value("${entity.security.encrypt.BASE64.fields:}")
    private String base64Fields;

    @Value("${entity.defaultDateByNow:false}")
    private boolean defaultDateByNow;
    private Map<String, Object> data;

    @Value("${entity.security.admin.username:admin}")
    private String admin;

    @Value("${entity.download.thumb.fixedWidth:0}")
    private Integer fixedWidth;

    @Value("${entity.download.thumb.scalRatio:0}")
    private Integer scalRatio;

    @Value("${entity.auth.wechat.secret:}")
    private String secret;

    @Value("${entity.auth.wechat.appid:}")
    private String appid;

    @Value("${entity.auth.wechat.enable:false}")
    private boolean wechatEnable;

    @Autowired
    private NonStaticResourceHttpRequestHandler nonStaticResourceHttpRequestHandler;

    @Value("${entity.upload.watermark:}")
    private String watermark;

    public BaseController() {
        RMessage = new ResponseData<Object>();
    }


    @PostConstruct
    private void construct() {
    }

    protected Logger getLogger()
    {
        return log;
    }

    protected Mono<ResponseData> successAs() {
        return Mono.just(success());
    }

    protected <T> Mono<ResponseData<T>> successAs(T data) {
        return Mono.just(success(data));
    }

    protected <T> Mono<ResponseData<T>> successAs(T data, PageInfo pageInfo) {
        return Mono.just(success(data, pageInfo));
    }

    protected Mono<ResponseData> errorAs(String message) {
        return Mono.just(error(message));
    }

    protected Mono<ResponseData> errorAs(int code, Exception e) {

        return Mono.just(error(code, e));
    }

    protected Mono<ResponseData> errorAs(int code, String message) {
        return Mono.just(error(code, message));
    }

    protected ResponseData<String> success() {
        return this.result(0, "OK", null, null);
    }

    protected <T> ResponseData<T> success(T data) {
        return this.result(0, "OK", data, null);
    }

    protected <T> ResponseData<T> success(T data, PageInfo pageInfo) {
        return this.result(0, "OK", data, pageInfo);
    }

    @SuppressWarnings("rawtypes")
    protected ResponseData error(String message) {
        return this.result(-1, message, "", null);
    }

    protected ResponseData error(int code, Exception e) {

        if(e instanceof HttpException) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            code = HttpStatus.UNAUTHORIZED.value();
        }

        if(StringUtils.isEmpty(e.getMessage())) {
            return this.result(code, e.toString(), "", null);
        }
        return this.result(code, e.getMessage(), "", null);
    }

    protected ResponseData error(int code, String message) {
        return this.result(code, message, "", null);
    }

    public <T> ResponseData<T> result(int code, String message, T data, PageInfo pageInfo) {
        ResponseData<T> result = new ResponseData<T>();

        result.setStatus(code);
        result.setMessage(message);
        result.setData(data);
        result.setPageInfo(pageInfo);

        result = handleResultEvent(result);

        response.addHeader("finish", "yes");

        return result;
    }

    private <T> ResponseData<T> handleResultEvent(ResponseData<T> result) {

        result.setStatus(0);

        String tablename = getTablename();

        try {
            result.setUuid(UUID.randomUUID());

            if(result.getStatus() != 0 || response.getStatus() != 200) {
                errorTrigger(result.getMessage(), tablename);
            }

            else {
                successTrigger(result, tablename);
            }

            if(result.getData() == null || isPrimitive(result.getData())) {
                return result;
            }

            //格式化输出数据
            result.setData(getFormatedData(tablename, result.getData())); //set format data

        } catch (Throwable throwable) {
            log.error(LOG_PRE_SUFFIX + throwable.getMessage(), throwable);
            result.setMessage(throwable.getMessage());
            errorTrigger(throwable.getMessage(), tablename);
        }

        return result;
    }

    protected <T> void setSession(String key, T data) {
        request.getSession().setAttribute( request.getSession().getId() + key, data );
    }

    @SuppressWarnings( "unchecked" )
    protected <T> T getSession(String key) {
        return (T) request.getSession().getAttribute( request.getSession().getId() + key );
    }

    protected <T> void setSession(HttpServletRequest request, String key, T data) {
        request.getSession().setAttribute( request.getSession().getId() + key, data );
    }

    @SuppressWarnings( "unchecked" )
    protected <T> T getSession(HttpServletRequest request, String key) {
        return (T) request.getSession().getAttribute( request.getSession().getId() + key );
    }

    protected void setLoginUser(UserModel data) {
        this.setSession( LOGIN_KEY, data );
    }

    @SuppressWarnings( "unchecked" )
    protected UserModel getLoginUser() {
        return (UserModel) this.getSession( LOGIN_KEY );
    }

    protected PrivilegeScope getCurrentMaxPrivilegeScope(String table) {
        Map<String, Object> map = ApplicationContext.getThreadLocalMap();
        if(!map.containsKey(CURRENT_MAX_PRIVILEGE_SCOPE + table)) {
            return null;
        }

        return (PrivilegeScope) map.get(CURRENT_MAX_PRIVILEGE_SCOPE + table);
    }

    public static String getTablename() {
        Map map = ApplicationContext.getThreadLocalMap();
        if(!map.containsKey(CURRENT_TABLE)) {
            return "";
        }

        return map.get(CURRENT_TABLE).toString();
    }

    protected String md5(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        return EncryptionUtil.md5(str, md5PublicKey, encoding);
    }

    protected void fillData(@RequestBody Map<String, Object> postData, Object entity)  {

        fillData(postData, entity, defaultDateByNow);
    }

    public static void fillData(Map<String, Object> postData, Object entity, boolean isDefaultDateByNow) {
        List<Field> fields = EntityContext.getFields(entity);
        List<String> argNames = postData.keySet().stream().collect(Collectors.toList());
        for(final String argname : argNames) {

            Optional<Field> opt = fields.stream().filter(a->a.getName().equals(smallHump(argname))).findAny();
            if("id".equals(argname) || !opt.isPresent()) {
                postData.remove(argname);
                continue;
            }

            String fieldName = opt.get().getName();
            Class<?> type = opt.get().getType();

            Object value = null;

            if(postData.get(argname) != null) {
                String valString = DBUtils.getSqlInjText(postData.get(argname).toString());
                value = cast(type, valString);
            }

            String key = argname;

            Map<String, Object> defaultValueMap = ApplicationConfig.getInstance().getMap("entity.defaultValue");
            if(defaultValueMap != null) {
                if(!defaultValueMap.containsKey(key)) {
                    key = String.format("%s.%s", getTablename(), key);
                }

                if(defaultValueMap.containsKey(key)) {
                    switch (DefaultValueMode.valueOf(defaultValueMap.get(key).toString())) {
                        case UUID_RANDOM:
                            if(type.equals(String.class) && value == null) {
                                value = UUID.randomUUID().toString().replace("-", "");
                            }
                            break;
                        case DATE_NOW:
                            if(type.equals(Date.class) && value == null) {
                                value = Datetime.now();
                            }
                            break;
                    }
                }
            }

            if(value == null) {
                if(type.equals(Date.class) && isDefaultDateByNow) {
                    value = Datetime.now();
                }

                else if(opt.get().getAnnotation(PrimaryKey.class) != null) {
                    if(type.equals(String.class)) {
                        value = UUID.randomUUID().toString().replace("-", "");
                    }

                    else if(type.equals(Integer.class) || type.equals(int.class) ||
                            type.equals(Long.class) || type.equals(long.class)) {
                        value = 0;
                    }
                }
            }

            ReflectionUtils.setFieldValue(entity.getClass(), entity, fieldName, value);
            postData.put(argname, String.format("#{%s}", fieldName));
        }
    }

    protected <T> void encrypt(T data) {

        if(data == null) {
            return;
        }

        if(StringUtils.isNotEmpty(md5Fields)) {

            List<String> fieldList = StringUtils.splitString2List(md5Fields, ",");

            if(data instanceof List){
                encrypt((List<ConditionInfo>)data, fieldList, EncryptMode.MD5);
            }

            else {
                encrypt((Map<String, Object>)data, fieldList, EncryptMode.MD5);
            }
        }

        if(StringUtils.isNotEmpty(macFields)) {

            List<String> fieldList = StringUtils.splitString2List(macFields, ",");

            if(data instanceof List){
                encrypt((List<ConditionInfo>)data, fieldList, EncryptMode.AES);
            }

            else {
                encrypt((Map<String, Object>)data, fieldList, EncryptMode.AES);
            }
        }

        if(StringUtils.isNotEmpty(shaFields)) {

            List<String> fieldList = StringUtils.splitString2List(shaFields, ",");

            if(data instanceof List){
                encrypt((List<ConditionInfo>)data, fieldList, EncryptMode.AES);
            }

            else {
                encrypt((Map<String, Object>)data, fieldList, EncryptMode.AES);
            }
        }

        if(StringUtils.isNotEmpty(base64Fields)) {

            List<String> fieldList = StringUtils.splitString2List(base64Fields, ",");

            if(data instanceof List){
                encrypt((List<ConditionInfo>)data, fieldList, EncryptMode.BASE64);
            }

            else {
                encrypt((Map<String, Object>)data, fieldList, EncryptMode.BASE64);
            }
        }

        if(StringUtils.isNotEmpty(aesFields)) {

            List<String> fieldList = StringUtils.splitString2List(aesFields, ",");

            if(data instanceof List){
                encrypt((List<ConditionInfo>)data, fieldList, EncryptMode.AES);
            }

            else {
                encrypt((Map<String, Object>)data, fieldList, EncryptMode.AES);
            }
        }
    }

    protected void encrypt(Map<String, Object> data, List<String> fieldList, EncryptMode encryptMode) {

        if(fieldList == null || fieldList.size() < 1) {
            return;
        }

        data.keySet().stream().filter(a -> fieldList.contains(a) || fieldList.contains(String.join(".", getTablename(), a)))
                .forEach(key -> {
                    if(data.get(key) instanceof String) {
                        switch (encryptMode) {
                            case MD5:
                                data.put(key, encrypt(data.get(key).toString(), encryptMode, md5PublicKey, encoding));
                                break;
                            case MAC:
                                data.put(key, encrypt(data.get(key).toString(), encryptMode, macPublicKey, encoding));
                                break;
                            case SHA:
                                data.put(key, encrypt(data.get(key).toString(), encryptMode));
                                break;
                            case BASE64:
                                data.put(key, encrypt(data.get(key).toString(), encryptMode, encoding));
                                break;
                            case AES:
                                data.put(key, encrypt(data.get(key).toString(), encryptMode, aesPublicKey));
                                break;
                        }
                    }
                });
    }

    public static String encrypt(String value, EncryptMode encryptMode, String... encryptArgs) {
        try {
            switch (encryptMode) {
                case MD5:
                    if(encryptArgs == null || encryptArgs.length != 2) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.md5(value, encryptArgs[0], encryptArgs[1]);
                    break;
                case MAC:
                    if(encryptArgs == null || encryptArgs.length != 2) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.mac(value.getBytes(encryptArgs[1]), encryptArgs[0]);
                    break;
                case SHA:
                    value = EncryptionUtil.sha(value);
                    break;
                case BASE64:
                    if(encryptArgs == null || encryptArgs.length != 1) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.base64Encode(value, encryptArgs[0]);
                    break;
                case AES:
                    if(encryptArgs == null || encryptArgs.length != 1) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.encryptByAES(value, encryptArgs[0]);
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            log.error(e, e);
        } catch (NoSuchAlgorithmException e) {
            log.error(e, e);
        } catch (Exception e) {
            log.error(e, e);
        }

        return value;
    }

    protected void encrypt(List<ConditionInfo> data, List<String> fieldList, EncryptMode encryptMode) {

        if(fieldList == null || fieldList.size() < 1) {
            return;
        }

        data.stream().filter(a -> fieldList.contains(a.getName()) || fieldList.contains(String.join(".", getTablename(), a.getName())))
                .forEach(b -> {
                    if(b.getValue() instanceof String) {
                        switch (encryptMode) {
                            case MD5:
                                b.setValue(encrypt(b.getValue().toString(), encryptMode, md5PublicKey, encoding));
                                break;
                            case MAC:
                                b.setValue(encrypt(b.getValue().toString(), encryptMode, macPublicKey, encoding));
                                break;
                            case SHA:
                                b.setValue(encrypt(b.getValue().toString(), encryptMode));
                                break;
                            case BASE64:
                                b.setValue(encrypt(b.getValue().toString(), encryptMode, encoding));
                                break;
                            case AES:
                                b.setValue(encrypt(b.getValue().toString(), encryptMode, aesPublicKey, encoding));
                                break;
                        }
                    }
                });
    }

    protected void decrypt(Map<String, Object> data) {

        if(data == null) {
            return;
        }

        if(StringUtils.isNotEmpty(aesFields)) {

            List<String> fieldList = StringUtils.splitString2List(aesFields, ",");

            decrypt((Map<String, Object>)data, fieldList, EncryptMode.AES);
        }

        if(StringUtils.isNotEmpty(base64Fields)) {

            List<String> fieldList = StringUtils.splitString2List(base64Fields, ",");

            decrypt((Map<String, Object>)data, fieldList, EncryptMode.BASE64);
        }
    }

    protected void decrypt(Map<String, Object> data, List<String> fieldList, EncryptMode encryptMode) {
        this.data = data;

        if(fieldList == null || fieldList.size() < 1) {
            return;
        }

        data.keySet().stream().filter(a -> fieldList.contains(a) || fieldList.contains(String.join(".", getTablename(), a)))
                .forEach(key -> {
                    if(data.get(key) instanceof String) {
                        switch (encryptMode) {
                            case BASE64:
                                data.put(key, decrypt(data.get(key).toString(), encryptMode, encoding));
                                break;
                            case AES:
                                data.put(key, decrypt(data.get(key).toString(), encryptMode, aesPublicKey));
                                break;
                        }
                    }
                });
    }

    public static String decrypt(String value, EncryptMode encryptMode, String... encryptArgs) {
        try {
            switch (encryptMode) {
                case BASE64:
                    if(encryptArgs == null || encryptArgs.length != 1) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.base64Decode(value, encryptArgs[0]);
                    break;
                case AES:
                    if(encryptArgs == null || encryptArgs.length != 1) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.decryptByAES(value, encryptArgs[0]);
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            log.error(e, e);
        } catch (NoSuchAlgorithmException e) {
            log.error(e, e);
        } catch (Exception e) {
            log.error(e, e);
        }

        return value;
    }

    private String getRequestPostString() throws IOException {
        return getRequestPostString(request);
    }

    public static String getRequestPostString(HttpServletRequest request)
            throws IOException {
        String charSetStr = request.getCharacterEncoding();
        if (charSetStr == null) {
            charSetStr = "UTF-8";
        }

        Charset charSet = Charset.forName(charSetStr);

        return StreamUtils.copyToString(request.getInputStream(), charSet);
    }

    protected String base64Encode(String data) throws Exception {
        return EncryptionUtil.base64Encode(data, encoding);
    }

    protected String base64EncodeSafe(String data) throws Exception {
        return EncryptionUtil.base64EncodeSafe(data, encoding);
    }

    protected String base64Decode(String data) throws Exception {
        return EncryptionUtil.base64Decode(data, encoding);
    }

    public static boolean isPrimitive(Object obj) {
        try {
            return ((Class<?>) obj.getClass().getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

    /***
     * 检查数据权限
     * @param table
     * @param data
     * @return
     * @throws SQLException
     */
    protected boolean checkDataPrivilege(String table, Map data) throws SQLException {
        switch(getCurrentMaxPrivilegeScope(table)) {
            case ALL:
                return true;
            case SELF:
                if(data.containsKey(createByField) && getLoginUser().getId().equals(data.get(createByField))) {
                    return true;
                }

                else if(data.containsKey(userPathField) && getLoginUser().getPath().equals(data.get(userPathField))) {
                    return true;
                }
                break;
            case CHILD:
                if(data.containsKey(userPathField) && data.get(userPathField) != null
                        && data.get(userPathField).toString().startsWith(getLoginUser().getPath())) {
                    return true;
                }
                break;
            case PARENT_AND_CHILD:
                if(data.containsKey(userPathField) && data.get(userPathField) != null) {
                    String parentPath = getLoginUser().getPath().substring(0, getLoginUser().getPath().lastIndexOf("/"));
                    if(data.get(userPathField).toString().startsWith(parentPath)) {
                        return true;
                    }
                }
                break;
            case GROUP:

                if(!data.containsKey(createByField)) { //公开数据
                    return true;
                }

                if(data.get(createByField) != null && data.get(createByField) != null) {

                    String createBy = data.get(createByField).toString();
                    List<Integer> userIdByGroups = (List<Integer>)ApplicationContext.getThreadLocalMap().get(CURRENT_USERID_BY_GROUPS);
                    if(userIdByGroups.contains(Integer.parseInt(createBy))) {
                        return true;
                    }
                }
                break;
        }

        return false;
    }

    /***
     * user logout
     */
    public void logout(){
        setLoginUser(null);
    }

    /***
     * user login
     * @param user
     * @return
     * @throws Exception
     */
    public UserModel logoin(UserModel user) throws Exception {

        return login(user.getUsername(), md5(user.getPassword()), request, response);
    }

    public static UserModel login(String username, String passwordEncode, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(passwordEncode)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            throw new Exception(LangConfig.getInstance().get("username_and_password_can_not_be_empty"));
        }

        UserModel user = new UserModel();

        user.setUsername(username);
        user.setPassword(passwordEncode);

        Where<UserModel> where = user.where("[username]=#{username}").and("[password]=#{password}");

        user = where.first();

        return login(request, user);
    }

    public static UserModel login(HttpServletRequest request, UserModel user) throws Exception {
        if(user == null) {
            throw new Exception(LangConfig.getInstance().get("username_or_password_is_invalid"));
        }

        if(user.getStatus() != null && !user.getStatus().equals(0)) {
            throw new Exception(LangConfig.getInstance().get("user_status_has_been_frozen"));
        }

        user.getUserGroupRoleModels().stream().forEach((item)->{
            item.getGroup();
            item.getRole();
        });

        user.setPassword("******");

        request.getSession().setAttribute( request.getSession().getId() + LOGIN_KEY, user );

        List<String> groupIdList = user.getUserGroupRoleModels().stream()
                .filter(a->a.getGroupId() != null)
                .map(a->a.getGroupId().toString().replace("-", ""))
                .collect(Collectors.toList());

        UserGroupRoleModel userGroupRoleModel = new UserGroupRoleModel();
        List<Integer> userIdListByGroups = new ArrayList<Integer>();
        if(groupIdList.size() > 0) {
            userIdListByGroups = userGroupRoleModel.where(String.format("groupId in ('%s')", join("', '", groupIdList)))
                    .select("userId").query(Integer.class);
        }
        ApplicationContext.getThreadLocalMap().put(CURRENT_USERID_BY_GROUPS, userIdListByGroups);

        if(jwtEnable) {
            String token = JwtUtils.createJWT(jwtMillis, user);
            user.setToken(token);
        }

        return user;
    }

    /***
     * delete data by id list
     * @param table
     * @param idList
     * @return
     * @throws Exception
     */
    public List deleteByIdList(String table, List<String> idList) throws Exception {

        List<Object> result = null;

        String strid = idList.get(0);
        String strIds = StringUtils.join("", idList);
        if(Pattern.matches("[^0-9]+", strIds)) {
            strid += "A";
        }
        Object entity = EntityContext.getEntityId(getCurrentDatasourceId(), table, strid);
        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        Where where = null;
        for (String id : idList) {
            where = queryInfo.getWhereQueryableById(entity, id);
            Maybe<Map> flow = where.asyncFirst(Map.class);
            Map data = flow.blockingGet();
            if(data == null) {
                throw new Exception(LangConfig.getInstance().get("can_not_find_data_for_delete"));
            }

            if(!checkDataPrivilege(table, data)) {
                throw new Exception(NO_PRIVILEGE_MESSAGE);
            }

            result.add(where.asyncDelete().toList().blockingGet());

            String currentDatasource = getCurrentDatasourceId();

            deleteFileByFieldname(table, data, currentDatasource);
        }

        return result;
    }

    /***
     * delete data by id
     * @param table
     * @param id
     * @return
     * @throws Exception
     */
    public Integer delete(String table, String id) throws Exception {
        Object entity = EntityContext.getEntityId(getCurrentDatasourceId(), table, id);
        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        Where where = queryInfo.getWhereQueryableById(entity, id);
        Map<String, Object> data = (Map<String, Object>) where.first(Map.class);
        if(data == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_data_for_delete"));
        }

        if(!checkDataPrivilege(table, data)) {
            throw new Exception(NO_PRIVILEGE_MESSAGE);
        }

        Integer result = null;

        if(where.delete()){
            String currentDatasource = getCurrentDatasourceId();

            deleteFileByFieldname(table, data, currentDatasource);
            result = 1;
        }

        return result;
    }

    private String getCurrentDatasourceId() {
        String currentDatasource = "default";
        if (ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE) != null) {
            currentDatasource = ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE).toString();
        }
        return currentDatasource;
    }

    private void deleteFileByFieldname(String table, Map<String, Object> data, String currentDatasource) {
        for(String fieldname : data.keySet()) {

            Map<String, Object> uploadConfigMap = ApplicationConfig.getInstance()
                    .getMap(String.format("entity.upload.%s.%s.%s", currentDatasource, table, fieldname));

            if(uploadConfigMap == null ||
                    uploadConfigMap.size() < 1 ||
                    uploadConfigMap.get("path") == null) {
                continue;
            }

            String root = uploadConfigMap.get("path").toString();
            if(root.lastIndexOf("/") == root.length() - 1 ||
                    root.lastIndexOf("\\") == root.length() - 1) {
                root = root.substring(0, root.length() - 2);
            }
            root = String.format("%s/%s/%s/%s", root, currentDatasource, table, fieldname);
            File file = new File(String.format("%s/%s", root, data.get(fieldname)));
            if(file.exists()){
                file.delete();
            }
        }
    }

    /***
     * query join result count
     * @param queryInfo
     * @return
     * @throws Exception
     */
    public Long joinQueryCount(QueryInfo queryInfo) throws Exception {
        Where where = getWhereQueryableByJoin(queryInfo);
        return where.count();
    }

    /***
     * query select data
     * @param queryInfo
     * @return
     * @throws Exception
     */
    public List joinQuery(QueryInfo queryInfo) throws Exception {

        return joinQuery(queryInfo, false);
    }

    /***
     * query select data
     * @param queryInfo
     * @return
     * @throws Exception
     */
    public List joinQuery(QueryInfo queryInfo, boolean byExport) throws Exception {


        if(byExport) {

            List<ColumnInfo> columns = DynamicClassBuilder.getColumnInfosBySelectList(queryInfo.getSelectList());

            Object info = DynamicClassBuilder.create("TABLE" + UUID.randomUUID().toString().replace("-", ""), columns, false);

            queryInfo.getSelectList().clear();

            Where where = getWhereQueryableByJoin(queryInfo);

            QueryableAction ac = getQueryableAction(queryInfo, where, true);

            return ac.query(info.getClass(), queryInfo.getSkip(), queryInfo.getPageInfo().getPageSize());
        }

        Where where = getWhereQueryableByJoin(queryInfo);
        QueryableAction ac = getQueryableAction(queryInfo, where, true);

        return ac.query(Map.class, queryInfo.getSkip(), queryInfo.getPageInfo().getPageSize());
    }

    private Where getWhereQueryableByJoin(QueryInfo queryInfo) throws Exception {
        if(queryInfo.getJoinTables() == null || queryInfo.getJoinTables().size() < 1) {
            throw new Exception(LangConfig.getInstance().get("join_tables_can_not_be_empty"));
        }

        if(queryInfo.getJoinTables().size() < 2) {
            throw new Exception(LangConfig.getInstance().get("join_tables_can_not_be_less_tow"));
        }

        if(queryInfo.getJoinTables().stream().filter(a->a.getOnList() == null ||
                a.getOnList().size() < 1).count() == queryInfo.getJoinTables().size()) {
            throw new Exception(LangConfig.getInstance().get("onlist_can_not_be_empty"));
        }

        encrypt(queryInfo.getConditionList());

        Queryable firstQuery = null;
        String[] aliases =  { "a", "b", "c", "d", "e", "f", "g", "h", "i",
                "j", "k", "l", "m", "n", "o", "p", "q", "e", "r", "s", "t",
                "u", "v", "w", "x", "y", "z" };

        int i = 0;
        Map<String, String> tableOnMap = new HashMap<String, String>();
        List<TableInfo> tableList = new ArrayList<TableInfo>();
        for(TableInfo table : queryInfo.getJoinTables()) {

            if(StringUtils.isEmpty(table.getTablename())) {
                continue;
            }

            Object entity = EntityContext.getEntity(table.getTablename(), queryInfo);

            Queryable query = (Queryable) entity;
            if(query == null) {
                continue;
            }

            else if (firstQuery == null){
                firstQuery = query;
            }

            table.setEntity(entity);
            if(StringUtils.isEmpty(table.getAlias())) {
                table.setAlias(aliases[i]);
                i++;
            }

            table.setPrivilegeScope(getCurrentMaxPrivilegeScope(table.getTablename()));

            tableList.add(table);

            if(table.getOnList() != null) {
                StringBuilder sbOn = new StringBuilder();
                for (ConditionInfo on : table.getOnList()) {
                    if (StringUtils.isEmpty(on.getName()) || on.getValue() == null) {
                        continue;
                    }

                    if(on.getAlgorithm() == null) {
                        on.setAlgorithm(Algorithm.EQ);
                    }

                    sbOn.append(String.format("%s%s%s", DBUtils.getSqlInjText(on.getName()), on.getAlgorithm().getValue(), DBUtils.getSqlInjText(on.getValue())));
                }

                tableOnMap.put(table.getTablename(), sbOn.toString());
            }
        }

        Join join = firstQuery.as(tableList.get(0).getAlias())
                .join(tableList.get(1).getJoinMode(), (Queryable)tableList.get(1).getEntity(), tableList.get(1).getAlias());

        if(tableList.size() > 2) {
            for (int j=2; j<tableList.size();j++) {
                join = join.on(tableOnMap.get(tableList.get(j-1).getTablename())).select("*")
                        .join(tableList.get(j).getJoinMode(), (Queryable) tableList.get(j).getEntity(), tableList.get(j).getAlias());
            }
        }

        return queryInfo
                .getWhereQuerableByJoin(tableList,
                        join.on(tableOnMap.get(tableList.get(tableList.size() - 1).getTablename())) );
    }

    /***
     * query select data
     * @param table
     * @param queryInfo
     * @return
     * @throws Exception
     */
    public List query(String table, QueryInfo queryInfo) throws Exception {
        return query(table, queryInfo, false);
    }

    /***
     * query select data
     * @param table
     * @param queryInfo
     * @return
     * @throws Exception
     */
    public List query(String table, QueryInfo queryInfo, boolean byExport) throws Exception {
        Object entity = EntityContext.getEntity(table, queryInfo);
        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        encrypt(queryInfo.getConditionList());

        if(byExport) {

            List<ColumnInfo> columns = DynamicClassBuilder.getColumnInfosBySelectList(queryInfo.getSelectList());

            Object info = DynamicClassBuilder.create(table, columns, false);

            queryInfo.getSelectList().clear();

            Where where = queryInfo.getWhereQuerable(table, entity, getCurrentMaxPrivilegeScope(table));

            QueryableAction ac = getQueryableAction(queryInfo, where, false);

            return ac.query(info.getClass(), queryInfo.getSkip(), queryInfo.getPageInfo().getPageSize());
        }


        Where where = queryInfo.getWhereQuerable(table, entity, getCurrentMaxPrivilegeScope(table));

        QueryableAction ac = getQueryableAction(queryInfo, where, false);

        return ac.query(Map.class, queryInfo.getSkip(), queryInfo.getPageInfo().getPageSize());
    }

    /***
     * query select data and update
     * @param table
     * @param queryInfo
     * @return
     * @throws Exception
     */
    public boolean updateByQuery(String table, QueryInfo queryInfo) throws Exception {

        Object entity = EntityContext.getEntity(table, queryInfo);
        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        if(queryInfo == null) {
            throw new Exception(LangConfig.getInstance().get("invalid_post_data"));
        }

        Map<String, Object> postData = queryInfo.getData();

        encrypt(postData);

        encrypt(queryInfo.getConditionList());

        ensureJsonData(postData);

        Where where = queryInfo.getWhereQuerable(table, entity, getCurrentMaxPrivilegeScope(table));

        return where.update(postData);
    }

    protected QueryableAction getQueryableAction(QueryInfo queryInfo, Where where, boolean isMutilTable) throws Exception {
        QueryableAction ac = queryInfo.getSelectQuerable(where, isMutilTable);

        long total = 0;

        if(!ignoreTotalCount) {
            Single<Long> single = ac.asyncCount();
            total = single.blockingGet();
        }

        if(queryInfo.getPageInfo() == null) {
            queryInfo.setPageInfo(new PageInfo());
        }

        queryInfo.getPageInfo().setTotalRecords(total);

        GroupBy groupBy = queryInfo.getGroupByQuerable(where);
        if(groupBy != null) {
            ac = queryInfo.getSelectQuerable(groupBy);
        }

        OrderBy orderBy = queryInfo.getOrderByQuerable(groupBy);
        if(orderBy != null) {
            ac = queryInfo.getSelectQuerable(orderBy);
        }

        else {
            orderBy = queryInfo.getOrderByQuerable(where);
            if(orderBy != null) {
                ac = queryInfo.getSelectQuerable(orderBy);
            }
        }

        if (maxPageSize != null && queryInfo.getPageInfo().getPageSize() > maxPageSize) {
            queryInfo.getPageInfo().setPageSize(maxPageSize);
        }

        return ac;
    }

    /***
     * get data by id
     * @param table
     * @param id
     * @return
     * @throws Exception
     */
    public Map get( String table, String id ) throws Exception {
        Object entity = EntityContext.getEntityId(getCurrentDatasourceId(), table, id);
        if(entity == null) {
            new Exception("Can not find entity!!!");
        }

        Where where = queryInfo.getWhereQueryableById(entity, id);

        Map data = null;
        data = (Map) where.first(Map.class);

        if(data == null) {
            return data;
        }

        if(!checkDataPrivilege(table, data)) {
            new Exception(NO_PRIVILEGE_MESSAGE);
        }

        decrypt(data);

        return data;
    }

    /***
     * Create or Alter Table
     * @param table
     * @param columns
     * @throws Exception
     */
    public void createOrAlterTable(String table, List<ColumnInfo> columns) throws Exception {

        UserModel user = getLoginUser();
        if(user == null || !admin.equals(user.getUsername())) {
            throw new Exception(LangConfig.getInstance().get("you_are_not_administrator"));
        }

        Object entity = EntityContext.getEntity(table, queryInfo);
        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        Queryable query = (Queryable)entity;

        if(Queryable.exist(query.dataSource().getId(), table)) {
            Queryable.alterTable(query.dataSource().getId(), table, columns);
        }

        else {
            Queryable.createTable(query.dataSource().getId(), table, columns);
        }
    }

    /***
     * Create or Alter View
     * @param viewName
     * @param queryInfo
     * @throws Exception
     */
    public void createOrAlterView(String viewName, QueryInfo queryInfo) throws Exception {

        UserModel user = getLoginUser();
        if(user == null || !admin.equals(user.getUsername())) {
            throw new Exception(LangConfig.getInstance().get("you_are_not_administrator"));
        }

        if(queryInfo.getJoinTables() == null || queryInfo.getJoinTables().size() < 1) {
            throw new Exception(LangConfig.getInstance().get("join_tables_can_not_be_empty"));
        }

        if(queryInfo.getJoinTables().size() < 2) {
            throw new Exception(LangConfig.getInstance().get("join_tables_can_not_be_less_tow"));
        }

        encrypt(queryInfo.getConditionList());

        Queryable q = null;
        String[] aliases =  { "a", "b", "c", "d", "e", "f", "g", "h", "i",
                "j", "k", "l", "m", "n", "o", "p", "q", "e", "r", "s", "t",
                "u", "v", "w", "x", "y", "z" };

        int i = 0;
        Map<String, String> tableOnMap = new HashMap<String, String>();
        List<TableInfo> tableList = new ArrayList<TableInfo>();
        for(TableInfo table : queryInfo.getJoinTables()) {

            if(StringUtils.isEmpty(table.getTablename())) {
                continue;
            }

            Object entity = EntityContext.getEntity(table.getTablename(), queryInfo);

            Queryable query = (Queryable) entity;
            if(query == null) {
                continue;
            }

            else if (q == null){
                q = query;
            }

            table.setEntity(entity);
            if(StringUtils.isEmpty(table.getAlias())) {
                table.setAlias(aliases[i]);
                i++;
            }

            table.setPrivilegeScope(getCurrentMaxPrivilegeScope(table.getTablename()));

            tableList.add(table);

            StringBuilder sbOn = new StringBuilder();
            for(ConditionInfo on : table.getOnList()) {
                if(StringUtils.isEmpty(on.getName()) || on.getValue() == null) {
                    continue;
                }

                sbOn.append(String.format("[%s]%s%s", on.getName(), on.getAlgorithm().getValue(), on.getValue()));
            }

            tableOnMap.put(table.getTablename(), sbOn.toString());
        }

        Join join = q.as(tableList.get(0).getAlias())
                .join(tableList.get(1).getJoinMode(), q, tableList.get(1).getAlias());

        if(tableList.size() > 2) {
            for (int j=2; j<tableList.size();j++) {
                join = join.on(tableOnMap.get(tableList.get(j))).select("*")
                        .join(tableList.get(j).getJoinMode(), q, tableList.get(j).getAlias());
            }
        }

        Where where = queryInfo
                .getWhereQuerableByJoin(tableList,
                        join.on(tableOnMap.get(tableList.get(tableList.size() - 1))) );

        String datasourceId = (String) ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE);
        if(Queryable.exist(datasourceId, viewName)) {
            where.select("*").alterView(viewName);
        }

        else {
            where.select("*").createView(viewName);
        }

    }

    /***
     * update data
     * @param table
     * @param id
     * @param postData
     * @return
     * @throws Exception
     */
    public Integer update(String table, String id, Map<String, Object> postData) throws Exception {
        Object entity = EntityContext.getEntity(table, postData, id);

        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        encrypt(postData);

        fillData(postData, entity);

        ensureJsonData(postData);

        Where where = queryInfo.getWhereQueryableById(entity, id);

        Map data = (Map) where.first(Map.class);
        if(data == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_data_for_update"));
        }

        if(!checkDataPrivilege(table, data)) {
            throw new Exception(NO_PRIVILEGE_MESSAGE);
        }

        Integer result = 0;
        if(where.update(postData)) {
            result = 1;

            indexingContext.createIndex(((Queryable)entity).tablename(), postData);

            String currentDatasource = getCurrentDatasourceId();
            deleteFileByFieldname(table, data, currentDatasource);
        }

        return result;
    }

    /***
     * insert data
     * @param table
     * @param postData
     * @return
     * @throws Exception
     */
    public Object insert(String table, Map<String, Object> postData) throws Exception {
        String result = insert(table, postData, null);
        if(Pattern.compile("^\\d+$").matcher(result).find()) {
            Integer.parseInt(result);
        }

        return result;
    }

    /***
     * insert data
     * @param table
     * @param postData
     * @return
     * @throws Exception
     */
    public String insert(String table, Map<String, Object> postData, String idField) throws Exception {
        Object entity = EntityContext.getEntity(table, postData);
        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        encrypt(postData);

        fillData(postData, entity);

        ensureJsonData(postData);

        Queryable queryable = ((Queryable) entity);

        if(StringUtils.isEmpty(idField)) {
            ColumnInfo primary = EntityContext.getPrimaryKey(getCurrentDatasourceId(), table);
            if (primary != null) {
                if (!"int".equals(primary.getDataType()) && !"bigint".equals(primary.getType())) {
                    idField = primary.getColumnName();
                }
            }
        }

        if(StringUtils.isEmpty(idField)) {
            Integer result = queryable.insert();
            indexingContext.createIndex(((Queryable)entity).tablename(), postData);
            if(result == null) {
                return "0";
            }

            return result.toString();
        }

        DruidPooledConnection conn = queryable.dataSource().getConnection();
        conn.setAutoCommit(false);
        queryable.insert();
        Object idValue = queryable.orderby("createOn desc").select(idField).first(String.class);
        conn.commit();

        return idValue.toString();
    }

    protected void ensureJsonData(Map<String, Object> data) {

        if(data != null) {
            for(Map.Entry entry : data.entrySet()) {
                if(entry.getValue() == null) {
                    continue;
                }
                if(entry.getValue() instanceof JSONObject) {
                    entry.setValue(entry.getValue().toString());
                }
            }
        }
    }

    public Long count(String table, QueryInfo queryInfo) throws Exception {
        Object entity = EntityContext.getEntity(table, queryInfo);
        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        encrypt(queryInfo.getConditionList());

        Where where = queryInfo.getWhereQuerable(table, entity, getCurrentMaxPrivilegeScope(table));

        QueryableAction ac = where;

        Long result = Long.valueOf(0);

        result = ac.count();

        return result;
    }

    public Boolean exist(String table, QueryInfo queryInfo) throws Exception {
        Object entity = EntityContext.getEntity(table, queryInfo);
        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        encrypt(queryInfo.getConditionList());

        Where where = queryInfo.getWhereQuerable(table, entity, getCurrentMaxPrivilegeScope(table));

        QueryableAction ac = where;

        Boolean result = false;
        result = ac.exist();

        return result;
    }

    protected void download(String table, String field, String id) throws Exception {
        DownloadData downloadData = new DownloadData(getCurrentDatasourceId(), table, field, id).invoke();

        TriggerContext.exec(table, EventType.Download, downloadData, request);

        byte[] buffer = preDownloadProcess(downloadData, downloadData.getMediaType());

        download(downloadData.getFilename(), downloadData.getMimeType(), buffer);
    }

    protected void download(String filename, String mimeType, byte[] data) throws IOException {

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" +
                URLEncoder.encode(filename, "UTF-8") );
        response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);

        ServletOutputStream output = response.getOutputStream();
        output.write(data);
    }

    protected void export(String filename, List data, QueryInfo queryInfo) throws IOException {

        if(getLoginUser() == null) {
            throw new IOException("login please!!!");
        }

        if(data.size() < 1) {
            throw new IOException("can not find data!!!");
        }

        filename = filename + ExcelTypeEnum.XLSX.getValue();
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" +
                URLEncoder.encode(filename, "UTF-8") );
        response.setHeader(HttpHeaders.CONTENT_TYPE, UploadUtils.getMIMEType("xlsx"));

        EasyExcel.write(response.getOutputStream()).sheet().head(data.get(0).getClass()).doWrite(data);
    }

    protected Mono downloadAs(String table, String field, String id) throws Exception {
        DownloadData downloadData = new DownloadData(getCurrentDatasourceId(), table, field, id).invoke();

        TriggerContext.exec(table, EventType.Download, downloadData, request);

        byte[] buffer = preDownloadProcess(downloadData, downloadData.getMediaType());

        return downloadAs(downloadData.getFilename(), buffer);
    }

    protected Mono downloadAs(String filename, byte[] data) throws IOException {

        ByteArrayResource resource = new ByteArrayResource(data);

        return ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"))
                .contentType(new MediaType("application", "force-download"))
                .body(BodyInserters.fromResource(resource)).switchIfEmpty(Mono.empty());
    }

    protected Mono exportAs(String filename, List data, QueryInfo queryInfo) throws IOException {

        List<ColumnInfo> columns = DynamicClassBuilder.getColumnInfosBySelectList(queryInfo.getSelectList());

        Object entity = DynamicClassBuilder.create(getTablename(), columns);

        filename = filename + ExcelTypeEnum.XLSX.getValue();

        response.setHeader(HttpHeaders.CONTENT_TYPE, UploadUtils.getMIMEType("xlsx"));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        EasyExcel.write(bos).sheet().head(entity.getClass()).doWrite(data);

        ByteArrayResource resource = new ByteArrayResource(bos.toByteArray());

        return ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"))
                .contentType(new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(BodyInserters.fromResource(resource)).switchIfEmpty(Mono.empty());
    }

    protected void preview(String table, String field, String id, Integer page) throws Exception {
        DownloadData downloadData = new DownloadData(getCurrentDatasourceId(), table, field, id, page).invoke();

        TriggerContext.exec(table, EventType.PreviewDoc, downloadData, request);

        byte[] buffer = preDownloadProcess(downloadData, downloadData.getMediaType());

        preview(downloadData.getMimeType(), buffer);
    }

    protected void preview(String mimeType, byte[] data) throws IOException {

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline;" );
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);
        ServletOutputStream output = response.getOutputStream();
        output.write(data);
    }

    protected Mono previewAs(String table, String field, String id, Integer page) throws Exception {
        DownloadData downloadData = new DownloadData(getCurrentDatasourceId(), table, field, id, page).invoke();
        if(downloadData.getMimeType().indexOf("image") != 0) {
            throw new  Exception(LangConfig.getInstance().get("not_support_file_format"));
        }

        TriggerContext.exec(table, EventType.PreviewDoc, downloadData, request);

        byte[] buffer = preDownloadProcess(downloadData, downloadData.getMediaType());

        return previewAs(downloadData.getMimeType(), buffer);
    }

    protected Mono previewAs(String mimeType, byte[] data) throws IOException {

        ByteArrayResource resource = new ByteArrayResource(data);

        return ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline;")
            .contentType(MediaType.valueOf(mimeType)).contentLength(data.length)
            .body(BodyInserters.fromDataBuffers(Mono.create(r -> {
                DataBuffer buf = new DefaultDataBufferFactory().wrap(data);
                r.success(buf);
                return;
            })));
    }

    private byte[] preDownloadProcess(DownloadData downloadData, MediaType mediaType) throws IOException, JCodecException {

        if(downloadData.getBuffer() == null) {
            return null;
        }

        if(mediaType.getType().equalsIgnoreCase("image")) {
            BufferedImage image = ImageUtils.getImage(downloadData.getBuffer());

            if(downloadData.isPreview) {
                image = previewProcess(image);
            }

            if(StringUtils.isNotEmpty(watermark)) {
                image = ImageUtils.watermark(image, watermark, new Color(41, 35, 255, 33), new Font("微软雅黑", Font.PLAIN, 35));
            }
            return ImageUtils.toBytes(image, downloadData.getExtension());
        }

        if(mediaType.getType().equalsIgnoreCase("video")) {

            downloadData.setMimeType("image/jpeg");
            byte[] bytes = VideoUtils.getThumbnail(new File(downloadData.path), scalRatio);
            downloadData.cleanTempFile();

            return bytes;
        }

        String extesion = UploadUtils.getExtesion(mediaType.toString());
        if(extesion.equalsIgnoreCase("ppt") ||
                extesion.equalsIgnoreCase("pptx")) {

            downloadData.setMimeType("image/jpeg");
            BufferedImage image = OfficeUtils.getPageImageByPPT(downloadData.getBuffer(), downloadData.page, extesion);
            if(image == null) {
                return null;
            }
            byte[] bytes = ImageUtils.toBytes(image);

            return bytes;
        }

        return downloadData.getBuffer();
    }

    private BufferedImage previewProcess(BufferedImage image) throws IOException {

        if (scalRatio > 0 || fixedWidth > 0) {

            if (scalRatio > 0) {
                image = ImageUtils.zoomImage(image, scalRatio);
            }

            if (fixedWidth > 0) {
                image = ImageUtils.resizeImage(image, fixedWidth);
            }
        }

        return image;
    }

    protected Map<String, Object> upload(String table, String field, Map<String, Object> uploadFiles) throws Exception {

        Map<String, Object> result = new HashMap<String, Object>();
        if(uploadFiles == null) {
            throw new IOException("request error!!!");
        }

        if(getLoginUser() == null) {
            throw new IOException("login please!!!");
        }

        List<Map.Entry<String, Object>> files = uploadFiles.entrySet().stream()
                .filter(a -> a.getKey().indexOf("_upload_filename") == -1).collect(Collectors.toList());
        if(files == null || files.size() < 1) {
            throw new IOException("files can not be empty!!!");
        }

        String currentDatasource = "default";
        if(ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE) != null) {
            currentDatasource = ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE).toString();
        }

        Map<String, Object> configMap = ApplicationConfig.getInstance().getMap(String.format("entity.upload.%s.%s.%s", currentDatasource, table, field));
        if(configMap == null || configMap.size() < 1) {
            throw new IOException(String.format("can not find upload config for %s.%s!!!", table, field));
        }

        if(configMap.get("path") == null) {
            throw new IOException("can not find path for upload config!!!");
        }

        for(Map.Entry<String, Object> fileEntry : files) {

            String tempKey = String.format("%s_upload_filename", fileEntry.getKey());
            if(!uploadFiles.containsKey(tempKey)) { //没有文件名的不是文件
                continue;
            }

            String filename = uploadFiles.get(tempKey).toString();
            String[] arr = filename.split("\\.");
            String extName = arr[arr.length - 1].toLowerCase();

            byte[] fileBytes = ImageUtils.getBytesForBase64(fileEntry.getValue().toString());

//            MagicMatch mimeMatcher = Magic.getMagicMatch(fileBytes, true);
            String mimeType = UploadUtils.getMIMEType(extName); //mimeMatcher.getMimeType();

            if(StringUtils.isEmpty(mimeType)) {
                continue;
            }

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
            String root = configMap.get("path").toString();
            if(root.lastIndexOf("/") == root.length() - 1 ||
                    root.lastIndexOf("\\") == root.length() - 1) {
                root = root.substring(0, root.length() - 2);
            }
            root = String.format("%s/%s/%s/%s", root, currentDatasource, table, field);

            value = UploadUtils.upload(root, filename, fileBytes);

            result.put(fileEntry.getKey(), value);

            if("ppt".equals(extName) || "pptx".equals(extName)) {
                result.put("pageCount", OfficeUtils.getPageCountByPPT(extName, fileBytes));
            }
        }

        return result;
    }

    protected void importExcel(String table, Map<String, Object> uploadFiles) throws Exception {

        if(uploadFiles == null) {
            throw new IOException("request error!!!");
        }

        if(getLoginUser() == null) {
            throw new IOException("login please!!!");
        }

        List<Map.Entry<String, Object>> files = uploadFiles.entrySet().stream()
                .filter(a -> a.getKey().indexOf("_upload_filename") == -1).collect(Collectors.toList());
        if(files == null || files.size() < 1) {
            throw new IOException("files can not be empty!!!");
        }

        String currentDatasource = "default";
        if(ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE) != null) {
            currentDatasource = ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE).toString();
        }

        for(Map.Entry<String, Object> fileEntry : files) {

            String tempKey = String.format("%s_upload_filename", fileEntry.getKey());
            String filename = uploadFiles.get(tempKey).toString();
            String[] arr = filename.split("\\.");
            byte[] fileBytes = ImageUtils.getBytesForBase64(fileEntry.getValue().toString());
            String extName = arr[arr.length - 1];

            //MagicMatch mimeMatcher = Magic.getMagicMatch(fileBytes, true);
            String mimeType = UploadUtils.getMIMEType(extName); //mimeMatcher.getMimeType();

            if(StringUtils.isEmpty(mimeType)) {
                continue;
            }


            List<String> fieldList = new ArrayList<String>();
            List<SheetHeaderModel> headerList = OfficeUtils.getHeadersByExcel(fileBytes, fieldList);

            Queryable entity = (Queryable) EntityContext.getEntity(table, fieldList);

            EasyExcel.read(new ByteArrayInputStream(fileBytes), entity.getClass(), new ExcelListener(table, entity, headerList)).sheet().doRead();
        }
    }

    protected void importPPT(String table, Map<String, Object> uploadFiles) throws Exception {

        if(uploadFiles == null) {
            throw new IOException("request error!!!");
        }

        if(getLoginUser() == null) {
            throw new IOException("login please!!!");
        }

        List<Map.Entry<String, Object>> data = uploadFiles.entrySet().stream()
                .filter(a -> a.getKey().indexOf("_upload_filename") == -1 &&
                        !a.getKey().equals("save_full_text") &&
                        !a.getKey().equals("save_source_ppt")).collect(Collectors.toList());
        if(data == null || data.size() < 1) {
            throw new IOException("Post data can not be empty!!!");
        }

        Boolean save_full_text = uploadFiles.entrySet().stream()
                .filter(a-> a.getKey().equals("save_full_text") && Boolean.TRUE.equals(a.getValue()))
                .isParallel();

        Boolean save_source_ppt = uploadFiles.entrySet().stream()
                .filter(a-> a.getKey().equals("save_full_text") && Boolean.TRUE.equals(a.getValue()))
                .isParallel();

        String currentDatasource = "default";
        if(ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE) != null) {
            currentDatasource = ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE).toString();
        }

        Map<String, Object> fieldSet = new HashMap<String, Object>();
        Map<String, Object> pptSet = new HashMap<String, Object>();
        for(Map.Entry<String, Object> entry : data) {
            String tempKey = String.format("%s_upload_filename", entry.getKey());
            if (!uploadFiles.containsKey(tempKey)) { //没有文件名的不是文件
                fieldSet.put(entry.getKey(), entry.getValue());
            }
            else {
                pptSet.put(entry.getKey(), entry.getValue());
            }
        }

        List<Map<String, Object>> resultSet = new ArrayList<Map<String, Object>>();
        for(Map.Entry<String, Object> entry : pptSet.entrySet()) {

            String tempKey = String.format("%s_upload_filename", entry.getKey());
            String filename = uploadFiles.get(tempKey).toString();
            String[] arr = filename.split("\\.");
            String extName = arr[arr.length - 1];

            byte[] fileBytes = ImageUtils.getBytesForBase64(entry.getValue().toString());

            //转换成图片
            OfficeUtils.saveImageByPPT(table, currentDatasource, resultSet, entry, filename, fileBytes);

            //保存ppt源文件
            if(save_source_ppt) {
                String pprSourcePath = String.format("/preview/ppt/%s/%s/%s/source", currentDatasource, table, entry.getKey());
                String value = UploadUtils.upload(pprSourcePath, filename, fileBytes);
                Map<String, Object> result = new HashMap<String, Object>();
                result.put(entry.getKey(), value);
                result.put("number", 0);
                resultSet.add(result);
            }

            //创建全文索引
            if(save_full_text) {
                String value = OfficeUtils.getTextByPPT(fileBytes);
                Map<String, Object> result = new HashMap<String, Object>();
                result.put(entry.getKey(), value);
                result.put("number", 0);
                resultSet.add(result);
            }
        }

        //准备实体所需的字段
        List<String> fields = new ArrayList<String>();
        for(String field : fieldSet.keySet()) {
            fields.add(field);
        }
        for(Map<String, Object> item : resultSet) {
            for(String field : item.keySet()) {
                if(fields.contains(field)) {
                    continue;
                }
                fields.add(field);
            }
        }

        //保存到数据库
        Queryable query = (Queryable)EntityContext.getEntity(table, fields);
        for(Map<String, Object> item : resultSet) {
            for(String field : item.keySet()) {
                if(item.get(field) == null) {
                    continue;
                }
                ReflectionUtils.setFieldValue(query, field, item.get(field));
            }
            for(String field : fieldSet.keySet()) {
                if(fieldSet.get(field) == null) {
                    continue;
                }
                ReflectionUtils.setFieldValue(query, field, fieldSet.get(field));
            }
            query.insert();
        }
    }

    protected void playVideo(String table, String field, String id) throws Exception {

        String currentDatasource = getCurrentDatasourceId();
        Map<String, Object> uploadConfigMap = ApplicationConfig.getInstance().getMap(
                String.format("entity.upload.%s.%s.%s", currentDatasource, table, field)
        );
        if(uploadConfigMap == null || uploadConfigMap.size() < 1) {
            throw new IOException("can not find the upload config!!!");
        }

        if(uploadConfigMap.get("path") == null) {
            throw new IOException("can not find the upload path on config!!!");
        }

        Map<String, Object> data = get(table, id);
        if(!data.containsKey(field)) {
            throw new Exception(LangConfig.getInstance().get("wrong_field_name"));
        }

        decrypt(data);

        String content = data.get(field).toString();
        DownloadData downloadData = new DownloadData(getCurrentDatasourceId(), table, field, id);
        String filePath = downloadData.getFullPath(content, currentDatasource, uploadConfigMap);
        if(!(new File(filePath)).exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
            return;
        }

        TriggerContext.exec(table, EventType.PlayVideo, downloadData, request);

        if (!StringUtils.isEmpty(downloadData.getMediaType().getType())) {
            response.setContentType(downloadData.getMediaType().getType());
        }
        request.setAttribute(NonStaticResourceHttpRequestHandler.ATTR_FILE, filePath);
        nonStaticResourceHttpRequestHandler.handleRequest(request, response);
    }

    public class DownloadData {
        private final String datasourceId;
        private final String table;
        private final String field;
        private final String id;
        private String[] arrMessage;
        private byte[] buffer;
        private MediaType mediaType;
        private String path;
        private String tempFilePath;
        private boolean isPreview;
        private int page;

        public DownloadData(String datasourceId, String table, String field, String id) {
            this.table = table;
            this.field = field;
            this.id = id;
            this.datasourceId = datasourceId;
            this.isPreview = false;
            this.page = 0;
        }

        public DownloadData(String datasourceId, String table, String field, String id, int page) {
            this.table = table;
            this.field = field;
            this.id = id;
            this.datasourceId = datasourceId;
            this.isPreview = true;
            this.page = page;
        }

        public String getFilename() {
            return arrMessage[2];
        }

        public String getExtension() {
            return arrMessage[1];
        }

        public String getMimeType() {
            return arrMessage[0];
        }

        public void setMimeType(String mimeType) {
            arrMessage[0] = mimeType;
        }

        public byte[] getBuffer() {
            return buffer;
        }

        public DownloadData invoke() throws Exception {
            Map<String, Object> data = get(table, id);
            if(!data.containsKey(field)) {
                throw new Exception(LangConfig.getInstance().get("wrong_field_name"));
            }

            decrypt(data);

            String content = data.get(field).toString();

            if(StringUtils.isEmpty(content)) {
                throw new Exception(LangConfig.getInstance().get("image_field_is_empty"));
            }

            Map<String, Object> uploadConfigMap = ApplicationConfig.getInstance().getMap(
                    String.format("entity.upload.%s.%s.%s", datasourceId, table, field)
            );
            if(uploadConfigMap == null || uploadConfigMap.size() < 1) {
                throw new IOException("can not find the upload config!!!");
            }

            if(uploadConfigMap.get("path") != null) {
                this.path = getFullPath(content, datasourceId, uploadConfigMap);
                File file = new File(this.path);
                if("/".equals(this.path.substring(0,1)) && !file.exists()) {
                    this.path = String.format("%s%s", System.getProperty("user.dir"), this.path);
                    file = new File(this.path);
                }
                buffer = UploadUtils.getFileByteArray(file);
            }

            else {
                int splitPoint = content.indexOf("|::|");
                String fileString = content.substring(splitPoint + 4);
                String messageBody = content.substring(0, splitPoint);
                arrMessage = messageBody.split("::");
                String[] arr = getMimeType().split("/");

                mediaType = new MediaType(arr[0], arr[1]);

                buffer = new BASE64Decoder().decodeBuffer(fileString);

                saveTempFile(buffer);
            }


            return this;
        }

        public void saveTempFile(byte[] buffer) {
            if(this.isPreview && mediaType.getType().equalsIgnoreCase("video")){
                String filename = UUID.randomUUID().toString().replace("-", "");
                String dir = String.format("%s/temp", System.getProperty("user.dir"));
                this.tempFilePath = dir + "/" + filename;
                FileUtils.save(buffer, dir, filename);
            }
        }

        private String getFullPath(String content, String currentDatasource, Map<String, Object> uploadConfigMap) {
            arrMessage = new String[3];
            String[] tmp = content.split("/");
            String[] fileArr = tmp[tmp.length -1].split("\\.");
            arrMessage[0] = UploadUtils.getMIMEType(fileArr[1]);
            arrMessage[1] = fileArr[1];
            arrMessage[2] = fileArr[0];
            String[] arr = arrMessage[0].split("/");
            mediaType = new MediaType(arr[0], arr[1]);

            String root = uploadConfigMap.get("path").toString();
            if(root.lastIndexOf("/") == root.length() - 1 ||
                    root.lastIndexOf("\\") == root.length() - 1) {
                root = root.substring(0, root.length() - 2);
            }
            if("/".equals(content.substring(0,1))) {
                content = content.substring(1);
            }
            root = String.format("%s/%s/%s/%s", root, currentDatasource, table, field);

            String filePath = String.format("%s/%s", root, content);
            if((new File(System.getProperty("user.dir")+filePath)).exists()) {
                filePath = System.getProperty("user.dir") + filePath;
            }

            return filePath;
        }

        public void cleanTempFile() {

            if(StringUtils.isEmpty(this.tempFilePath)) {
                return;
            }
            FileUtils.delete(this.tempFilePath);
        }

        public MediaType getMediaType() {
            return mediaType;
        }
    }

    protected <T> T getFormatedData(String tablename, T data) throws IOException {

        List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
        boolean needReset = false;
        boolean returnList = false;
        Map<String, Object> map = ApplicationConfig.getInstance().getMap("entity.formatter");
        if(map != null) {

            if(data == null) {
                return null;
            }

            else if(data instanceof String && StringUtils.isEmpty(data.toString())) {
                return null;
            }

            else if(data instanceof Map) {
                dataList.add((Map<String, Object>) data);
            }

            else if(data instanceof List) {
                dataList = (List<Map<String, Object>>) data;
                returnList = true;
            }

            else {
                dataList.add(FastJsonUtils.convert(data, Map.class));
            }

            try {
                for (Map<String, Object> item : dataList) {
                    for (String key : item.keySet()) {
                        Optional opt = map.keySet().stream()
                                .filter(a -> a.equals(key) ||
                                        String.format("%s.%s", tablename, key).equals(a))
                                .findAny();

                        if (opt.isPresent() && item.get(key) != null) {
                            if (item.get(key) instanceof Date) {
                                item.put(key, Datetime.format((Date) item.get(key), opt.get().toString()));
                                needReset = true;
                            } else if (item.get(key) instanceof Long || item.get(key).getClass().equals(long.class)) {
                                item.put(key, Datetime.format((Date) StringUtils.cast(Date.class, item.get(key).toString()),
                                        map.get(opt.get()).toString()));

                                needReset = true;
                            } else if (item.get(key) instanceof Map && ((Map) item.get(key)).containsKey("time") &&
                                    (((Map) item.get(key)).get("time") instanceof Long || ((Map) item.get(key)).get("time").getClass().equals(long.class))) {
                                item.put(key, Datetime.format(new Date((Long) ((Map) item.get(key)).get("time")),
                                        map.get(opt.get()).toString()));

                                needReset = true;
                            } else if (isPrimitive(item.get(key))) {
                                item.put(key, String.format(opt.get().toString(), item.get(key)));
                                needReset = true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e);
            }
        }

        if(needReset) {

            if(returnList) {
                return (T) dataList;
            }

            if(dataList.size() != 1) {
                return (T) dataList;
            }

            return (T) dataList.get(0);
        }

        return data;
    }

    private void successTrigger(ResponseData result, String tablename) throws InvocationTargetException, IllegalAccessException {
        TriggerContext.exec(tablename, EventType.Success, result, request);
    }

    private void errorTrigger(String message, String tablename) {
        if(StringUtils.isEmpty(message)) {
            message = "request error!!!";
        }

        try {
            TriggerContext.exec(tablename, EventType.Error, new Exception(message), request);
        } catch (InvocationTargetException e) {
            log.error(LOG_PRE_SUFFIX + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            log.error(LOG_PRE_SUFFIX + e.getMessage(), e);
        }
    }


    /***
     * search data by elasticSearch
     * @param table
     * @param queryInfo
     * @return
     * @throws Exception
     */
    public SearchData search(String table, QueryInfo queryInfo) throws Exception {
        Object entity = EntityContext.getEntity(table, queryInfo);
        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        encrypt(queryInfo.getConditionList());

        return indexingContext.search(table, queryInfo, Map.class);
    }

    public boolean wechatLogin(String code) throws IOException {
        if(!wechatEnable) {
            throw new RuntimeException("Can not support wechat!!!");
        }

        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, secret, code);

        String result = RequestUtils.get(url);

        Map<String, String> map = JsonUtils.parse(result, Map.class);

        return false;
    }
}
