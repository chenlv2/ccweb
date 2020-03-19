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
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.websocket.MessageBody;
import ccait.ccweb.websocket.WebSocketClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;


@RestController
public class WebsocketController {

    private static final Logger log = LogManager.getLogger(WebsocketController.class);

    @Autowired
    private WebSocketClient wsClient;

    /***
     * send message
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "api/message/send", method = RequestMethod.POST )
    public ResponseData sendMessage(@RequestBody MessageBody messageBody) {

        try {
            wsClient.send(messageBody);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseData(500, e.getMessage());
        }

        return new ResponseData();
    }


    /***
     * send message for async
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "asyncapi/message/send", method = RequestMethod.POST )
    public Mono<ResponseData> asyncSendMessage(@RequestBody MessageBody messageBody) {

        return Mono.just(this.sendMessage(messageBody));
    }
}
