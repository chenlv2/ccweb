/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.filter;

import ccait.ccweb.context.TriggerContext;
import ccait.ccweb.controllers.BaseController;
import ccait.ccweb.enums.EventType;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.utils.FastJsonUtils;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ccait.ccweb.context.ApplicationContext.LOG_PRE_SUFFIX;

@WebFilter(urlPatterns = "/*")
public class RequestFilter implements Filter  {

    private static final Logger log = LogManager.getLogger( RequestFilter.class );

    private final static ExecutorService executor = Executors.newFixedThreadPool( 5 );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // TODO Auto-generated method stub

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        final HttpServletRequest req = (HttpServletRequest)request;
        final HttpServletResponse res = (HttpServletResponse)response;

        RequestWrapper requestWrapper = new RequestWrapper(req);
        ResponseWrapper responseWrapper = new ResponseWrapper(res);

        log.info(LOG_PRE_SUFFIX + "Request Url：" + requestWrapper.getRequestURL());
        final long startTime = System.currentTimeMillis();

        try
        {
            chain.doFilter(requestWrapper, responseWrapper);

            try {
                log.info(LOG_PRE_SUFFIX + "Status：" + responseWrapper.getStatus());
                log.info(LOG_PRE_SUFFIX + "Client Ip：" + requestWrapper.getClientIp());
                try {
                    log.info(LOG_PRE_SUFFIX + "Server Ip：" +  InetAddress.getLocalHost().getHostAddress());
                } catch (UnknownHostException e) {
                    log.error( LOG_PRE_SUFFIX + e.getMessage(), e );
                }

                log.info(LOG_PRE_SUFFIX + "Method：" + requestWrapper.getMethod());

                Map<String, Object> oHeaderMap = new HashMap<String, Object>();
                Enumeration headerNames = requestWrapper.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String key = (String) headerNames.nextElement();
                    String value = requestWrapper.getHeader(key);

                    oHeaderMap.put(key, value);
                }

                log.info(LOG_PRE_SUFFIX + "Header：" + FastJsonUtils.convertObjectToJSON(oHeaderMap));

                String postString = requestWrapper.getRequestPostString();
                if(!StringUtils.isEmpty(postString)) {
                    log.info(LOG_PRE_SUFFIX + "Post String：" + postString);
                }

                final String responstBody = responseWrapper.getResponseBody();
                if(StringUtils.isNotEmpty(responstBody)) {
                    responseWrapper.flushBuffer();
                }
                else {
                    chain.doFilter(requestWrapper, res);
                }
                log.info(LOG_PRE_SUFFIX + "responstBody：" + responstBody);

                if(StringUtils.isNotEmpty(BaseController.getTablename()))    {
                    TriggerContext.exec(BaseController.getTablename(), EventType.Response, responseWrapper, requestWrapper);
                }
            }
            catch (Exception ex) {

                String message = getErrorMessage(ex);

                log.error( LOG_PRE_SUFFIX + message, ex );
            }

            final long endTime = System.currentTimeMillis() - startTime;
            log.info(LOG_PRE_SUFFIX + "TimeMillis：" + endTime + "ms");
        }

        catch ( Exception e )
        {
            String message = getErrorMessage(e);

            log.error( LOG_PRE_SUFFIX + message, e );

            ResponseData responseData = new ResponseData();
            responseData.setCode(-2);
            responseData.setMessage(message);

            responseWrapper.reset();
            responseWrapper.writeBuffer(responseData);
            responseWrapper.flushBuffer();
        }
    }

    private String getErrorMessage(Exception e) {
        String message = e.getMessage();
        if(e.getCause() != null && ((InvocationTargetException)e.getCause()).getTargetException() != null) {
            message = ((InvocationTargetException)e.getCause()).getTargetException().getMessage();
        }

        return message;
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }
}
