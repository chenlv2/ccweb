package ccait.ccweb.utils;

import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.enums.PrivilegeScope;
import entity.query.Datetime;
import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static ccait.ccweb.utils.NetworkUtils.getClientIp;
import static ccait.ccweb.utils.StaticVars.*;

public class PermissionUtils {

    public static final Logger log = LogManager.getLogger( PermissionUtils.class );
    public static final Pattern tablePattern = Pattern.compile("^/(api|asyncapi)(/[^/]+){1,2}/build/table$", Pattern.CASE_INSENSITIVE);
    public static final Pattern viewPattern = Pattern.compile("^/(api|asyncapi)(/[^/]+){1,2}/build/view$", Pattern.CASE_INSENSITIVE);
    public static final Pattern uploadPattern = Pattern.compile("^/(api|asyncapi)(/[^/]+){1,3}/upload$", Pattern.CASE_INSENSITIVE);
    public static final Pattern importPattern = Pattern.compile("^/(api|asyncapi)(/[^/]+){1,2}/import$", Pattern.CASE_INSENSITIVE);
    public static final Pattern exportPattern = Pattern.compile("^/(api|asyncapi)(/[^/]+){1,2}/export", Pattern.CASE_INSENSITIVE);
    public static final Pattern updatePattern = Pattern.compile("^/(api|asyncapi)(/[^/]+){1,2}/update$", Pattern.CASE_INSENSITIVE);
    public static final Pattern deletePattern = Pattern.compile("^/(api|asyncapi)(/[^/]+){1,2}/delete$", Pattern.CASE_INSENSITIVE);

    public static boolean allowIp(HttpServletRequest request, String whiteListText, String blackListText) {

        log.info("whiteList: " + whiteListText);
        log.info("blackList: " + blackListText);

        List<String> whiteList = StringUtils.splitString2List(whiteListText, ",");
        List<String> blackList = StringUtils.splitString2List(blackListText, ",");

        String accessIP = getClientIp(request);
        log.info("access ip =====>>> " + accessIP);
        if(StringUtils.isEmpty(accessIP)) {
            return false;
        }

        if(whiteList.size() > 0) {
            if (whiteList.contains(accessIP)) {
                return true;
            }

            else {
                return false;
            }
        }

        if(blackList.size() > 0) {
            if (blackList.contains(accessIP)) {
                return false;
            }
        }

        return true;
    }

    public static class InitLocalMap {
        private boolean myResult;
        private HttpServletResponse response;
        private Map<String, String> attrs;
        private String currentTable;

        public InitLocalMap(HttpServletResponse response, Map<String, String> attrs) {
            this.response = response;
            this.attrs = attrs;
        }

        public boolean is() {
            return myResult;
        }

        public String getCurrentTable() {
            return currentTable;
        }

        public InitLocalMap invoke() throws IOException {
            String datasource = null;
            currentTable = null;

            if(attrs != null) {
                datasource = attrs.get("datasource");
                if(!"join".equalsIgnoreCase(attrs.get("table"))) {
                    currentTable = attrs.get("table");
                }
            }

            if(StringUtils.isNotEmpty(datasource)){
                final String ds = datasource;
                List<String> datasourceList = StringUtils.splitString2List(ApplicationConfig.getInstance().get("${entity.datasource}", ""), ",");
                Optional<String> opt = datasourceList.stream()
                        .filter(a -> a.toLowerCase().equals(ds.toLowerCase())).findAny();
                if(opt == null || !opt.isPresent()) {
                    if(!response.isCommitted()) {
                        response.sendError(HttpStatus.NOT_FOUND.value(), LangConfig.getInstance().get("can_not_access_this_database"));
                    }
                    myResult = true;
                    return this;
                }
            }

            ApplicationContext.getThreadLocalMap().put(CURRENT_DATASOURCE, datasource);
            if(StringUtils.isNotEmpty(currentTable)) {
                ApplicationContext.getThreadLocalMap().put(CURRENT_TABLE, currentTable);
                ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + currentTable, PrivilegeScope.DENIED);
            }
            myResult = false;
            return this;
        }
    }
}
