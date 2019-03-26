/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.trigger;


import ccait.ccweb.annotation.Trigger;
import ccait.ccweb.enums.EncryptMode;
import ccait.ccweb.filter.RequestWrapper;
import ccait.ccweb.model.QueryInfo;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.FastJsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ccait.ccweb.controllers.BaseController.*;
import static ccait.ccweb.utils.StaticVars.*;

@Component
@Scope("prototype")
@Trigger(tablename = "${entity.table.user}")
public final class UserTableTrigger implements ITrigger {

    private static final Logger log = LogManager.getLogger( UserTableTrigger.class );

    @Value("${entity.security.encrypt.AES.publicKey:ccait}")
    private String aesPublicKey;

    @Value("${entity.encoding:UTF-8}")
    private String encoding;

    @Value("${entity.security.admin.username:admin}")
    private String admin;

    @Override
    public void onInsert(Map<String, Object> data, HttpServletRequest request) throws Exception {

        List<String> keys = data.keySet().stream().collect(Collectors.toList());
        for (String key : keys) {
            String lowerKey = key.toLowerCase();
            if("id".equals(lowerKey)) {
                data.remove(key);
            }

            if("status".equals(lowerKey) || data.get(key) == null) {
                data.remove(key);
                data.put("status", 0);
            }

            if("username".equals(lowerKey) || data.get(key) == null) {
                if(data.get(key) == null) {
                    throw new Exception("username can not be empty!!!");
                }

                UserModel user = new UserModel();
                user.setUsername(data.get(key).toString());
                if(user.where("[username]=#{username}").exist()) {
                    throw new Exception(String.format("username %s already exist!!!", data.get(key)));
                }
            }
        }

        if(!data.containsKey("status")) {
            data.put("status", 0);
        }

        RequestWrapper wrapper = (RequestWrapper) request;
        wrapper.setPostParameter(data);
    }

    @Override
    public void onUpdate(Map<String, Object> data, HttpServletRequest request) throws Exception {

        List<String> keys = data.keySet().stream().collect(Collectors.toList());
        for (String key : keys) {
            String lowerKey = key.toLowerCase();
            if("username".equals(lowerKey)) {
                throw new Exception("username can not be modify!!!");
            }

            if("id".equals(lowerKey)) {
                data.remove(key);
            }
        }

        RequestWrapper wrapper = (RequestWrapper) request;
        wrapper.setPostParameter(data);

        Map<String, String> attrs = (Map<String, String>)request.getAttribute(VARS_PATH);
        if(attrs != null && attrs.containsKey("id")) {
            attrs = decryptIdString(attrs.get("id"), attrs);

            request.setAttribute(VARS_PATH, attrs);
        }
    }

    @Override
    public void onDelete(String id, HttpServletRequest request) throws Exception {

        UserModel user = (UserModel)request.getSession().getAttribute(request.getSession().getId() + LOGIN_KEY);
        if(user == null) {
            return;
        }

        Map<String, String> attrs = (Map<String, String>)request.getAttribute(VARS_PATH);

        attrs = decryptIdString(id, attrs);

        if(admin.equals(user.getUsername())) {
            UserModel currentUser = new UserModel();
            currentUser.setId(Long.parseLong(attrs.get("id")));
            currentUser = currentUser.where("[id]=#{id}").first();
            if(currentUser != null && currentUser.getUsername().equals(admin)) {
                throw new Exception("admin can not be removed!!!");
            }
        }

        if(attrs.get("id").equals(user.getId().toString())) {
            throw new Exception("can not delete self!!!");
        }

        request.setAttribute(VARS_PATH, attrs);
    }

    @Override
    public void onList(QueryInfo queryInfo, HttpServletRequest request) {

    }

    @Override
    public void onView(String id, HttpServletRequest request) {
        Map<String, String> data = (Map<String, String>)request.getAttribute(VARS_PATH);

        data = decryptIdString(id, data);

        request.setAttribute(VARS_PATH, data);
    }

    private Map decryptIdString(String id, Map data) {

        if(data == null) {
            data = new HashMap<String, Object>();
        }

        data.put("id", decrypt(id, EncryptMode.AES, aesPublicKey));

        if(data.get("id") == null) {
            log.error(LOG_PRE_SUFFIX + String.format("(UserId: %s) encryption has been failure!!!", id));
        }

        return data;
    }

    @Override
    public void onQuery(QueryInfo queryInfo, HttpServletRequest request) {

    }

    @Override
    public void onResponse(HttpServletResponse response, HttpServletRequest request) throws IOException {

    }

    @Override
    public void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception {

        if(request.getMethod().equalsIgnoreCase("GET") || request.getMethod().equalsIgnoreCase("POST")){

            if(responseData.getData() == null || isPrimitive(responseData.getData())) {
                return;
            }

            if(isPrimitive(responseData.getData())) {
                return;
            }

            List<Map> list = new ArrayList<Map>();

            boolean isMapResult = true;
            if(responseData.getData() instanceof List) {
                list = FastJsonUtils.convert(responseData.getData(), List.class);
                isMapResult = false;
            }

            else {
                Map map = FastJsonUtils.convert(responseData.getData(), Map.class);
                list.add(map);
            }

            for(int i=0; i<list.size(); i++) {

                List<String> keyList = (List) list.get(i).keySet().stream()
                        .filter(a -> a.toString().toUpperCase().equals("ID") ||
                                a.toString().toUpperCase().equals("PASSWORD"))
                        .map(b -> b.toString()).collect(Collectors.toList());

                if (keyList == null) {
                    return;
                }

                for (String key : keyList) {
                    String id = encrypt(list.get(i).get(key).toString(), EncryptMode.AES, aesPublicKey);
                    if(key.toUpperCase().equals("ID")) {
                        list.get(i).remove(key);
                        list.get(i).put("userId", id);
                    }

                    else if(key.toUpperCase().equals("PASSWORD")) {
                        list.get(i).remove(key);
                    }
                }
            }

            if(isMapResult) {
                responseData.setData(list.get(0));
            }

            else {
                responseData.setData(list);
            }
        }
    }

    @Override
    public void onError(Exception ex, HttpServletRequest request) {

    }
}
