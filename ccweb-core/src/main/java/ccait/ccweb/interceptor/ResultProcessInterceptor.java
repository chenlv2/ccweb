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


import ccait.ccweb.context.TriggerContext;
import ccait.ccweb.controllers.BaseController;
import ccait.ccweb.enums.EventType;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.utils.FastJsonUtils;
import entity.query.Datetime;
import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static ccait.ccweb.context.ApplicationContext.LOG_PRE_SUFFIX;
import static ccait.ccweb.controllers.BaseController.isPrimitive;

@Aspect // FOR AOP
@Order(-55555) // 控制多个Aspect的执行顺序，越小越先执行
@Component
public class ResultProcessInterceptor {

    private static final Logger log = LogManager.getLogger( ResultProcessInterceptor.class );

    @Autowired
    protected HttpServletRequest request;

    @Autowired
    protected HttpServletResponse response;

    private String tablename = null;

    @Around("@annotation(ccait.ccweb.annotation.OnResult)") //execution为AspectJ语法
    public Object processing(ProceedingJoinPoint proceedingJoinPoint) {

        ResponseData result = new ResponseData();
        result.setCode(0);

        try {

            tablename = BaseController.getTablename();
            
            Object obj = proceedingJoinPoint.proceed();//调用执行目标方法

            if(obj != null) {

                if(obj instanceof ResponseData) {
                    result = (ResponseData) obj;
                }
            }

            else {
                result.setData(obj);
            }

            result.setUuid(UUID.randomUUID());

            if(result.getCode() != 0 || response.getStatus() != 200) {
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

    private Object getFormatedData(String tablename, Object data) throws IOException {

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

                        if (opt.isPresent()) {
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
                return dataList;
            }

            if(dataList.size() != 1) {
                return dataList;
            }

            return dataList.get(0);
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
}
