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
import ccait.ccweb.model.QueryInfo;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.model.SearchData;
import ccait.ccweb.model.UserModel;
import entity.query.ColumnInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;


@RestController
@RequestMapping( value = {"asyncapi/{datasource}"} )
public class AsyncApiController extends BaseController {

    /***
     * join query
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "join", method = RequestMethod.POST )
    public Mono doJoinQuery(@RequestBody QueryInfo queryInfo) {

        try {
            List result = super.joinQuery(queryInfo);

            queryInfo.getPageInfo().setPageCount();

            return successAs( result, queryInfo.getPageInfo() );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(113, e);
        }
    }

    /***
     * create or alter table
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/build/table", method = {RequestMethod.POST, RequestMethod.PUT}  )
    public Mono doCreateOrAlterTable(@PathVariable String table, @RequestBody List<ColumnInfo> columns) {
        try{

            super.createOrAlterTable(table, columns);

            return successAs();
        }
        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);
            return errorAs(e.getMessage());
        }
    }
    /***
     * create or alter view
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/build/view", method = {RequestMethod.POST, RequestMethod.PUT}  )
    public Mono doCreateOrAlterView(@PathVariable String table, @RequestBody QueryInfo queryInfo) {
        try{

            super.createOrAlterView(table, queryInfo);

            return successAs();
        }
        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);
            return errorAs(e.getMessage());
        }
    }

    /***
     * get
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.GET  )
    public Mono doGet(@PathVariable String table, @PathVariable String id)  {
        try {

            Map data = super.get(table, id);

            return successAs( data );
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(100, e);
        }
    }

    /***
     * query
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}", method = RequestMethod.POST  )
    public Mono doQuery(@PathVariable String table, @RequestBody QueryInfo queryInfo) {

        try {

            List result = super.query(table, queryInfo);

            queryInfo.getPageInfo().setPageCount();

            return successAs( result, queryInfo.getPageInfo() );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(110, e);
        }
    }

    /***
     * search
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "search/{table}", method = RequestMethod.POST  )
    public Mono doSearch(@PathVariable String table, @RequestBody QueryInfo queryInfo) {

        try {

            SearchData result = super.search(table, queryInfo);

            queryInfo.getPageInfo().setTotalRecords(result.getPageInfo().getTotalRecords());
            queryInfo.getPageInfo().setPageCount();

            return successAs( result.getData(), queryInfo.getPageInfo() );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(110, e);
        }
    }

    /***
     * exist
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/exist", method = RequestMethod.POST  )
    public Mono doExist(@PathVariable String table, @RequestBody QueryInfo queryInfo) {
        try {

            Boolean result = super.exist(table, queryInfo);

            return successAs( result );

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(111, e);
        }
    }

    /***
     * count
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/count", method = RequestMethod.POST  )
    public Mono doCount(@PathVariable String table, @RequestBody QueryInfo queryInfo) {
        try {

            Long result = super.count(table, queryInfo);
            return successAs( result );

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(112, e);
        }
    }

    /***
     * insert
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}", method = RequestMethod.PUT  )
    public Mono doInsert(@PathVariable String table, @RequestBody List<Map<String, Object>> postData)
    {
        try {
            List<Integer> result = new ArrayList<>();
            for(int i=0; i < postData.size(); i++) {
                Map data = (Map)postData.get(i);
                result.add(super.insert(table, data));
            }

            if(result.size() == 1) {
                return successAs(result.get(0));
            }

            return successAs(result);
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(120, e);
        }
    }

    /***
     * update
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.PUT  )
    public Mono doUpdate(@PathVariable String table, @PathVariable String id, @RequestBody Map<String, Object> postData) {
        try {

            Integer result = super.update(table, id, postData);

            return successAs(result);
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(130, e);
        }
    }

    /***
     * query and update
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/update", method = RequestMethod.PUT )
    public Mono doQueryUpdate(@PathVariable String table, @RequestBody QueryInfo queryInfo) {

        try {

            boolean result = super.updateByQuery(table, queryInfo);

            return successAs( result );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(110, e);
        }
    }

    /***
     * delete
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.DELETE  )
    public Mono doDelete(@PathVariable String table, @PathVariable String id) {
        try {

            Integer result = super.delete(table, id);

            return successAs(result);
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(140, e);
        }
    }


    /***
     * delete
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/list", method = RequestMethod.DELETE  )
    public Mono deleteByIds(@PathVariable String table, @RequestBody List<String> idList) {

        List result = null;
        try {
            result = super.deleteByIdList(table, idList);
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(140, e);
        }

        return successAs(result);
    }

    /***
     * login
     * @return
     */
    @ResponseBody
    @RequestMapping( value = "login", method = RequestMethod.POST  )
    public Mono loginByPassword(@RequestBody UserModel user) {
        try {

            user = super.logoin(user);

            return successAs(user);

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(150, e);
        }
    }

    /***
     * logout
     * @return
     */
    @ResponseBody
    @RequestMapping( value = "logout", method = RequestMethod.GET  )
    public Mono logouted() {

        super.logout();

        return successAs();
    }

    /***
     * download
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "download/{table}/{field}/{id}", method = RequestMethod.GET  )
    public void downloaded(@PathVariable String table, @PathVariable String field, @PathVariable String id) throws Exception {

        super.download(table, field, id);
    }

    /***
     * preview
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "preview/{table}/{field}/{id}", method = RequestMethod.GET  )
    public Mono previewed(@PathVariable String table, @PathVariable String field, @PathVariable String id) throws Exception {

        return super.previewAs(table, field, id);
    }

    /***
     * query
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/stream", method = RequestMethod.POST, produces= MediaType.APPLICATION_STREAM_JSON_VALUE  )
    public Mono stream(@PathVariable String table, @RequestBody QueryInfo queryInfo) {

        try {

            List result = super.query(table, queryInfo);

            queryInfo.getPageInfo().setPageCount();

            return successAs( result, queryInfo.getPageInfo() );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(110, e);
        }
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
    public Mono doExport(@PathVariable String table, @RequestBody QueryInfo queryInfo) throws Exception {
        List list = query(table, queryInfo);
        return exportAs(table, list, queryInfo);
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
    public Mono doExport(@RequestBody QueryInfo queryInfo) throws Exception {
        List list = joinQuery(queryInfo);
        return exportAs(UUID.randomUUID().toString().replace("-", ""), list, queryInfo);
    }

}
