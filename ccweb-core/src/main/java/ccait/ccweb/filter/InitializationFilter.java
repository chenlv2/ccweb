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
import entity.query.core.ApplicationConfig;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static ccait.ccweb.utils.NetworkUtils.getClientIp;
import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;

@javax.servlet.annotation.WebFilter(urlPatterns = "/*")
public class InitializationFilter implements WebFilter, Filter {

    private static final Logger log = LogManager.getLogger( InitializationFilter.class );

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

        res.setHeader("Access-Control-Allow-Origin","*");
        res.setHeader("Access-Control-Allow-Credentials", "true");
        res.setHeader("Access-Control-Allow-Methods", "POST, GET, PATCH, DELETE, PUT");
        res.setHeader("Access-Control-Max-Age", "3600");
        res.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        res.setContentType("application/json; charset=utf-8");
        res.setCharacterEncoding("UTF-8");

        RequestWrapper requestWrapper = new RequestWrapper(req);
//        ResponseWrapper responseWrapper = new ResponseWrapper(res);

        log.info(LOG_PRE_SUFFIX + "Request Url：" + requestWrapper.getRequestURL());
        final long startTime = System.currentTimeMillis();

        try
        {
            log.info("entity.enableFlux============================================>>>" + ApplicationConfig.getInstance().get("${entity.enableFlux}"));
            if("true".equals(ApplicationConfig.getInstance().get("${entity.enableFlux}")) &&
                    req.getRequestURI().toLowerCase().startsWith("/api")) {

                String path = req.getRequestURI();
                List<String> list = StringUtils.splitString2List(path, "/");
                list.set(1, "asyncapi");
                path = StringUtils.join("/", list);

                request.getRequestDispatcher(path).include(requestWrapper, response);
                return;
            }

            chain.doFilter(requestWrapper, res);
            /*** don't need responseBody
            chain.doFilter(requestWrapper, responseWrapper);
             ***/

            try {
                log.info(LOG_PRE_SUFFIX + "Status：" + res.getStatus());
                log.info(LOG_PRE_SUFFIX + "Client Ip：" + getClientIp(req));
                try {
                    log.info(LOG_PRE_SUFFIX + "Server Ip：" +  InetAddress.getLocalHost().getHostAddress());
                } catch (UnknownHostException e) {
                    log.error( LOG_PRE_SUFFIX + e.getMessage(), e );
                }

                log.info(LOG_PRE_SUFFIX + "Method：" + req.getMethod());

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

                /*** don't need responseBody
                final String responseBody = responseWrapper.getResponseBody();
                if(StringUtils.isNotEmpty(responseBody)) {
                    responseWrapper.flushBuffer();
                }

                log.info(LOG_PRE_SUFFIX + "responseBody：" + responseBody);
                ***/

                if(StringUtils.isNotEmpty(BaseController.getTablename()))    {
                    TriggerContext.exec(BaseController.getTablename(), EventType.Response, res, requestWrapper);
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
            responseData.setStatus(-2);
            responseData.setMessage(message);

            res.reset();
            res.getWriter().write(JsonUtils.toJson(responseData));
            res.getWriter().flush();
            res.getWriter().close();
        }
    }

    @Override
    public void destroy() {

    }

    private String getErrorMessage(Exception e) {
        String message = e.getMessage();
        if(e.getCause() != null && ((InvocationTargetException)e.getCause()).getTargetException() != null) {
            message = ((InvocationTargetException)e.getCause()).getTargetException().getMessage();
        }

        return message;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
        return null;
    }
}
