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

import ccait.ccweb.annotation.*;
import ccait.ccweb.enums.EventType;
import ccait.ccweb.model.EventInfo;
import ccait.ccweb.model.QueryInfo;
import ccait.ccweb.trigger.ITrigger;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Order;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static ccait.ccweb.context.ApplicationContext.*;
import static ccait.ccweb.context.ApplicationContext.TABLE_ACL;
import static ccait.ccweb.context.ApplicationContext.TABLE_PRIVILEGE;
import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;

@Order(-55555)
public final class TriggerContext {

    private static final Logger log = LogManager.getLogger( TriggerContext.class );

    private static final String DEFAULT_ALL_TABLE_EVENT = "CCAIT_WEB_DEFAULT_ALL_TABLE_EVENT";

    @Value(TABLE_USER)
    private String userTablename;

    @Value(TABLE_GROUP)
    private String groupTablename;

    @Value(TABLE_ROLE)
    private String roleTablename;

    @Value(TABLE_ACL)
    private String aclTablename;

    @Value(TABLE_PRIVILEGE)
    private String privilegeTablename;

    private final static List<EventInfo> eventList = new ArrayList<EventInfo>();

    @PostConstruct
    private void postConstruct() {

        org.springframework.context.ApplicationContext app = ApplicationContext.getInstance();
        Map<String, Object> map = app.getBeansWithAnnotation(Trigger.class);

        for (String key : map.keySet()) {

            Object trigger = map.get(key);

            Trigger ann = trigger.getClass().getAnnotation(Trigger.class);
            String tablename = getTablename(ann);

            if(StringUtils.isEmpty(tablename)) {
                tablename = DEFAULT_ALL_TABLE_EVENT;
            }

            Class<?> clazz = trigger.getClass();

            EventInfo eventInfo = new EventInfo();
            eventInfo.setType(clazz);
            eventInfo.setTablename(tablename);

            Order order = clazz.getAnnotation(Order.class);
            if(order != null) {
                eventInfo.setOrder(order.value());
            }

            if(ITrigger.class.isAssignableFrom(clazz)) {
                clazz = ITrigger.class;
            }

            Method[] methods = clazz.getMethods();
            for(Method m : methods) {

                Method method;
                try {
                    method = trigger.getClass().getMethod(m.getName(), m.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    log.error(LOG_PRE_SUFFIX + e);
                    continue;
                }

                if(method == null) {
                    continue;
                }

                if(m.getAnnotation(OnInsert.class) != null) {
                    eventInfo.getOnInsertMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnUpdate.class) != null) {
                    eventInfo.getOnUpdateMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnDelete.class) != null) {
                    eventInfo.getOnDeleteMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnList.class) != null) {
                    eventInfo.getOnListMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnView.class) != null) {
                    eventInfo.getOnViewMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnQuery.class) != null) {
                    eventInfo.getOnQueryMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnResponse.class) != null) {
                    eventInfo.getOnResponseMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnSuccess.class) != null) {
                    eventInfo.getOnSuccessMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnError.class) != null) {
                    eventInfo.getOnErrorMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnBuildTable.class) != null) {
                    eventInfo.getOnBuildMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnDownload.class) != null) {
                    eventInfo.getOnDownloadMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnPreviewDoc.class) != null) {
                    eventInfo.getOnPreviewDocMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnPlayVideo.class) != null) {
                    eventInfo.getOnPlayVideoMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnUpload.class) != null) {
                    eventInfo.getOnUploadMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnExport.class) != null) {
                    eventInfo.getOnExportMethodSet().add(method);
                    continue;
                }

                if(m.getAnnotation(OnImport.class) != null) {
                    eventInfo.getOnImportMethodSet().add(method);
                    continue;
                }
            }

            eventList.add(eventInfo);
        }
    }

    public static <T> void exec(String tablename, EventType eventType, T params, HttpServletRequest request) throws InvocationTargetException, IllegalAccessException {

        if(StringUtils.isEmpty(tablename)) {
            return;
        }

        List<EventInfo> list = eventList.stream()
                .filter(a -> a.getTablename().equals(tablename) || a.getTablename().equals(DEFAULT_ALL_TABLE_EVENT))
                .collect(Collectors.toList());

        for(EventInfo eventInfo : list) {
            Class<?> type = eventInfo.getType();
            String beanName = type.getSimpleName().substring(0, 1).toLowerCase() + type.getSimpleName().substring(1);
            Object obj = ApplicationContext.getInstance().getBean(beanName);

            switch (eventType) {
                case Insert:
                    if(params instanceof Map) {
                        List<Map<String, Object>> paramsList = new ArrayList<Map<String, Object>>();
                        paramsList.add((Map<String, Object>) params);
                        invoke(eventInfo.getOnInsertMethodSet(), obj, paramsList, request);
                        break;
                    }
                    invoke(eventInfo.getOnInsertMethodSet(), obj, params, request);
                    break;
                case Update:
                    invoke(eventInfo.getOnUpdateMethodSet(), obj, params, request);
                    break;
                case Delete:
                    invoke(eventInfo.getOnDeleteMethodSet(), obj, params, request);
                    break;
                case List:
                    invoke(eventInfo.getOnListMethodSet(), obj, params, request);
                    break;
                case View:
                    invoke(eventInfo.getOnViewMethodSet(), obj, params, request);
                    break;
                case Query:
                    invoke(eventInfo.getOnQueryMethodSet(), obj, params, request);
                    break;
                case Response:
                    invoke(eventInfo.getOnResponseMethodSet(), obj, params, request);
                    break;
                case Success:
                    invoke(eventInfo.getOnSuccessMethodSet(), obj, params, request);
                    break;
                case Error:
                    invoke(eventInfo.getOnErrorMethodSet(), obj, params, request);
                    break;
                case BuildTable:
                    invoke(eventInfo.getOnBuildMethodSet(), obj, params, request);
                    break;
                case Download:
                    invoke(eventInfo.getOnDownloadMethodSet(), obj, params, request);
                    break;
                case PreviewDoc:
                    invoke(eventInfo.getOnPreviewDocMethodSet(), obj, params, request);
                    break;
                case PlayVideo:
                    invoke(eventInfo.getOnPlayVideoMethodSet(), obj, params, request);
                    break;
                case Upload:
                    invoke(eventInfo.getOnUploadMethodSet(), obj, params, request);
                    break;
                case Import:
                    invoke(eventInfo.getOnImportMethodSet(), obj, params, request);
                    break;
                case Export:
                    invoke(eventInfo.getOnExportMethodSet(), obj, params, request);
                    break;
                default:
                    throw new IllegalAccessException("Invalid event type!!!");
            }
        }
    }

    private static <T> void invoke(Set<Method> methodSet, Object instance, T params, HttpServletRequest request) throws InvocationTargetException, IllegalAccessException {

        for(Method method : methodSet) {
            if(method.getParameterTypes()[0].equals(Map.class) && params instanceof QueryInfo) {
                method.invoke(instance, ((QueryInfo) params).getData(), request);
                continue;
            }

            if(method.getParameterTypes()[0].equals(QueryInfo.class) && params instanceof Map) {
                QueryInfo queryInfo = new QueryInfo();
                queryInfo.setData((Map) params);
                method.invoke(instance, queryInfo, request);
                continue;
            }

            method.invoke(instance, params, request);
        }
    }

    private String getTablename(Trigger ann) {
        switch (ann.tablename()) {
            case TABLE_ACL:
                return aclTablename;
            case TABLE_GROUP:
                return groupTablename;
            case TABLE_PRIVILEGE:
                return privilegeTablename;
            case TABLE_ROLE:
                return roleTablename;
            case TABLE_USER:
                return userTablename;
            default:
                return ann.tablename();
        }
    }
}
