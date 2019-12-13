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
import ccait.ccweb.model.UserModel;
import entity.tool.util.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;


@Controller
public class MvcController extends BaseController {

    /***
     * get
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.GET, produces="application/json;charset=UTF-8" )
    public Mono doGet(@PathVariable String table, @PathVariable String id, final Model model)  {
        try {

            Map data = super.get(table, id);

            model.addAttribute("data", data);

            return Mono.create(monoSink -> monoSink.success(String.format("%s/get", table)));
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return Mono.create(monoSink -> monoSink.error(e));
        }
    }

    /***
     * query
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}", method = RequestMethod.POST, produces="application/json;charset=UTF-8" )
    public Mono doQuery(@PathVariable String table, @RequestBody QueryInfo queryInfo, final Model model) {

        try {

            List result = super.query(table, queryInfo);

            queryInfo.getPageInfo().setPageCount();

            model.addAttribute("data", result);
            model.addAttribute("pageInfo", queryInfo.getPageInfo());

            return Mono.create(monoSink -> monoSink.success(String.format("%s/query", table)));
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return Mono.create(monoSink -> monoSink.error(e));
        }
    }

    /***
     * exist
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/exist", method = RequestMethod.POST, produces="application/json;charset=UTF-8" )
    public Mono doExist(@PathVariable String table, @RequestBody QueryInfo queryInfo, final Model model) {
        try {

            Boolean result = super.exist(table, queryInfo);

            model.addAttribute(result);

            return Mono.create(monoSink -> monoSink.success(String.format("%s/exist", table)));

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return Mono.create(monoSink -> monoSink.error(e));
        }
    }

    /***
     * count
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/count", method = RequestMethod.POST, produces="application/json;charset=UTF-8" )
    public Mono doCount(@PathVariable String table, @RequestBody QueryInfo queryInfo, final Model model) {
        try {

            Long result = super.count(table, queryInfo);

            model.addAttribute(result);

            return Mono.create(monoSink -> monoSink.success(String.format("%s/count", table)));

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return Mono.create(monoSink -> monoSink.error(e));
        }
    }

    /***
     * join query
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "join", method = RequestMethod.POST, produces="application/json;charset=UTF-8" )
    public Mono doJoinQuery(@RequestBody QueryInfo queryInfo, final Model model) {

        try {
            List result = super.joinQuery(queryInfo);

            queryInfo.getPageInfo().setPageCount();


            model.addAttribute("data", result);
            model.addAttribute("pageInfo", queryInfo.getPageInfo());

            List<String> tablenames = queryInfo.getJoinTables().stream()
                    .map(a->a.getTablename()).collect(Collectors.toList());

            return Mono.create(monoSink -> monoSink.success(String.format("join/%s", StringUtils.join("/", tablenames))));

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return Mono.create(monoSink -> monoSink.error(e));
        }
    }

    /***
     * insert and select id
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/max/{field}", method = RequestMethod.PUT )
    public Mono doInsertAndReturnId(@PathVariable String table, @PathVariable String field, @RequestBody List<Map<String, Object>> postData)
    {
        try {
            List<String> result = new ArrayList<>();
            for(int i=0; i < postData.size(); i++) {
                Map data = (Map)postData.get(i);
                result.add(super.insert(table, data, field));
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
     * insert
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}", method = RequestMethod.PUT, produces="application/json;charset=UTF-8" )
    public Mono doInsert(@PathVariable String table, @RequestBody Map<String, Object> postData, final Model model)
    {
        try {

            Object result = super.insert(table, postData);

            model.addAttribute(result);

            return Mono.create(monoSink -> monoSink.success(String.format("%s/insert", table)));
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return Mono.create(monoSink -> monoSink.error(e));
        }
    }

    /***
     * update
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.PUT, produces="application/json;charset=UTF-8" )
    public Mono doUpdate(@PathVariable String table, @PathVariable String id,
                         @RequestBody Map<String, Object> postData, final Model model) {
        try {

            Integer result = super.update(table, id, postData);

            model.addAttribute(result);

            return Mono.create(monoSink -> monoSink.success(String.format("%s/update", table)));
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return Mono.create(monoSink -> monoSink.error(e));
        }
    }

    /***
     * delete
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.DELETE, produces="application/json;charset=UTF-8" )
    public Mono doDelete(@PathVariable String table, @PathVariable String id, final Model model) {
        try {

            Integer result = super.delete(table, id);

            model.addAttribute(result);

            return Mono.create(monoSink -> monoSink.success(String.format("%s/delete", table)));
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return Mono.create(monoSink -> monoSink.error(e));
        }
    }


    /***
     * delete
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "batch/{table}", method = RequestMethod.DELETE, produces="application/json;charset=UTF-8" )
    public Mono deleteByIds(@PathVariable String table, @RequestBody List<String> idList, final Model model) {

        List result = null;
        try {
            result = super.deleteByIdList(table, idList);

            model.addAttribute(result);

            return Mono.create(monoSink -> monoSink.success(String.format("%s/delete", table)));
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return Mono.create(monoSink -> monoSink.error(e));
        }
    }

    /***
     * login
     * @return
     */
    @ResponseBody
    @RequestMapping( value = "login", method = RequestMethod.POST, produces="application/json;charset=UTF-8" )
    public Mono loginByPassword(@RequestBody UserModel user, final Model model) {
        try {

            user = super.logoin(user);

            model.addAttribute(user);

            return Mono.create(monoSink -> monoSink.success("login"));

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return Mono.create(monoSink -> monoSink.error(e));
        }
    }

    /***
     * login
     * @return
     */
    @ResponseBody
    @RequestMapping( value = "logout", method = RequestMethod.GET, produces="application/json;charset=UTF-8" )
    public Mono logouted() {

        super.logout();

        return Mono.create(monoSink -> monoSink.success());
    }
}
