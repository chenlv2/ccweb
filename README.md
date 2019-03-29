
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

CCWEB是基于springboot设计的CURD敏捷web api开发框架，CCWEB提倡动态向前端提供基础数据，由前端根据基础数据组装业务来提高开发效率;内置用户管理、权限设置 等安全模块，启动服务后无需添加任何后端代码前端便可以通过默认接口直接访问到自己在数据库建的表和查询视图；底层orm采用entityQueryable访问数据，支持多种数据库，支持SpringCloud微服务扩展；项目包含ccweb-core，ccweb-api，ccweb-admin，ccweb-start
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
* ccweb-start-1.0.0-SNAPSHOT.jar 【ccweb默认服务启动包 [下载jar包](https://github.com/linlurui/ccweb/blob/master/ccweb-start/target/ccweb-start-1.0.0-SNAPSHOT.jar)】
* application.yml 【应用程序主配置文件 [查看](https://github.com/linlurui/ccweb/blob/master/ccweb-start/src/main/resources/application.yml)】
* db-config.xml 【数据库连接配置文件 [查看](https://github.com/linlurui/ccweb/blob/master/ccweb-start/src/main/resources/db-config.xml)】
* install.sh【linux系统依赖包安装脚本，windows下需要安装cygwin来运行该脚本 [查看](https://github.com/linlurui/ccweb/blob/master/ccweb-start/src/main/resources/log4j.properties)】
* log4j2.xml 【可选，log4j2日志配置文件，记录ccweb服务异常信息 [查看](https://github.com/linlurui/ccweb/blob/master/ccweb-start/src/main/resources/log4j2.xml)】
* log4j.properties 【可选，log4j日志配置文件，记录ORM异常信息 [查看](https://github.com/linlurui/ccweb/blob/master/ccweb-start/src/main/resources/log4j.properties)】
## 服务启动命令
***java -jar ccweb-start-1.0.0-SNAPSHOT.jar***

## 接口说明
## 系统表结构说明
# ccweb-admin
ccweb-admin是为超级管理员在设计阶段准备的数据管理界面，包含用户管理、用户组管理、权限管理、表结构与视图管理 [下载jar包](https://github.com/linlurui/ccweb/blob/master/ccweb-start/target/ccweb-start-1.0.0-SNAPSHOT.jar)

## 启动命令
## 访问地址

# 二次开发
ccweb的二次开发实际就是自定义ccweb-start包的过程，只需RESTful则引入ccweb-api，需要视图模板则引入ccweb-mvc
## jar包介绍
#### ccweb-core: ccweb的核心公共库

#### ccweb-api: 提供RESTful接口服务和websocket服务，内置ccweb-core，不能直接起动，需要在ccweb-start中提供入口启动jar包。

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
## 自定义控制器
## BaseContoller
## 事件触发器Tagger
