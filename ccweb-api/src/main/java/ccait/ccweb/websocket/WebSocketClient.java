/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.websocket;


import ccait.ccweb.utils.FastJsonUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static ccait.ccweb.context.ApplicationContext.LOG_PRE_SUFFIX;

@Component
@ClientEndpoint
public class WebSocketClient extends Endpoint {
    private static final Logger log = LogManager.getLogger(WebSocketClient.class);

    // 获取WebSocket连接器
    WebSocketContainer container;
    //连接超时
    public static final long MAX_TIME_OUT = 60 * 60 * 24 * 1000;

    @PostConstruct
    private void init() {
        container = ContainerProvider.getWebSocketContainer();
    }

    @Value("${websocket.url:}")
    private String websocket_url;

    @OnOpen
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        log.debug(LOG_PRE_SUFFIX + "连接成功");
        session.setMaxIdleTimeout(MAX_TIME_OUT);
        session.addMessageHandler(new MessageHandler.Whole<String>() {

            /** * 有返回信息时触发 * */
            @OnMessage
            @Override
            public void onMessage(String message) {
                log.debug(LOG_PRE_SUFFIX + "返回信息：" + message);
            }
        });
    }

    @OnError
    @Override
    public void onError(Session session, Throwable t) {
        log.error(LOG_PRE_SUFFIX + "失败：" + t.getMessage(), t);
    }

    @OnClose
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        log.warn(LOG_PRE_SUFFIX + "Websocket连接已关闭......");
    }

    public <T> void send(T data) {

        String message = "";
        if(data instanceof String) {
            message = data.toString();
        }

        else {
            message = FastJsonUtils.convertObjectToJSON(data);
        }

        Session session = null;
        try {
            session = connect(data);
            session.getBasicRemote().sendText(message);// 发送信息
        } catch (Exception e) {
            log.error(LOG_PRE_SUFFIX + String.format("WebSocket(%s)创建连接出错：%s", websocket_url, e.getMessage()), e);
        }
    }

    private <T> Session connect(T data) throws DeploymentException, IOException, URISyntaxException {
        Session session;
        ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().build();
        clientEndpointConfig.getUserProperties().put("data", data);

        if(StringUtils.isEmpty(websocket_url)) {
            throw new IOException("connection has been released!!!");
        }

        // 创建会话
        session = container.connectToServer(WebSocketClient.class, clientEndpointConfig, new URI(websocket_url));
        return session;
    }
}
