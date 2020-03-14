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


import ccait.ccweb.annotation.*;
import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.controllers.BaseController;
import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.entites.ConditionInfo;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.entites.ResponseData;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.FastJsonUtils;
import entity.query.ColumnInfo;
import entity.query.Datetime;
import entity.query.Queryable;
import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.sql.Blob;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ccait.ccweb.controllers.BaseController.getTablename;
import static ccait.ccweb.controllers.BaseController.isPrimitive;
import static ccait.ccweb.utils.StaticVars.CURRENT_DATASOURCE;
import static ccait.ccweb.utils.StaticVars.LOGIN_KEY;


@Component
@Scope("prototype")
@Trigger
public final class DefaultTrigger {

    private static final Logger log = LogManager.getLogger( DefaultTrigger.class );

    @Value("${entity.table.reservedField.userPath:userPath}")
    private String userPathField;

    @Value("${entity.table.reservedField.createOn:createOn}")
    private String createOnField;

    @Value("${entity.table.reservedField.modifyOn:modifyOn}")
    private String modifyOnField;

    @Value("${entity.table.reservedField.createBy:createBy}")
    private String createByField;

    @Value("${entity.table.reservedField.modifyBy:modifyBy}")
    private String modifyByField;

    @Autowired
    private QueryInfo queryInfo;

    private String datasourceId;

    @PostConstruct
    private void construct() {
        datasourceId = (String) ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE);
    }

    @OnInsert
    public void onInsert(List<Map<String, Object>> list, HttpServletRequest request) throws Exception {

        boolean hasUserPath = EntityContext.hasColumn(datasourceId, getTablename(), userPathField);
        boolean hasCreateBy = EntityContext.hasColumn(datasourceId, getTablename(), createByField);
        boolean hasCreateOn = EntityContext.hasColumn(datasourceId, getTablename(), createOnField);
        UserModel user = (UserModel) request.getSession().getAttribute(request.getSession().getId() + LOGIN_KEY);

        for(Map item : list) {

            vaildPostData(item);

            if(user != null) {
                if(hasUserPath) {
                    if (StringUtils.isEmpty(user.getPath())) {
                        item.put(userPathField, user.getId() + "/" + user.getId());
                    } else {
                        item.put(userPathField, user.getPath() + "/" + user.getId());
                    }
                }

                if(hasCreateBy) {
                    item.put(createByField, user.getId());
                }
            }
            else {
                throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
            }

            if(hasCreateOn) {
                item.put(createOnField, Datetime.now());
            }
        }

        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        wrapper.setPostParameter(list);
    }

    @OnUpdate
    public void onUpdate(QueryInfo queryInfo, HttpServletRequest request) throws Exception {

        Map<String, Object> data = queryInfo.getData();

        vaildPostData(data);

        if(data.containsKey(createOnField)) {
            data.remove(createOnField);
        }

        if(data.containsKey(userPathField)) {
            data.remove(userPathField);
        }

        if(data.containsKey(createByField)) {
            data.remove(createByField);
        }

        boolean hasModifyByField = EntityContext.hasColumn(datasourceId, getTablename(), modifyByField);
        boolean hasModifyOnField = EntityContext.hasColumn(datasourceId, getTablename(), modifyOnField);

        UserModel user = (UserModel)request.getSession().getAttribute(request.getSession().getId() + LOGIN_KEY);
        if(user != null) {
            if(hasModifyByField) {
                data.put(modifyByField, user.getId());
            }
        }
        else{
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }

        if(hasModifyOnField) {
            data.put(modifyOnField, Datetime.now());
        }

        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        String[] arr = request.getRequestURI().split("/");
        if("update".equals(arr[arr.length - 1].toLowerCase())) {
            wrapper.setPostParameter(queryInfo);
        }

        else {
            wrapper.setPostParameter(data);
        }
    }

    @OnBuildTable
    public void onBuild(List<ColumnInfo> columns, HttpServletRequest request) throws Exception {

        Object entity = EntityContext.getEntity(BaseController.getTablename(), queryInfo);
        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        Queryable query = (Queryable)entity;

        if(Queryable.exist(query.dataSource().getId(), BaseController.getTablename())) {
            return;
        }

        ColumnInfo col = null;
        if(StringUtils.isNotEmpty(userPathField) && !columns.stream()
                .filter(a->userPathField.toLowerCase().equals(a.getColumnName().toLowerCase()))
                .findAny().isPresent()) {

            col = new ColumnInfo();
            col.setColumnName(userPathField);
            col.setCanNotNull(true);
            col.setMaxLength(2048);
            col.setType(String.class);
            columns.add(col);
        }

        if(StringUtils.isNotEmpty(createOnField) && !columns.stream()
                .filter(a->createOnField.toLowerCase().equals(a.getColumnName().toLowerCase()))
                .findAny().isPresent()) {

            col = new ColumnInfo();
            col.setColumnName(createOnField);
            col.setType(Date.class);
            columns.add(col);
        }

        if(StringUtils.isNotEmpty("status") && !columns.stream()
                .filter(a->"status".toLowerCase().equals(a.getColumnName().toLowerCase()))
                .findAny().isPresent()) {

            col = new ColumnInfo();
            col.setColumnName("status");
            col.setType(Integer.class);
            columns.add(col);
        }

        if(StringUtils.isNotEmpty(createByField) && !columns.stream()
                .filter(a->createByField.toLowerCase().equals(a.getColumnName().toLowerCase()))
                .findAny().isPresent()) {

            col = new ColumnInfo();
            col.setColumnName(createByField);
            col.setCanNotNull(true);
            col.setType(Long.class);
            columns.add(col);
        }
        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        wrapper.setPostParameter(columns);
    }

    private void vaildPostData(Map<String, Object> data) throws Exception {
        Map<String, Object> map = ApplicationConfig.getInstance().getMap("entity.validation");
        if(map != null) {
            for(String key : data.keySet()){
                Optional opt = map.keySet().stream()
                        .filter(a -> a.equals(key) ||
                                String.format("%s.%s", BaseController.getTablename(), key).equals(a))
                        .findAny();

                if(!opt.isPresent()){
                    continue;
                }

                Map vaildation = (Map) opt.get();
                if(vaildation.containsKey("match")) {
                    if(!Pattern.matches(vaildation.get("match").toString(), data.get(key).toString())){
                        if(vaildation.containsKey("message")) {
                            throw new Exception(vaildation.get("message").toString());
                        }
                        throw new Exception(key + LangConfig.getInstance().get("field_has_invalid_parameter_value"));
                    }
                }
            }
        }
    }

    @OnList
    public void onList(QueryInfo queryInfo, HttpServletRequest request) throws Exception {

        vaildCondition(queryInfo);
    }

    @OnQuery
    public void onQuery(QueryInfo queryInfo, HttpServletRequest request) throws Exception {

        vaildCondition(queryInfo);
    }

    private void vaildCondition(QueryInfo queryInfo) throws Exception {
        Map<String, Object> map = ApplicationConfig.getInstance().getMap("entity.validation");
        if(map != null && queryInfo.getConditionList() != null) {
            for(ConditionInfo condition : queryInfo.getConditionList()){
                Optional opt = map.keySet().stream()
                        .filter(a -> a.equals(condition.getName()) ||
                                String.format("%s.%s", getTablename(), condition.getName()).equals(a))
                        .findAny();

                if(opt.isPresent() && !Pattern.matches(opt.get().toString(), condition.getValue().toString())){
                    throw new Exception(condition.getName() + LangConfig.getInstance().get("field_has_invalid_parameter_value"));
                }
            }
        }
    }

    @OnSuccess
    public void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception {

        if(responseData.getData() == null || isPrimitive(responseData.getData())) {
            return;
        }

        if(request.getMethod().equalsIgnoreCase("POST")){

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
                if(list.get(i)==null) {
                    continue;
                }
                List<String> keyList = (List<String>) list.get(i).keySet().stream().map(a->a!=null ? a.toString() : "").collect(Collectors.toList());
                for(Object key : keyList) {
                    if(ApplicationConfig.getInstance().get(String.format("${entity.table.display.%s.%s}", BaseController.getTablename(), key.toString())).equals("hidden")) {
                        list.get(i).remove(key);
                    }

                    else if(list.get(i).get(key) instanceof Blob) {
                        list.get(i).remove(key);
                    }

                    else if(list.get(i).get(key) instanceof String) {
                        int index = list.get(i).get(key).toString().indexOf("|::|");
                        if(index > 0 && Pattern.matches("[^/]+/[^/:]+::[^/:]+::[^/:\\|]+", list.get(i).get(key).toString().substring(0, index))) {
                            list.get(i).remove(key);
                        }
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

        else if(request.getMethod().equalsIgnoreCase("GET")) {
            if(responseData.getData() instanceof Map) {
                Map<String, Object> dataMap = ((Map<String, Object>) responseData.getData());
                Set<String> keys = dataMap.keySet();
                for(String key : keys) {
                    if(dataMap.get(key) instanceof Blob) {
                        dataMap.remove(key);
                    }

                    else if(dataMap.get(key) instanceof String) {
                        int index = dataMap.get(key).toString().indexOf("|::|");
                        if(index > 0 && Pattern.matches("[^/]+/[^/:]+::[^/:]+::[^/:\\|]+", dataMap.get(key).toString().substring(0, index))) {
                            dataMap.remove(key);
                        }
                    }
                }
            }
        }
    }
}
