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
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.controllers.BaseController;
import ccait.ccweb.filter.RequestWrapper;
import ccait.ccweb.model.ConditionInfo;
import ccait.ccweb.model.QueryInfo;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.FastJsonUtils;
import entity.query.ColumnInfo;
import entity.query.Datetime;
import entity.query.Queryable;
import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ccait.ccweb.controllers.BaseController.getTablename;
import static ccait.ccweb.controllers.BaseController.isPrimitive;


@Component
@Scope("prototype")
@Trigger
public final class DefaultTrigger {

    private static final Logger log = LogManager.getLogger( DefaultTrigger.class );

    @Value("${entity.table.reservedField.userPath:userPath}")
    private String userPathField;

    @Value("${entity.table.reservedField.createOn:createTime}")
    private String createOnField;

    @Value("${entity.table.reservedField.modifyOn:modifyTime}")
    private String modifyOnField;

    @Value("${entity.table.reservedField.createBy:createBy}")
    private String createByField;

    @Value("${entity.encoding:UTF-8}")
    private String encoding;

    @Autowired
    private QueryInfo queryInfo;

    @OnInsert
    public void onInsert(Map<String, Object> data, HttpServletRequest request) throws Exception {

        vaildPostData(data);

        UserModel user = (UserModel)request.getSession().getAttribute(request.getSession().getId() + BaseController.LOGIN_KEY);
        if(user != null) {
            if(StringUtils.isEmpty(user.getPath())) {
                data.put(userPathField, user.getId() + "/" + user.getId());
            }
            else {
                data.put(userPathField, user.getPath() + "/" + user.getId());
            }
            data.put(createByField, user.getId());
        }

        data.put(createOnField, Datetime.now());

        RequestWrapper wrapper = (RequestWrapper) request;
        wrapper.setPostParameter(data);
    }

    @OnUpdate
    public void onUpdate(Map<String, Object> data, HttpServletRequest request) throws Exception {

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

        data.put(modifyOnField, Datetime.now());

        RequestWrapper wrapper = (RequestWrapper) request;
        wrapper.setPostParameter(data);
    }

    @OnBuildTable
    public void onBuild(List<ColumnInfo> columns, HttpServletRequest request) throws Exception {

        Object entity = EntityContext.getEntity(BaseController.getTablename(), queryInfo);
        if(entity == null) {
            throw new Exception("Can not find entity!!!");
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
        RequestWrapper wrapper = (RequestWrapper) request;
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

                if(opt.isPresent() && !Pattern.matches(opt.get().toString(), data.get(key).toString())){
                    throw new Exception("无效的参数(" + key + " field has invalid parameter value!!!)");
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
        if(map != null) {
            for(ConditionInfo condition : queryInfo.getConditionList()){
                Optional opt = map.keySet().stream()
                        .filter(a -> a.equals(condition.getName()) ||
                                String.format("%s.%s", getTablename(), condition.getName()).equals(a))
                        .findAny();

                if(opt.isPresent() && !Pattern.matches(opt.get().toString(), condition.getValue().toString())){
                    throw new Exception("无效的参数(" + condition.getName() + " field has invalid parameter value!!!)");
                }
            }
        }
    }

    @OnSuccess
    public void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception {

        if(request.getMethod().equalsIgnoreCase("POST")){

            if(responseData.getData() == null || isPrimitive(responseData.getData())) {
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
                List<String> keyList = (List<String>) list.get(i).keySet().stream().map(a->a.toString()).collect(Collectors.toList());
                for(Object key : keyList) {
                    if(ApplicationConfig.getInstance().get(String.format("${entity.table.display.%s.%s}", BaseController.getTablename(), key.toString())).equals("hidden")) {
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
}
