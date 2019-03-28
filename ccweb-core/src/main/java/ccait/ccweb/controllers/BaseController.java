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


import ccait.ccweb.annotation.OnResult;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.enums.DefaultValueMode;
import ccait.ccweb.enums.EncryptMode;
import ccait.ccweb.enums.PrivilegeScope;
import ccait.ccweb.model.*;
import ccait.ccweb.utils.EncryptionUtil;
import entity.query.*;
import entity.query.core.ApplicationConfig;
import entity.tool.util.DBUtils;
import entity.tool.util.ReflectionUtils;
import entity.tool.util.StringUtils;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.StaticVars.*;
import static entity.tool.util.StringUtils.cast;

public abstract class BaseController {

    private static final Logger log = LogManager.getLogger(BaseController.class);

    public ResponseData<Object> RMessage;

    @Autowired
    protected HttpServletRequest request;

    @Autowired
    protected HttpServletResponse response;

    @Autowired
    private QueryInfo queryInfo;

    @Value("${entity.enableRxJdbc:false}")
    private boolean enableRxJdbc;

    @Value("${entity.security.admin.username:admin}")
    protected String admin;

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

    public BaseController() {
        RMessage = new ResponseData<Object>();
    }
    
    protected boolean isEnableRxJdbc() {
        return enableRxJdbc && (ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE) == null ||
                StringUtils.isEmpty(ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE).toString()));
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

        if(StringUtils.isEmpty(e.getMessage())) {
            return this.result(code, e.toString(), "", null);
        }
        return this.result(code, e.getMessage(), "", null);
    }

    protected ResponseData error(int code, String message) {
        return this.result(code, message, "", null);
    }

    @OnResult
    protected <T> ResponseData<T> result(int code, String message, T data, PageInfo pageInfo) {
        ResponseData<T> result = new ResponseData<T>();

        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        result.setPageInfo(pageInfo);

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

    protected void fillData(@RequestBody Map<String, Object> postData, Object entity) throws IOException {
        List<Field> fields = EntityContext.getFields(entity);
        List<String> argNames = postData.keySet().stream().collect(Collectors.toList());
        for(final String argname : argNames) {

            Optional<Field> opt = fields.stream().filter(a->a.getName().toLowerCase().equals(argname.toLowerCase())).findAny();
            if(!opt.isPresent()) {
                postData.remove(argname);
                continue;
            }

            String fieldName = opt.get().getName();
            Class<?> type = opt.get().getType();
            String valString = DBUtils.getSqlInjValue(postData.get(argname).toString());
            Object value = cast(type, valString);

            String key = fieldName;

            Map<String, Object> defaultValueMap = ApplicationConfig.getInstance().getMap("entity.defaultValue");
            if(defaultValueMap != null) {
                if(!defaultValueMap.containsKey(key)) {
                    key = String.format("%s.%s", getTablename(), fieldName);
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
                if(type.equals(Date.class) && defaultDateByNow) {
                    value = Datetime.now();
                }
            }

            ReflectionUtils.setFieldValue(entity.getClass(), entity, fieldName, value);
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

            encrypt((Map<String, Object>)data, fieldList, EncryptMode.AES);
        }

        if(StringUtils.isNotEmpty(base64Fields)) {

            List<String> fieldList = StringUtils.splitString2List(base64Fields, ",");

            encrypt((Map<String, Object>)data, fieldList, EncryptMode.BASE64);
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

        if(isEnableRxJdbc()) {
            Maybe<UserModel> flow = where.asyncFirst();
            user = flow.blockingGet();
        }

        else {
            user = where.first();
        }

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
        if(isEnableRxJdbc()) {
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
            }

            return result;
        }

        Connection conn = queryInfo.getConnection(entity);

        try {
            conn.setAutoCommit(false);
            for (String id : idList) {
                where = queryInfo.getWhereQueryableById(entity, id);
                Map data = (Map) where.first(Map.class);
                if(data == null) {
                    throw new Exception("Can not find data for delete!!!");
                }

                if(!checkDataPrivilege(table, data)) {
                    throw new Exception(NO_PRIVILEGE_MESSAGE);
                }

                result.add(where.delete());
            }
            conn.commit();

        }
        catch (Exception e) {
            String message = String.format("fail  to delete in [%s]", StringUtils.join(", ", idList));
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            throw new Exception(message);
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
        Map data = (Map) where.first(Map.class);
        if(data == null) {
            throw new Exception("Can not find data for delete!!!");
        }

        if(!checkDataPrivilege(table, data)) {
            throw new Exception(NO_PRIVILEGE_MESSAGE);
        }

        Integer result = null;

        if(isEnableRxJdbc()) {

            Flowable<Integer> flowable = where.asyncDelete();
            result = flowable.blockingFirst();
        }

        else if(where.delete()){
            result = 1;
        }

        return result;
    }


    /***
     * query select data
     * @param queryInfo
     * @return
     * @throws Exception
     */
    public List joinQuery(QueryInfo queryInfo) throws Exception {

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

        return getQueryDataByWhere(queryInfo, where);
    }

    /***
     * query select data
     * @param table
     * @param queryInfo
     * @return
     * @throws Exception
     */
    public List query(String table, QueryInfo queryInfo) throws Exception {
        Object entity = EntityContext.getEntity(table, queryInfo);
        if(entity == null) {
            throw new Exception("Can not find entity!!!");
        }

        encrypt(queryInfo.getConditionList());

        Where where = queryInfo.getWhereQuerable(table, entity, getCurrentMaxPrivilegeScope(table));

        return getQueryDataByWhere(queryInfo, where);
    }

    protected List getQueryDataByWhere(QueryInfo queryInfo, Where where) throws Exception {
        QueryableAction ac = queryInfo.getSelectQuerable(where);

        long total = 0;

        if(!ignoreTotalCount) {
            Single<Long> single = ac.asyncCount();
            total = single.blockingGet();
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

        if(maxPageSize != null && queryInfo.getPageInfo().getPageSize() > maxPageSize) {
            queryInfo.getPageInfo().setPageSize(maxPageSize);
        }

        List list = null;

        if(isEnableRxJdbc()) {
            Flowable<List> flowable = ac.asyncQuery(Map.class, queryInfo.getSkip(), queryInfo.getPageInfo().getPageSize());

            list = flowable.toList().blockingGet();
        }

        else {
            list = ac.query(Map.class, queryInfo.getSkip(), queryInfo.getPageInfo().getPageSize());
        }

        return list;
    }

    /***
     * get data by id
     * @param table
     * @param id
     * @return
     * @throws Exception
     */
    public Map get(String table, String id) throws Exception {
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
        if(isEnableRxJdbc()) {
            Maybe<Map> maybe =  where.asyncFirst(Map.class);
            data = maybe.blockingGet();
        }

        else {
            data = (Map) where.first(Map.class);
        }

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
        Object entity = EntityContext.getEntity(table, postData);
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
        if(isEnableRxJdbc()) {
            Flowable<Integer> flowable = where.asyncUpdate(postData);

            result = flowable.blockingFirst();
        }

        else if(where.update(postData)) {
            result = 1;
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
        Object entity = EntityContext.getEntity(table, postData);
        if(entity == null) {
            throw new Exception("Can not find entity!!!");
        }

        encrypt(postData);

        fillData(postData, entity);

        Object result = null;
        if(isEnableRxJdbc()) {
            Flowable<Integer> flowable = ((Queryable) entity).asyncInsert();

            result = flowable.toList().blockingGet();
        }

        else {
            result = ((Queryable) entity).insert();
        }

        return result;
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

        if(isEnableRxJdbc()) {
            Single<Long> single = ac.asyncCount();

            result = single.blockingGet();
        }

        else {
            result = ac.count();
        }

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
        if(isEnableRxJdbc()) {
            Single<Integer> single = (Single<Integer>) ac.asyncExist().blockingGet();
            result = single.blockingGet() > 0;
        }

        else {
            result = ac.exist();
        }

        return result;
    }
}
