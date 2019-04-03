
                      c
                     c#
                  /c++)
                  python
                  #VB^
                 ccait'
                 java   ccait    ccweb  En       ti #SPRING  Babel^_^~
                node   CC" '''  CC" ''' ty  Que  ry MVC      TS   `VUE
              delphi  CCC      CCC      ab  /le go  Docker   Electron@,
        javascript,   CC       CC       pg/   pay   AOP      ES      66,
     2019@copyright     v1.0.0   HTTP:  //    ///   CCAIT.CN FREAMEWORK

    =========================================================================
    :: CCWEB :: (v1.0.0-SNAPSHOT)  Author: 草耑(linlurui) 2019@copyright

CCWEB是基于springboot设计的CQRS敏捷web api开发框架，CCWEB提倡动态向前端提供基础数据，由前端根据基础数据组装业务来提高开发效率;内置用户管理、权限设置 等安全模块，启动服务后无需添加任何后端代码前端便可以通过默认接口直接访问到自己在数据库建的表和查询视图；底层orm采用entityQueryable访问数据，支持多种数据库，支持SpringCloud微服务扩展；项目包含ccweb-core，ccweb-api，ccweb-admin，ccweb-start
</p>
    <img align="right" src="https://github.com/linlurui/entityQueryable/blob/master/pay5.jpg" alt="捐赠给作者"  width="200">
    <p align="right">
        <em>捐赠给作者</em>
    </p>
</p>

# ccweb-start
ccweb-start是ccweb-api的启动包，其中包含了springcloud的微服务组件与springboos2.0

## 运行环境
* jdk1.8

## 文件结构
* ccweb-start-1.0.0-SNAPSHOT.jar 【ccweb默认服务启动包 [下载](https://github.com/linlurui/ccweb/raw/master/release/ccweb-start-1.0.0-SNAPSHOT.jar)】
* application.yml 【应用程序主配置文件 [详情](https://github.com/linlurui/ccweb/blob/master/release/application.yml)】
* db-config.xml 【数据库连接配置文件 [详情](https://github.com/linlurui/ccweb/blob/master/release/db-config.xml)】
* entity.queryable-2.0-SNAPSHOT.jar【动态查询依赖包 [下载](https://github.com/linlurui/ccweb/raw/master/release/entity.queryable-2.0-SNAPSHOT.jar)】
* rxjava-2.1.10.jar【查询结果异步IO依赖包 [下载](https://github.com/linlurui/ccweb/raw/master/release/rxjava-2.1.10.jar)】
* spring-context-5.0.4.RELEASE.jar【动态实体注入依赖包 [下载](https://github.com/linlurui/ccweb/raw/master/release/spring-context-5.0.4.RELEASE.jar)】
* install.sh【linux系统依赖包安装脚本，需要先安装JDK1.8并且使用JDK自带的JRE，windows下需要安装cygwin来运行该脚本 [详情](https://github.com/linlurui/ccweb/blob/master/release/install.sh)】
* log4j2.xml 【可选，log4j2日志配置文件，记录ccweb服务异常信息 [详情](https://github.com/linlurui/ccweb/blob/master/release/log4j2.xml)】

## 服务启动命令
***java -jar ccweb-start-1.0.0-SNAPSHOT.jar***

## 接口说明
ccweb-start内置了默认的api接口可以让前端直接通过表名操作数据，需要限制访问的可以设置系统默认创建的用户权限表进行控制。
### 1. 新增
* URL：/api/{table} 
* 请求方式：PUT
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
{
  "字段名": "值",
  ...
}
```

### 2. 删除
* URL：/api/{table}/{id} 
* 请求方式：DELETE
* URL参数：{table}为数据库表名称，{id}为主键
* POST参数：无

### 3. 修改
* URL：/api/{table}/{id} 
* 请求方式：PUT
* URL参数：{table}为数据库表名称，{id}为主键
* POST参数：
```javascript
{
  "字段名": "值", 
  ...
}
```

### 4. 查询
* URL：/api/{table} 
* 请求方式：POST
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
{
    "pageInfo" : {
        "pageIndex": 1, //页码
        "pageSize": 50  //每页条数
    },

    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2, "="), GT(3, ">"), LT(4, "<"), GTEQ(5, ">="), LTEQ(6, "<="), NOT(7, "<>"), NOTEQ(8, "!=")
    }, ... ],

    "sortList": [{ //排序条件
        "name": "id", //字段名 
        "desc": true  //true为降序，false为升序
    }, ... ],

    "groupList" : [ //分组条件
        "id", //字段名 
        ...
    ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值
    }, ...],

    "selectList": [{ //显示字段
        "field": "id",  //字段名 
        "function": "MAX",  //数据库相关函数：MAX, MIN, UPPER, LOWER, LENGTH, AVG, COUNT, SUM, GROUP_CONCAT等; 
    }, ... ]
}
```

### 5. 联表查询
* URL：/api/join 
* 请求方式：POST
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
{
    "joinTables": [{
        "tablename": "user", //表名
        "alias": "a", //别名, 可选
        "fields": [{ //查询字段
            "name": "id"
        }, ... ], 
        "JoinMode": "Inner", //联表方式：Inner, Left, Right, Outer, Cross
        "onList": [{ //联表条件
            "name": "id",   //字段名
            "value": "1",   //值
            "algorithm": "EQ",   //条件: EQ(2, "="), GT(3, ">"), LT(4, "<"), GTEQ(5, ">="), LTEQ(6, "<="), NOT(7, "<>"), NOTEQ(8, "!=")
        }, ... ],
    }, ... ]
    "pageInfo" : {
        "pageIndex": 1, //页码
        "pageSize": 50  //每页条数
    },

    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2, "="), GT(3, ">"), LT(4, "<"), GTEQ(5, ">="), LTEQ(6, "<="), NOT(7, "<>"), NOTEQ(8, "!=")
    }, ... ],

    "sortList": [{ //排序条件
        "name": "id", //字段名 
        "desc": true  //true为降序，false为升序
    }, ... ],

    "groupList" : [ //分组条件
        "id", //字段名 
        ...
    ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值
    }, ...],

    "selectList": [{ //显示字段
        "field": "id",  //字段名 
        "function": "MAX",  //数据库相关函数：MAX, MIN, UPPER, LOWER, LENGTH, AVG, COUNT, SUM, GROUP_CONCAT等; 
    }, ... ]
}
```

### 6. ID查询
查询与联合查询加密的字段不会解密显示，多用于列表，而ID查询的结果可以显示解密后内容，可用于保密详情。
* URL：/api/{table}/{id} 
* 请求方式：GET
* URL参数：{table}为数据库表名称，{id}为主键
* POST参数：无

### 7. 登录
* URL：/api/login 
* 请求方式：POST
* POST参数：
```javascript
{
  "username": "用户名",
  "password": "密码",
}
```

### 8. 登出
* URL：/api/login 
* 请求方式：GET

## 系统用户/权限表结构说明
用户权限相关表在服务启动时会自动创建，目的在于使用系统服务控制数据库表的访问权限，用户组是扁平结构的，需要更复杂的权限控制功能建议通过二次开发实现。
* 用户表 (user, 主键id)
* 用户组 (group, 主键groupId)
* 角色表 (role, 主键roleId)
* 用户/组/角色关联关系表 (userGroupRole, 主键userGroupRoleId, 外键关联userId、groupId、roleId)
* 数据访问控制表 (acl, 主键aclId, 外键关联groupId)
* 操作权限表 (privilege, 主键privilegeId, 外键关联groupId、roleId、aclId)

# ccweb-admin
ccweb-admin是为超级管理员在设计阶段准备的数据管理界面，包含用户管理、用户组管理、权限管理、表结构与视图管理 [下载](https://github.com/linlurui/ccweb/blob/master/release/ccweb-admin-1.0.0-SNAPSHOT.jar)

## 启动命令
## 访问地址

# 二次开发
ccweb的二次开发实际就是自定义ccweb-start包的过程，只需RESTful则引入ccweb-api，需要视图模板则引入ccweb-mvc
## jar包介绍
* ccweb-core: ccweb的核心公共库
* ccweb-api: 提供RESTful接口服务和websocket服务，内置ccweb-core，不能直接起动，需要在ccweb-start中提供入口启动jar包。

## Maven仓库中引入jar包
```xml
    <repositories>
        <repository>
            <id>ccweb</id>
            <url>https://raw.github.com/linlurui/ccweb/snapshots</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>ccait</groupId>
            <artifactId>ccweb-api</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
```

## Ccweb启动方法
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        CcwebAppliction.run(Application.class, args);
    }
｝
```
## 生成实体类
* ccweb虽然支持通过请求动态生成数据查询实体类，但推荐在二次开发的时候通过实体生成器生成数据查询的实体以提高访问的性能，实体生在器在ccweb-core包里，包路径为package ccait.generator，启动类EntitesGenerator，生成的路径与包名可在application.yml中设置。

## 编写控制器
```java
@RestController
public class ApiController extends BaseController {

    @ResponseBody
    @RequestMapping( value = "login", method = RequestMethod.POST )
    public Mono loginByPassword(@RequestBody UserModel user) {
        try {

            user = super.logoin(user);

            return successAs(user);

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(150, e);
        }
    }

}
```

## BaseContoller
BaseContoller规范了ResponseData返回数据的格式，并为用户封装了后端http请求数据获取校验等方法提供给自定义的rest控制器继承使用。

* getLoginUser()【获取当前登录用户】
* getCurrentMaxPrivilegeScope(table)【获取当前用户对表的操作权限】
* getTablename()【获取当前访问的表名】
* md5(text)text【md5加密】
* encrypt(data)【加密数据或查询条件字段值】
* decrypt(data)【解密数据或查询条件字段值】
* base64Encode(text)【base64编码】
* base64Decode(text)【base64解码】
* checkDataPrivilege(table, data)【检查当前用户对数据的访问权限】
* success(data)【成功返回方法】
* error(data)【错误返回方法】
* successAs(data)【异步IO成功返回方法】
* errorAs(data)【异步IO错误返回方法】
* ResponseData【数据响应封装类】

## 事件触发器Tagger
为了方便二次开发可以拦截及响应请求，框架提供了触发器能力，可以针对不同请求事件嵌入自定义的逻辑，示例如下：

```java
@Component
@Scope("prototype")
@Trigger //触发器注解
public final class DefaultTrigger {

    /***
     * 新增数据事件
     * @param data （提交的数据）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnInsert
    public void onInsert(Map<String, Object> data, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 更新数据事件
     * @param data （提交的数据）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnUpdate
    public void onUpdate(Map<String, Object> data, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 删除数据事件
     * @param id （要删除的数据ID）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnDelete
    @Order(-55555)
    void onDelete(String id, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 建表事件
     * @param columns （字段内容列表）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnBuildTable
    public void onBuild(List<ColumnInfo> columns, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 列出数据事件，当queryInfo没有查询条件时触发
     * @param queryInfo （分页/分组/排序条件）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnList
    public void onList(QueryInfo queryInfo, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 查询数据事件，queryInfo存在查询条件时触发
     * @param queryInfo （查询/分页/分组/排序条件）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnQuery
    public void onQuery(QueryInfo queryInfo, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 浏览数据事件，ID查询时触发
     * @param id （要浏览的数据ID）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnView
    public void onView(String id, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 成功返回数据时触发
     * @param responseData （响应的数据）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnSuccess
    public void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 返回错误数据时触发
     * @param ex （Exception异常类）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnError
    public void onError(Exception ex, HttpServletRequest request) {
        //TODO
    }

    /***
     * 响应数据流时触发
     * @param response （响应对象）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnResponse
    void onResponse(HttpServletResponse response, HttpServletRequest request) throws Exception {
        //TODO
    }

}
```

## 数据响应说明
### 1. ResponseData
```java
    private int code; //0=成功
    private String message; //code不等于零时返回错误消息
    private T data; //code等于0返回查询的结果
    private PageInfo pageInfo; //分页信息
    private UUID uuid; //该次请求唯一识别码
```
### 2. PageInfo
```java
    private int pageCount; //总页数
    private int pageIndex; //当前页
    private int pageSize;  //每页显示记录数
    private long totalRecords; //总记录数
```

## 打包说明
项目部署时如果要使用动态查询功能，打包时建议打成war包部署在tomcat，jar包目前只有在windows下支持使用动态查询功能，在linul环境下由于目录权限问题可能会加载不到依赖包。
