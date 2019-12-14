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


import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.context.IndexingContext;
import ccait.ccweb.context.TriggerContext;
import ccait.ccweb.dynamic.DynamicClassBuilder;
import ccait.ccweb.enums.*;
import ccait.ccweb.listener.ExcelListener;
import ccait.ccweb.model.*;
import ccait.ccweb.utils.EncryptionUtil;
import ccait.ccweb.utils.FastJsonUtils;
import ccait.ccweb.utils.ImageUtils;
import ccait.ccweb.utils.UploadUtils;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import entity.query.*;
import entity.query.annotation.PrimaryKey;
import entity.query.core.ApplicationConfig;
import entity.tool.util.DBUtils;
import entity.tool.util.ReflectionUtils;
import entity.tool.util.StringUtils;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.apache.http.HttpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ccait.ccweb.dynamic.DynamicClassBuilder.smallHump;
import static ccait.ccweb.utils.StaticVars.*;
import static entity.tool.util.StringUtils.cast;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

public abstract class BaseController {

    private static final Logger log = LogManager.getLogger(BaseController.class);

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

    public BaseController() {
        RMessage = new ResponseData<Object>();
    }


    @PostConstruct
    private void construct() {
        admin = ApplicationConfig.getInstance().get("${entity.security.admin.username}", admin);
        fixedWidth = Integer.parseInt(ApplicationConfig.getInstance().get("${entity.download.thumb.fixedWidth}", fixedWidth.toString()));
        scalRatio = Integer.parseInt(ApplicationConfig.getInstance().get("${entity.download.thumb.scalRatio}", scalRatio.toString()));
        md5Fields = ApplicationConfig.getInstance().get("${entity.security.encrypt.MD5.fields}", md5Fields);
        md5PublicKey = ApplicationConfig.getInstance().get("${entity.security.encrypt.MD5.publicKey}", md5PublicKey);
        base64Fields = ApplicationConfig.getInstance().get("${entity.security.encrypt.BASE64.fields}", base64Fields);
        macFields = ApplicationConfig.getInstance().get("${entity.security.encrypt.MAC.fields}", macFields);
        shaFields = ApplicationConfig.getInstance().get("${entity.security.encrypt.SHA.fields}", shaFields);
        macPublicKey = ApplicationConfig.getInstance().get("${entity.security.encrypt.MAC.publicKey}", macPublicKey);
        aesFields = ApplicationConfig.getInstance().get("${entity.security.encrypt.AES.fields}", aesFields);
        aesPublicKey = ApplicationConfig.getInstance().get("${entity.security.encrypt.AES.publicKey}", aesPublicKey);
        encoding = ApplicationConfig.getInstance().get("${entity.encoding}", encoding);
        userPathField = ApplicationConfig.getInstance().get("${entity.table.reservedField.userPath}", userPathField);
        groupIdField = ApplicationConfig.getInstance().get("${entity.table.reservedField.groupId}", groupIdField);
        createByField = ApplicationConfig.getInstance().get("${entity.table.reservedField.createBy}", createByField);

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
                String valString = DBUtils.getSqlInjValue(postData.get(argname).toString());
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

                if(!data.containsKey(groupIdField)) { //公开数据
                    return true;
                }

                if(data.get(groupIdField) != null) {

                    Optional<UserGroupRoleModel> opt = getLoginUser().getUserGroupRoleModels().stream()
                            .filter(a -> a.getGroupId().equals(data.get(groupIdField))).findAny();

                    if(opt!= null && opt.isPresent()) { //存在相同的组
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

        if(StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
            throw new Exception("Username and password can not be empty!!!");
        }

        user.setPassword(md5(user.getPassword()));

        entity.query.Where<UserModel> where = user.where("[username]=#{username}").and("[password]=#{password}");

        user = where.first();

        if(user == null) {
            throw new Exception("Username or password is invalid!!!");
        }

        if(!user.getStatus().equals(0)) {
            throw new Exception("user status has been frozen!!!");
        }

        user.getUserGroupRoleModels().stream().forEach((item)->{
            item.getGroup();
            item.getRole();
        });

        user.setPassword("******");

        setLoginUser(user);

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
        Object entity = EntityContext.getEntityId(table, strid);
        if(entity == null) {
            throw new Exception("Can not find entity!!!");
        }

        Where where = null;
        for (String id : idList) {
            where = queryInfo.getWhereQueryableById(entity, id);
            Maybe<Map> flow = where.asyncFirst(Map.class);
            Map data = flow.blockingGet();
            if(data == null) {
                throw new Exception("Can not find data for delete!!!");
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
        Object entity = EntityContext.getEntityId(table, id);
        if(entity == null) {
            throw new Exception("Can not find entity!!!");
        }

        Where where = queryInfo.getWhereQueryableById(entity, id);
        Map<String, Object> data = (Map<String, Object>) where.first(Map.class);
        if(data == null) {
            throw new Exception("Can not find data for delete!!!");
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
    public List joinQuery(QueryInfo queryInfo, boolean bySelectInfos) throws Exception {

        Where where = getWhereQueryableByJoin(queryInfo);

        QueryableAction ac = getQueryableAction(queryInfo, where, true);

        if(bySelectInfos && queryInfo.getSelectList().size() > 0) {

            List<ColumnInfo> columns = DynamicClassBuilder.getColumnInfosBySelectList(queryInfo.getSelectList());

            Object info = DynamicClassBuilder.create("TABLE" + UUID.randomUUID().toString().replace("-", ""), columns, false);

            return ac.query(info.getClass(), queryInfo.getSkip(), queryInfo.getPageInfo().getPageSize());
        }

        return ac.query(Map.class, queryInfo.getSkip(), queryInfo.getPageInfo().getPageSize());
    }

    private Where getWhereQueryableByJoin(QueryInfo queryInfo) throws Exception {
        if(queryInfo.getJoinTables() == null || queryInfo.getJoinTables().size() < 1) {
            throw new Exception("join tables can not be empty!!!");
        }

        if(queryInfo.getJoinTables().size() < 2) {
            throw new Exception("join tables can not be less tow!!!");
        }

        if(queryInfo.getJoinTables().stream().filter(a->a.getOnList() == null ||
                a.getOnList().size() < 1).count() == queryInfo.getJoinTables().size()) {
            throw new Exception("onList can not be empty!!!");
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

                    sbOn.append(String.format("%s%s%s", DBUtils.getSqlInjValue(on.getName()), on.getAlgorithm().getValue(), DBUtils.getSqlInjValue(on.getValue())));
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
    public List query(String table, QueryInfo queryInfo, boolean bySelectInfos) throws Exception {
        Object entity = EntityContext.getEntity(table, queryInfo);
        if(entity == null) {
            throw new Exception("Can not find entity!!!");
        }

        encrypt(queryInfo.getConditionList());

        Where where = queryInfo.getWhereQuerable(table, entity, getCurrentMaxPrivilegeScope(table));

        QueryableAction ac = getQueryableAction(queryInfo, where, false);

        if(bySelectInfos && queryInfo.getSelectList().size() > 0) {

            List<ColumnInfo> columns = DynamicClassBuilder.getColumnInfosBySelectList(queryInfo.getSelectList());

            Object info = DynamicClassBuilder.create(getTablename(), columns, false);

            return ac.query(info.getClass(), queryInfo.getSkip(), queryInfo.getPageInfo().getPageSize());
        }

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
            throw new Exception("Can not find entity!!!");
        }

        if(queryInfo == null) {
            throw new Exception("Invalid post data!!!");
        }

        Map<String, Object> postData = queryInfo.getData();

        encrypt(postData);

        encrypt(queryInfo.getConditionList());

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
        Object entity = EntityContext.getEntityId(table, id);
        if(entity == null) {
            new Exception("Can not find entity!!!");
        }

        PrimaryKeyInfo pk = EntityContext.getPrimaryKeyInfo(entity);
        if(pk == null) {
            new Exception("Can not find primary key!!!");
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
            throw new Exception("You are not administrator!!!");
        }

        Object entity = EntityContext.getEntity(table, queryInfo);
        if(entity == null) {
            throw new Exception("Can not find entity!!!");
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
            throw new Exception("You are not administrator!!!");
        }

        if(queryInfo.getJoinTables() == null || queryInfo.getJoinTables().size() < 1) {
            throw new Exception("join tables can not be empty!!!");
        }

        if(queryInfo.getJoinTables().size() < 2) {
            throw new Exception("join tables can not be less tow!!!");
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
            throw new Exception("Can not find entity!!!");
        }

        encrypt(postData);

        fillData(postData, entity);

        Where where = queryInfo.getWhereQueryableById(entity, id);

        Map data = (Map) where.first(Map.class);
        if(data == null) {
            throw new Exception("Can not find data for update!!!");
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
    public Integer insert(String table, Map<String, Object> postData) throws Exception {
        String result = insert(table, postData, null);

        return Integer.parseInt(result);
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
            throw new Exception("Can not find entity!!!");
        }

        encrypt(postData);

        fillData(postData, entity);

        Queryable queryable = ((Queryable) entity);

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

    public Long count(String table, QueryInfo queryInfo) throws Exception {
        Object entity = EntityContext.getEntity(table, queryInfo);
        if(entity == null) {
            throw new Exception("Can not find entity!!!");
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
            throw new Exception("Can not find entity!!!");
        }

        encrypt(queryInfo.getConditionList());

        Where where = queryInfo.getWhereQuerable(table, entity, getCurrentMaxPrivilegeScope(table));

        QueryableAction ac = where;

        Boolean result = false;
        result = ac.exist();

        return result;
    }

    protected void download(String table, String field, String id) throws Exception {
        DownloadData downloadData = new DownloadData(table, field, id).invoke();

        TriggerContext.exec(table, EventType.Download, downloadData, request);

        byte[] buffer = preDownloadProcess(downloadData, downloadData.getMediaType(), false);

        download(downloadData.getFilename(), downloadData.getMimeType(), downloadData.getBuffer());
    }

    protected void download(String filename, String mimeType, byte[] data) throws IOException {

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" +
                URLEncoder.encode(filename, "UTF-8") );
        response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);

        ServletOutputStream output = response.getOutputStream();
        output.write(data);
    }

    protected void export(String filename, List data, QueryInfo queryInfo) throws IOException {

        if(queryInfo.getSelectList() == null || queryInfo.getSelectList().size() < 1) {
            throw new IOException("SelectList can not be empty!!!");
        }

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
        DownloadData downloadData = new DownloadData(table, field, id).invoke();

        TriggerContext.exec(table, EventType.Download, downloadData, request);

        byte[] buffer = preDownloadProcess(downloadData, downloadData.getMediaType(), false);

        return downloadAs(downloadData.getFilename(), downloadData.getBuffer());
    }

    protected Mono downloadAs(String filename, byte[] data) throws IOException {

        ByteArrayResource resource = new ByteArrayResource(data);

        return ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"))
                .contentType(new MediaType("application", "force-download"))
                .body(BodyInserters.fromResource(resource)).switchIfEmpty(Mono.empty());
    }

    protected Mono exportAs(String filename, List data, QueryInfo queryInfo) throws IOException {

        if(queryInfo.getSelectList() == null || queryInfo.getSelectList().size() < 1) {
            throw new IOException("SelectList can not be empty!!!");
        }

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

    protected void preview(String table, String field, String id) throws Exception {
        DownloadData downloadData = new DownloadData(table, field, id).invoke();

        TriggerContext.exec(table, EventType.PreviewDoc, downloadData, request);

        byte[] buffer = preDownloadProcess(downloadData, downloadData.getMediaType(), true);

        preview(downloadData.getMimeType(), buffer);
    }

    protected void preview(String mimeType, byte[] data) throws IOException {

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline;" );
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);
        ServletOutputStream output = response.getOutputStream();
        output.write(data);
    }

    protected Mono previewAs(String table, String field, String id) throws Exception {
        DownloadData downloadData = new DownloadData(table, field, id).invoke();
        if(downloadData.getMimeType().indexOf("image") != 0) {
            throw new  Exception("不支持预览的文件格式");
        }

        TriggerContext.exec(table, EventType.PreviewDoc, downloadData, request);

        byte[] buffer = preDownloadProcess(downloadData, downloadData.getMediaType(), true);

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

    private byte[] preDownloadProcess(DownloadData downloadData, MediaType mediaType, boolean isPreview) throws IOException {

        if(mediaType.getType().equalsIgnoreCase("image")) {
            BufferedImage image = ImageUtils.getImage(downloadData.getBuffer());

            if(isPreview) {
                image = previewProcess(image);
            }
            String watermark = ApplicationConfig.getInstance().get("${entity.upload.watermark}", "");
            if(StringUtils.isNotEmpty(watermark)) {
                image = ImageUtils.watermark(image, watermark, new Color(41, 35, 255, 33), new Font("微软雅黑", Font.PLAIN, 35));
            }
            return ImageUtils.toBytes(image, downloadData.getExtension());
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

    protected Map<String, String> upload(String table, String field, Map<String, Object> uploadFiles) throws Exception {

        Map<String, String> result = new HashMap<String, String>();
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
            String extName = arr[arr.length - 1];

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
                    throw new IOException("Can not supported file type!!!");
                }
            }

            if (configMap.get("maxSize") != null) {
                if (fileBytes.length > 1024 * 1024 * Integer.parseInt(configMap.get("maxSize").toString())) {
                    throw new IOException("Upload field to be long!!!");
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
        }

        return result;
    }

    protected void importData(String table, Map<String, Object> uploadFiles) throws Exception {

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

            if (!extName.equalsIgnoreCase("xls") &&
                    extName.equalsIgnoreCase("xlsx")) {
                throw new IOException("Can not supported file type!!!");
            }

            InputStream is = new ByteArrayInputStream(fileBytes);

            Map<String, Object> dataMap = new HashMap<String, Object>();
            List<List<String>> headers = EasyExcel.readSheet().sheetName("schema").build().getHead();
            int count = headers.get(0).size();
            List<SheetHeaderModel> headerList = new ArrayList<SheetHeaderModel>();
            for(int i=0; i<count; i++) {
                SheetHeaderModel headerModel = new SheetHeaderModel();
                headerModel.setHeader(headers.get(0).get(i));
                headerModel.setField(headers.get(1).get(i));
                headerModel.setIndex(i);

                dataMap.put(headers.get(1).get(i), "");
                headerList.add(headerModel);
            }
            Queryable entity = (Queryable) EntityContext.getEntity(table, dataMap);

            EasyExcel.read(is, Map.class, new ExcelListener(table, entity, headerList)).sheet().doRead();
        }
    }

    public class DownloadData {
        private String table;
        private String field;
        private String id;
        private String[] arrMessage;
        private byte[] buffer;
        private MediaType mediaType;

        public DownloadData(String table, String field, String id) {
            this.table = table;
            this.field = field;
            this.id = id;
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

        public byte[] getBuffer() {
            return buffer;
        }

        public DownloadData invoke() throws Exception {
            Map<String, Object> data = get(table, id);
            if(!data.containsKey(field)) {
                throw new Exception("wrong field name!!!");
            }

            decrypt(data);

            String content = data.get(field).toString();

            if(StringUtils.isEmpty(content)) {
                throw new Exception("can not find image in the field!");
            }


            String currentDatasource = getCurrentDatasourceId();
            Map<String, Object> uploadConfigMap = ApplicationConfig.getInstance().getMap(
                    String.format("entity.upload.%s.%s.%s", currentDatasource, table, field)
            );
            if(uploadConfigMap == null || uploadConfigMap.size() < 1) {
                throw new IOException("can not find the upload config!!!");
            }

            if(uploadConfigMap.get("path") != null) {
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

                String fullpath = String.format("%s/%s", root, content);
                File file = new File(fullpath);
                if("/".equals(fullpath.substring(0,1)) && !file.exists()) {
                    file = new File(String.format("%s/%s", System.getProperty("user.dir"), fullpath));
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
            }


            return this;
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
            throw new Exception("Can not find entity!!!");
        }

        encrypt(queryInfo.getConditionList());

        return indexingContext.search(table, queryInfo, Map.class);
    }
}
