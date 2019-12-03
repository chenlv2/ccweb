package ccait.ccweb.trigger;


import ccait.ccweb.annotation.Trigger;
import ccait.ccweb.controllers.BaseController;
import ccait.ccweb.filter.RequestWrapper;
import ccait.ccweb.model.QueryInfo;
import ccait.ccweb.model.ResponseData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Scope("prototype")
@Trigger(tablename = "${entity.table.role}")
public final class RoleTableTrigger implements ITrigger {

    private static final Logger log = LogManager.getLogger( RoleTableTrigger.class );

    @Value("${entity.table.reservedField.roleId:roleId}")
    private String roleIdField;

    @Override
    public void onInsert(Map<String, Object> data, HttpServletRequest request) {

        data.put(roleIdField, UUID.randomUUID().toString().replace("-", ""));
        RequestWrapper wrapper = (RequestWrapper) request;
        wrapper.setPostParameter(data);
    }

    @Override
    public void onUpdate(Map<String, Object> data, HttpServletRequest request) {
        if(data.containsKey(roleIdField)) {
            data.remove(roleIdField);
        }
        RequestWrapper wrapper = (RequestWrapper) request;
        wrapper.setPostParameter(data);
    }

    @Override
    public void onDelete(String id, HttpServletRequest request) {
    }

    @Override
    public void onList(QueryInfo queryInfo, HttpServletRequest request) throws IOException {
    }

    @Override
    public void onView(String id, HttpServletRequest request) {}

    @Override
    public void onQuery(QueryInfo queryInfo, HttpServletRequest request) throws IOException {
    }

    @Override
    public void onResponse(HttpServletResponse response, HttpServletRequest request) throws IOException {
    }

    @Override
    public void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception {
    }

    @Override
    public void onError(Exception ex, HttpServletRequest request) {
    }

    @Override
    public void onUpload(byte[] data, HttpServletRequest request) {
    }

    @Override
    public void onDownload(BaseController.DownloadData data, HttpServletRequest request) {
    }

    @Override
    public void onPreviewDoc(BaseController.DownloadData data, HttpServletRequest request) {
    }
}
