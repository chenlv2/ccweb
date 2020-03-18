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
import ccait.ccweb.websocket.MessageBody;
import ccait.ccweb.websocket.WebSocketClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;


@RestController
public class WebsocketController {

    private static final Logger log = LogManager.getLogger(WebsocketController.class);

    /***
     * send message
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "api/message/send", method = RequestMethod.POST )
    public void sendMessage(@RequestBody MessageBody messageBody) {

        try {
            WebSocketClient ws = new WebSocketClient();
            ws.send(messageBody);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    /***
     * send message for async
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "asyncapi/message/send", method = RequestMethod.POST )
    public Mono asyncSendMessage(@RequestBody MessageBody messageBody) {

        try {
            this.sendMessage(messageBody);

        } catch (Exception e) {
            log.error(LOG_PRE_SUFFIX + e.getMessage(), e);
        }

        return Mono.empty();
    }
}
