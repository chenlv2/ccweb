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

import ccait.ccweb.dynamic.DynamicClassBuilder;
import ccait.ccweb.model.PrimaryKeyInfo;
import ccait.ccweb.model.QueryInfo;
import entity.query.ColumnInfo;
import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.annotation.Tablename;
import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Order;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;

@Order(-55555)
public final class EntityContext {

    @Value(value = "${entity.suffix:Entity}")
    private String suffix;

    private static final Logger log = LogManager.getLogger( EntityContext.class );

    private final Map<String, String> tableMap = new HashMap<String, String>();

    private static final Map<Class<?>, List<Field>> objectFieldsMap = new HashMap<Class<?>, List<Field>>() ;

    @PostConstruct
    private void postConstruct() {
        org.springframework.context.ApplicationContext app = ApplicationContext.getInstance();
        String[] names = app.getBeanNamesForAnnotation(Tablename.class);
        if(names != null) {
            for(String name : names) {

                String key = name;
                if(StringUtils.isNotEmpty(suffix) && name.length() > suffix.length()) {
                    key = name.substring(0, name.length() - suffix.length());
                }

                tableMap.put(key.toLowerCase(), name);
            }
        }

        log.info(LOG_PRE_SUFFIX + "实体类上下文 ： [EntityContext]", "初始化完成");
    }

    public static Object getEntity(String tablename, Map<String, Object> data) {

        Object bean = getEntity(tablename);
        if(bean == null) {
            bean = DynamicClassBuilder.create(tablename, data);
        }

        return bean;
    }

    public static Object getEntity(String tablename, QueryInfo queryInfo) {

        Object bean = getEntity(tablename);
        if(bean == null) {
            bean = DynamicClassBuilder.create(tablename, queryInfo);
        }

        return bean;
    }

    public static Object getEntityId(String tablename, String id) {
        Object bean = getEntity(tablename);
        if(bean == null) {
            List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

            if(Pattern.matches("^[0-9]{1,32}$", id)) {
                columns.add(new ColumnInfo("id", "integer", true));
            }

            else if(Pattern.matches("^\\d+$", id)) {
                columns.add(new ColumnInfo("id", "long", true));
            }

            else {
                columns.add(new ColumnInfo("id", "text", true));
            }

            bean = DynamicClassBuilder.create(tablename, columns);
        }

        return bean;
    }

    private static Object getEntity(String tablename) {
        org.springframework.context.ApplicationContext app = ApplicationContext.getInstance();
        EntityContext entityContext = (EntityContext) app.getAutowireCapableBeanFactory().getBean("entityContext");

        Object bean = null;
        if(entityContext.tableMap.containsKey(tablename)) {
            String entityName = entityContext.tableMap.get(tablename);

            bean = app.getAutowireCapableBeanFactory().getBean(entityName);
        }

        return bean;
    }

    public static PrimaryKeyInfo getPrimaryKeyInfo(Object instance) {

        List<Field> fields = getFields(instance);
        for(Field fld : fields) {
            PrimaryKey ann = fld.getAnnotation(PrimaryKey.class);
            if(ann == null) {
                continue;
            }

            PrimaryKeyInfo result = new PrimaryKeyInfo();
            result.setPrimaryKey(ann);
            result.setField(fld);

            return result;
        }

        return null;
    }

    public static List<Field> getFields(Object instance) {

        Class<?> cls = instance.getClass();
        if(objectFieldsMap.containsKey(cls)) {
            return objectFieldsMap.get(cls);
        }

        List<Field> result = new ArrayList<Field>();
        Field[] fields = cls.getDeclaredFields();

        if(fields == null) {
            return result;
        }

        for(Field fld : fields) {
            result.add(fld);
        }

        if(result.size() > 0) {
            synchronized (objectFieldsMap) {
                objectFieldsMap.put(cls, result);
            }
        }

        return result;
    }

    public static String getColumnName(Field fld) {
        Fieldname ann = fld.getAnnotation(Fieldname.class);
        if(ann == null) {
            return fld.getName();
        }

        return ApplicationConfig.getInstance().get(ann.value());
    }
}
