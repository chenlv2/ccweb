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
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;


@RestController
public class WeChatController extends BaseController {

    /***
     * get
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "wechat/callback", method = RequestMethod.GET )
    public ResponseData doGet()  {
        try {
            return success();
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(100, e);
        }
    }
}
