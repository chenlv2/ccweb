/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.context;


import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.EncryptionUtil;
import entity.query.ColumnInfo;
import entity.query.Datetime;
import entity.query.Queryable;
import entity.query.core.ApplicationConfig;
import entity.query.core.DataSource;
import entity.query.core.DataSourceFactory;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;


@Service
public class ApplicationContext implements ApplicationContextAware {

    private static final Logger log = LogManager.getLogger( ApplicationContext.class );

    private static org.springframework.context.ApplicationContext instance;

    public static org.springframework.context.ApplicationContext getInstance() {
        return instance;
    }

    private List<String> allTables = new ArrayList<String>();

    public static final String TABLE_USER = "${entity.table.user}";
    public static final String TABLE_GROUP = "${entity.table.group}";
    public static final String TABLE_ROLE = "${entity.table.role}";
    public static final String TABLE_USER_GROUP_ROLE = "${entity.table.userGroupRole}";
    public static final String TABLE_ACL = "${entity.table.acl}";
    public static final String TABLE_PRIVILEGE = "${entity.table.privilege}";

    public static Map<String, Object> getThreadLocalMap() {
        return threadLocal.get();
    }

    private final static InheritableThreadLocal<Map<String, Object>> threadLocal = new InheritableThreadLocal<Map<String, Object>>();
    static {
        threadLocal.set(new HashMap<String, Object>());
    }

    @Value(TABLE_USER)
    private String userTablename;

    @Value(TABLE_GROUP)
    private String groupTablename;

    @Value(TABLE_USER_GROUP_ROLE)
    private String userGroupRoleTablename;

    @Value(TABLE_ROLE)
    private String roleTablename;

    @Value(TABLE_ACL)
    private String aclTablename;

    @Value(TABLE_PRIVILEGE)
    private String privilegeTablename;

    @Value("${entity.queryable.configFile:db-config.xml}")
    private String configFile;

    @Value("${entity.security.encrypt.MD5.fields:}")
    private String md5Fields;

    @Value("${entity.security.encrypt.MD5.publicKey:ccait}")
    private String md5PublicKey;

    @Value("${entity.security.encrypt.BASE64.fields:}")
    private String base64Fields;

    @Value("${entity.security.encrypt.MAC.fields:}")
    private String macFields;

    @Value("${entity.security.encrypt.SHA.fields:}")
    private String shaFields;

    @Value("${entity.security.encrypt.MAC.publicKey:ccait}")
    private String macPublicKey;

    @Value("${entity.security.encrypt.AES.fields:}")
    private String aesFields;

    @Value("${entity.security.encrypt.AES.publicKey:ccait}")
    private String aesPublicKey;

    @Value("${entity.security.admin.username:admin}")
    private String admin;

    @Value("${entity.security.admin.password:}")
    private String password;

    @Value("${entity.encoding:UTF-8}")
    private String encoding;


    @Value("${entity.table.reservedField.createOn:createTime}")
    private String createOnField;

    @Value("${entity.table.reservedField.modifyOn:modifyTime}")
    private String modifyOnField;

    @Value("${entity.table.reservedField.modifyBy:modifyBy}")
    private String modifyByField;

    @Value("${entity.table.reservedField.userPath:userPath}")
    private String userPathField;

    @Value("${entity.table.reservedField.groupId:groupId}")
    private String groupIdField;

    @Value("${entity.table.reservedField.userId:userId}")
    private String userIdField;

    @Value("${entity.table.reservedField.roleId:roleId}")
    private String roleIdField;

    @Value("${entity.table.reservedField.createBy:createBy}")
    private String createByField;



    /**
     * 通过名称获取bean
     */
    public static Object getBean(String name) {
        return instance.getBean(name);
    }


    /**
     * 通过类型获取bean
     */
    public static Object getBean(Class<?> clazz) {
        return instance.getBean(clazz);
    }

    /**
     * 判断某个bean是不是存在
     */
    public static boolean hasBean(String name) {
        return instance.containsBean(name);
    }

    @PostConstruct
    private void construct() {
        admin = ApplicationConfig.getInstance().get("${entity.security.admin.username}", admin);
        password = ApplicationConfig.getInstance().get("${entity.security.admin.password}", password);
        configFile = ApplicationConfig.getInstance().get("${entity.queryable.configFile}", configFile);
        md5Fields = ApplicationConfig.getInstance().get("${entity.security.encrypt.MD5.fields}", md5Fields);

        md5PublicKey = ApplicationConfig.getInstance().get("${entity.security.encrypt.MD5.publicKey}", md5PublicKey);
        base64Fields = ApplicationConfig.getInstance().get("${entity.security.encrypt.BASE64.fields}", base64Fields);

        macFields = ApplicationConfig.getInstance().get("${entity.security.encrypt.MAC.fields}", macFields);
        shaFields = ApplicationConfig.getInstance().get("${entity.security.encrypt.SHA.fields}", shaFields);
        macPublicKey = ApplicationConfig.getInstance().get("${entity.security.encrypt.MAC.publicKey}", macPublicKey);
        aesFields = ApplicationConfig.getInstance().get("${entity.security.encrypt.AES.fields}", aesFields);
        aesPublicKey = ApplicationConfig.getInstance().get("${entity.security.encrypt.AES.publicKey}", aesPublicKey);
        encoding = ApplicationConfig.getInstance().get("${entity.encoding}", encoding);
        createOnField = ApplicationConfig.getInstance().get("${entity.table.reservedField.createOn}", createOnField);
        modifyOnField = ApplicationConfig.getInstance().get("${entity.table.reservedField.modifyOn}", modifyOnField);
        userPathField = ApplicationConfig.getInstance().get("${entity.table.reservedField.userPath}", userPathField);
        groupIdField = ApplicationConfig.getInstance().get("${entity.table.reservedField.groupId}", groupIdField);
        roleIdField = ApplicationConfig.getInstance().get("${entity.table.reservedField.roleId}", roleIdField);
        createByField = ApplicationConfig.getInstance().get("${entity.table.reservedField.createBy}", createByField);
    }

    /**
     * 实现该接口用来初始化应用程序上下文
     * 该接口会在执行完毕@PostConstruct的方法后被执行
     * 接着，会进行Mapper地址扫描并加载，就是RequestMapping中指定的那个路径
     *
     * @param applicationContext 应用程序上下文
     * @throws BeansException beans异常
     */
    @Override
    public void setApplicationContext(org.springframework.context.ApplicationContext applicationContext) throws BeansException {
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 ： [{}]", "开始初始化");
        this.instance = applicationContext;


        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getId ： [{}]", applicationContext.getId());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getApplicationName ： [{}]", applicationContext.getApplicationName());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getAutowireCapableBeanFactory ： [{}]", applicationContext.getAutowireCapableBeanFactory());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getDisplayName ： [{}]", applicationContext.getDisplayName());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getParent ： [{}]", applicationContext.getParent());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getStartupDate ： [{}]", applicationContext.getStartupDate());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getEnvironment ： [{}]",applicationContext.getEnvironment());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 ： [ApplicationContext]", "初始化完成");

        initDatasource(applicationContext);
    }

    public void initDatasource(org.springframework.context.ApplicationContext applicationContext) {
        try {
            Collection<DataSource> dsList = DataSourceFactory.getInstance().getAllDataSource();
            if(dsList == null || dsList.size() < 1) {
                throw new Exception(String.format("无法读取数据源配置文件(Can not read dataSource file %s)", configFile));
            }

            Optional<DataSource> opt = null;
            if(dsList.size() == 1) {
                opt = dsList.stream().findFirst();
            }

            else {
                opt = dsList.stream().filter(a->a.isDefault()).findFirst();
            }

            if(!opt.isPresent()) {
                throw new Exception(String.format("没有找到数据源(Can not find dataSource in %s)", configFile));
            }

            DataSource ds = opt.get();

            if(StringUtils.isEmpty(userTablename)) {
                throw new Exception("请设置用户表名称(Setting ${entity.table.user} in application.yml pls)");
            }

            if(!"n1ql_jdbc".equals(ds.getDriverClassName())) {

                allTables = Queryable.getTables(ds.getId());

                List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
                ColumnInfo col = null;

                createUserTable(ds, columns);
                createGroupTable(ds, columns);
                createRoleTable(ds, columns);
                createAclTable(ds, columns);
                createPrivilegeTable(ds, columns);
                createUserGroupRoleTable(ds, columns);
            }

            String pwd = getEncryptPassword();
            UserModel user = new UserModel();
            user.setCreateBy(Long.valueOf(0));
            user.setStatus(0);
            user.setCreateOn(Datetime.getTime());
            user.setUsername(admin);
            user.setPassword(pwd);
            user.setPath("0");
            UserModel admin = user.where("[username]=#{username}").first();
            if(null == admin) {
                user.insert();
            }

            else if(!pwd.equals(admin.getPassword())) {
                admin.where("[username]=#{username}").update(String.format("[password]='%s'", pwd));
            }

        } catch (Exception e) {
            log.error(LOG_PRE_SUFFIX + "数据源初始化：[ApplicationContext]" + e.getMessage(), e);
            if (applicationContext instanceof ConfigurableApplicationContext) {
                ((ConfigurableApplicationContext) applicationContext).close();
            }
        }
    }

    private void createUserGroupRoleTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        if(allTables.stream().filter(a->a.toLowerCase().equals(groupTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", groupTablename));
            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName("userGroupRoleId");
        col.setPrimaryKey(true);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("userId");
        col.setCanNotNull(true);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(groupIdField);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(roleIdField);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(userPathField);
        col.setCanNotNull(true);
        col.setMaxLength(2048);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createByField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        Queryable.createTable(ds.getId(), userGroupRoleTablename, columns);
        log.info(String.format(LOG_PRE_SUFFIX + "用户群组角色表[%s]创建成功！", groupTablename));
    }

    private void createPrivilegeTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        if(allTables.stream().filter(a->a.toLowerCase().equals(privilegeTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", privilegeTablename));
            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName("privilegeId");
        col.setPrimaryKey(true);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(groupIdField);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(roleIdField);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("aclId");
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canAdd");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canDelete");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canUpdate");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canView");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canDownload");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canPreviewDoc");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canUpload");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canExport");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canImport");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canDecrypt");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canList");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canQuery");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("scope");
        col.setCanNotNull(true);
        col.setMaxLength(16);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(userPathField);
        col.setCanNotNull(true);
        col.setMaxLength(2048);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createByField);
        col.setCanNotNull(true);
        col.setType(Long.class);
        columns.add(col);

        Queryable.createTable(ds.getId(), privilegeTablename, columns);
        log.info(String.format(LOG_PRE_SUFFIX + "权限表[%s]创建成功！", privilegeTablename));
    }

    private void createAclTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        if(allTables.stream().filter(a->a.toLowerCase().equals(aclTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", aclTablename));
            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName("aclId");
        col.setPrimaryKey(true);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(groupIdField);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("tableName");
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);
        col = new ColumnInfo();

        col.setColumnName(userPathField);
        col.setCanNotNull(true);
        col.setMaxLength(2048);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createByField);
        col.setCanNotNull(true);
        col.setType(Long.class);
        columns.add(col);

        Queryable.createTable(ds.getId(), aclTablename, columns);
        log.info(String.format(LOG_PRE_SUFFIX + "访问控制表[%s]创建成功！", aclTablename));
    }

    private void createRoleTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        if(allTables.stream().filter(a->a.toLowerCase().equals(roleTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", roleTablename));
            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName(roleIdField);
        col.setPrimaryKey(true);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("roleName");
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("description");
        col.setMaxLength(255);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(userPathField);
        col.setCanNotNull(true);
        col.setMaxLength(2048);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createByField);
        col.setCanNotNull(true);
        col.setType(Long.class);
        columns.add(col);

        Queryable.createTable(ds.getId(), roleTablename, columns);
        log.info(String.format(LOG_PRE_SUFFIX + "角色表[%s]创建成功！", roleTablename));
    }

    private void createGroupTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        if(allTables.stream().filter(a->a.toLowerCase().equals(groupTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", groupTablename));
            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName(groupIdField);
        col.setPrimaryKey(true);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("groupName");
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("description");
        col.setMaxLength(255);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(userPathField);
        col.setCanNotNull(true);
        col.setMaxLength(2048);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createByField);
        col.setCanNotNull(true);
        col.setType(Long.class);
        columns.add(col);

        Queryable.createTable(ds.getId(), groupTablename, columns);
        log.info(String.format(LOG_PRE_SUFFIX + "群组表[%s]创建成功！", groupTablename));
    }

    public void createUserTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        UserModel user = new UserModel();
        user.setUsername(admin);
        String pwd = getEncryptPassword();

        user.setPassword(pwd);
        user.setPath("0");
        user.setCreateOn(new Date());
        user.setStatus(0);
        user.setCreateBy(Long.valueOf(0));

        if(allTables.stream().filter(a->a.toLowerCase().equals(userTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", userTablename));

            if(StringUtils.isNotEmpty(password)) {
                user.where("[username]=#{username}").update("[password]=#{password}"); //确保超级管理员密码
            }

            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName(userIdField);
        col.setIsAutoIncrement(true);
        col.setCanNotNull(true);
        col.setMaxLength(16);
        col.setType(Long.class);
        col.setPrimaryKey(true);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("username");
        col.setCanNotNull(true);
        col.setMaxLength(16);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("password");
        col.setCanNotNull(true);
        col.setMaxLength(255);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(userPathField);
        col.setCanNotNull(true);
        col.setMaxLength(2048);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createByField);
        col.setCanNotNull(true);
        col.setType(Long.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("status");
        col.setType(Integer.class);
        columns.add(col);

        Queryable.createTable(ds.getId(), userTablename, columns);

        user.insert();

        log.info(String.format(LOG_PRE_SUFFIX + "用户表[%s]创建成功！", userTablename));
    }

    public String getEncryptPassword() throws Exception {
        String pwd = password;

        if(StringUtils.isNotEmpty(md5Fields)) {
            List<String> fieldList = StringUtils.splitString2List(md5Fields, ",");
            if(fieldList.contains("password") || fieldList.contains("user.password")) {
                pwd = EncryptionUtil.md5(pwd, md5PublicKey, encoding);
            }
        }

        if(StringUtils.isNotEmpty(macFields)) {
            List<String> fieldList = StringUtils.splitString2List(macFields, ",");
            if(fieldList.contains("password") || fieldList.contains("user.password")) {
                pwd = EncryptionUtil.mac(pwd.getBytes(encoding), macPublicKey);
            }
        }

        if(StringUtils.isNotEmpty(shaFields)) {
            List<String> fieldList = StringUtils.splitString2List(shaFields, ",");
            if(fieldList.contains("password") || fieldList.contains("user.password")) {
                pwd = EncryptionUtil.sha(pwd);
            }
        }

        if(StringUtils.isNotEmpty(base64Fields)) {
            List<String> fieldList = StringUtils.splitString2List(base64Fields, ",");
            if(fieldList.contains("password") || fieldList.contains("user.password")) {
                pwd = EncryptionUtil.base64Encode(pwd, encoding);
            }
        }

        if(StringUtils.isNotEmpty(aesFields)) {
            List<String> fieldList = StringUtils.splitString2List(aesFields, ",");
            if(fieldList.contains("password")) {
                pwd = EncryptionUtil.encryptByAES(pwd, encoding);
            }
        }
        return pwd;
    }
}
