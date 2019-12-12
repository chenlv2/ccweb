/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.controllers;


import ccait.ccweb.annotation.AccessCtrl;
import ccait.ccweb.model.*;
import ccait.ccweb.utils.UploadUtils;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import entity.query.ColumnInfo;
import entity.tool.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.xml.ws.RequestWrapper;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;


@RestController
@RequestMapping( value = {"api/{datasource}"} )
public class ApiController extends BaseController {


    /***
     * join query count
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "join/count", method = RequestMethod.POST )
    public ResponseData doJoinQueryCount(@RequestBody QueryInfo queryInfo) {

        try {
            Long result = super.joinQueryCount(queryInfo);

            return success( result );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(113, e);
        }
    }

    /***
     * join query
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "join", method = RequestMethod.POST )
    public ResponseData doJoinQuery(@RequestBody QueryInfo queryInfo) {

        try {
            List result = super.joinQuery(queryInfo);

            queryInfo.getPageInfo().setPageCount();

            return success( result, queryInfo.getPageInfo() );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(113, e);
        }
    }

    /***
     * create or alter table
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/build/table", method = {RequestMethod.POST, RequestMethod.PUT} )
    public ResponseData doCreateOrAlterTable(@PathVariable String table, @RequestBody List<ColumnInfo> columns) {
        try{

            super.createOrAlterTable(table, columns);

            return success();
        }
        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);
            return error(e.getMessage());
        }
    }
    /***
     * create or alter view
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/build/view", method = {RequestMethod.POST, RequestMethod.PUT} )
    public ResponseData doCreateOrAlterView(@PathVariable String table, @RequestBody QueryInfo queryInfo) {
        try{

            super.createOrAlterView(table, queryInfo);

            return success();
        }
        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);
            return error(e.getMessage());
        }
    }

    /***
     * get
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.GET )
    public ResponseData doGet(@PathVariable String table, @PathVariable String id)  {
        try {

            Map data = super.get(table, id);

            return success( data );
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(100, e);
        }
    }

    /***
     * query
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}", method = RequestMethod.POST )
    public ResponseData doQuery(@PathVariable String table, @RequestBody QueryInfo queryInfo) {

        try {

            List result = super.query(table, queryInfo);

            queryInfo.getPageInfo().setPageCount();

            return success( result, queryInfo.getPageInfo() );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(110, e);
        }
    }

    /***
     * query and update
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/update", method = RequestMethod.POST )
    public ResponseData doQueryUpdate(@PathVariable String table, @RequestBody QueryInfo queryInfo) {

        try {

            boolean result = super.updateByQuery(table, queryInfo);

            return success( result );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(110, e);
        }
    }

    /***
     * search
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "search/{table}", method = RequestMethod.POST )
    public ResponseData doSearch(@PathVariable String table, @RequestBody QueryInfo queryInfo) {

        try {

            SearchData result = super.search(table, queryInfo);

            queryInfo.getPageInfo().setTotalRecords(result.getPageInfo().getTotalRecords());
            queryInfo.getPageInfo().setPageCount();

            return success( result.getData(), queryInfo.getPageInfo() );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(110, e);
        }
    }

    /***
     * exist
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/exist", method = RequestMethod.POST )
    public ResponseData doExist(@PathVariable String table, @RequestBody QueryInfo queryInfo) {
        try {

            Boolean result = super.exist(table, queryInfo);

            return success( result );

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(111, e);
        }
    }

    /***
     * count
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/count", method = RequestMethod.POST )
    public ResponseData doCount(@PathVariable String table, @RequestBody QueryInfo queryInfo) {
        try {

            Long result = super.count(table, queryInfo);
            return success( result );

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(112, e);
        }
    }

    /***
     * insert
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}", method = RequestMethod.PUT )
    public ResponseData doInsert(@PathVariable String table, @RequestBody List<Map<String, Object>> postData)
    {
        try {
            List<Integer> result = new ArrayList<>();
            for(int i=0; i < postData.size(); i++) {
                Map data = (Map)postData.get(i);
                result.add(super.insert(table, data));
            }

            if(result.size() == 1) {
                return success(result.get(0));
            }

            return success(result);
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(120, e);
        }
    }

    /***
     * update
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.PUT )
    public ResponseData doUpdate(@PathVariable String table, @PathVariable String id, @RequestBody Map<String, Object> postData) {
        try {

            Integer result = super.update(table, id, postData);

            return success(result);
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(130, e);
        }
    }

    /***
     * delete
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.DELETE )
    public ResponseData doDelete(@PathVariable String table, @PathVariable String id) {
        try {

            Integer result = super.delete(table, id);

            return success(result);
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(140, e);
        }
    }


    /***
     * delete
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/list", method = RequestMethod.DELETE )
    public ResponseData deleteByIds(@PathVariable String table, @RequestBody List<String> idList) {

        List result = null;
        try {
            result = super.deleteByIdList(table, idList);
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(140, e);
        }

        return success(result);
    }

    /***
     * login
     * @return
     */
    @ResponseBody
    @RequestMapping( value = "login", method = RequestMethod.POST )
    public ResponseData loginByPassword(@RequestBody UserModel user) {
        try {

            user = super.logoin(user);

            return success(user);

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(150, e);
        }
    }

    /***
     * logout
     * @return
     */
    @ResponseBody
    @RequestMapping( value = "logout", method = RequestMethod.GET )
    public ResponseData logouted() {

        super.logout();

        return success();
    }

    /***
     * download
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "download/{table}/{field}/{id}", method = RequestMethod.GET )
    public void downloaded(@PathVariable String table, @PathVariable String field, @PathVariable String id) throws Exception {

        super.download(table, field, id);
    }

    /***
     * upload
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "upload/{table}/{field}", method = RequestMethod.POST )
    public Map<String, String> uploaded(@PathVariable String table, @PathVariable String field, @RequestBody Map<String, Object> uploadFiles) throws Exception {
        return super.upload(table, field, uploadFiles);
    }

    /***
     * preview
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "preview/{table}/{field}/{id}", method = RequestMethod.GET )
    public void previewed(@PathVariable String table, @PathVariable String field, @PathVariable String id) throws Exception {

        super.preview(table, field, id);
    }

    /***
     * export select data
     * @param table
     * @param queryInfo
     * @return
     * @throws Exception
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/export", method = RequestMethod.POST )
    public void doExport(@PathVariable String table, @RequestBody QueryInfo queryInfo) throws Exception {

        List list = query(table, queryInfo, true);

        export(table, list, queryInfo);
    }

    /***
     * export by join query
     * @param queryInfo
     * @return
     * @throws Exception
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "export/join", method = RequestMethod.POST )
    public void doExport(@RequestBody QueryInfo queryInfo) throws Exception {
        List list = joinQuery(queryInfo, true);
        export(UUID.randomUUID().toString().replace("-", ""), list, queryInfo);
    }

    /***
     * import
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/import", method = {RequestMethod.POST, RequestMethod.PUT} )
    public void doImport(@PathVariable String table, @RequestBody Map<String, Object> uploadFiles) throws Exception {
        super.importData(table, uploadFiles);
    }
}
