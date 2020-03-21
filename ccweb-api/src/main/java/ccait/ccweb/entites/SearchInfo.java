/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.entites;


import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.enums.Algorithm;
import ccait.ccweb.enums.PrivilegeScope;
import ccait.ccweb.model.PageInfo;
import ccait.ccweb.model.UserModel;
import com.alibaba.fastjson.JSONArray;
import entity.query.*;
import entity.query.core.DataSource;
import entity.query.core.DataSourceFactory;
import entity.query.enums.Function;
import entity.tool.util.DBUtils;
import entity.tool.util.ReflectionUtils;
import entity.tool.util.StringUtils;
import org.apache.http.HttpException;
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

import static ccait.ccweb.dynamic.DynamicClassBuilder.ensureColumnName;
import static ccait.ccweb.dynamic.DynamicClassBuilder.smallHump;
import static ccait.ccweb.utils.StaticVars.*;
import static entity.tool.util.StringUtils.cast;
import static entity.tool.util.StringUtils.join;

@Component
public class SearchInfo extends QueryInfo {


    private static SearchInfo context;
    @PostConstruct
    public void init() {
        context = this;
        context.request = this.request;
        // 初使化时将已静态化的request实例化
    }

    private String fulltextSearch;
    private List<String> fulltextSearchFields;
}
