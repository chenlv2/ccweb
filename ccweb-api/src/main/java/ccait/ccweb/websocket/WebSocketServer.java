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

import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.FastJsonUtils;
import entity.tool.util.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.StaticVars.*;


@Component
@ServerEndpoint(value="/ws")
public class WebSocketServer {

    private static final Logger log = LogManager.getLogger(WebSocketServer.class);
    private static final AtomicInteger OnlineCount = new AtomicInteger(0);
    private final static Hashtable<String, Session> sessionSet = new Hashtable<String, Session>();
    private final static Hashtable<String, UserModel> sessionIdUserMap = new Hashtable<String, UserModel>();

    //连接超时
    public static final long MAX_TIME_OUT = 60 * 60 * 24 * 1000;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig conf) {
        session.setMaxIdleTimeout( MAX_TIME_OUT );

        HandshakeRequest req = (HandshakeRequest) conf.getUserProperties().get("sessionKey");
        HttpSession httpSession = (HttpSession) req.getHttpSession();

        sessionSet.put(httpSession.getId(), session);

        if(httpSession.getAttribute( httpSession.getId() + LOGIN_KEY ) != null) {
            UserModel user = (UserModel) httpSession.getAttribute(httpSession.getId() + LOGIN_KEY);

            sessionIdUserMap.put(httpSession.getId(), user);
        }

        int cnt = OnlineCount.incrementAndGet(); // 在线数加1
        log.info(LOG_PRE_SUFFIX + String.format("有连接加入，当前连接数为：%s", cnt));
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) {

        try {
            List<String> ids = sessionSet.entrySet().stream()
                    .filter(a->a.getValue().getId().equals(session.getId()))
                    .map(b->b.getKey()).collect(Collectors.toList());

            if(ids != null) {
                for (String id :ids) {
                    sessionIdUserMap.remove(id);
                    sessionSet.remove(id);
                }
            }
        }
        catch (Exception e){
            log.error(e);
        }

        int cnt = OnlineCount.decrementAndGet();
        log.info(LOG_PRE_SUFFIX + String.format("有连接关闭，当前连接数为：%s", cnt));
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message
     *            客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info(LOG_PRE_SUFFIX + String.format("来自客户端的消息：%s", message));
        try
        {
            if(StringUtils.isEmpty( message )) {
                throw new Exception("消息不能为空");
            }

            MessageBody body = FastJsonUtils.convertJsonToObject(message, MessageBody.class);
            if(body != null) {
                switch (body.getSendMode()) {
                    case ALL:
                        sendToAll(message);
                        break;
                    case USER:
                        sendToUser(body.getReceiver(), message);
                        break;
                    case GROUP:
                        sendToGroup(body.getReceiver(), message);
                        break;
                    case ROLE:
                        sendToRole(body.getReceiver(), message);
                        break;
                }
                return;
            }

            throw new Exception("无效的消息格式");

        } catch ( Exception e )
        {
            log.error(LOG_PRE_SUFFIX + String.format("接收消息发生错误：%s",e.getMessage()));

            sendMessage(session, e.getMessage());
        }
    }

    private void sendToRole(ReceiverInfo receiver, String message) {

        List<String> ids = sessionIdUserMap.entrySet().stream()
                .filter(a -> {
                    try {
                        return a.getValue().getUserGroupRoleModels().stream()
                                .filter(b -> b.getGroupId().equals(receiver.getGroupId()) &&
                                    b.getRoleId().equals(receiver.getRoleId())).isParallel();
                    } catch (SQLException e) {
                        log.error(e);
                    }

                    return false;
                }).map(c->c.getKey()).collect(Collectors.toList());

        List<Session> list = sessionSet.entrySet().stream()
                .filter(a->ids.contains(a.getKey())).map(b->b.getValue())
                .collect(Collectors.toList());

        for (Session session : list) {
            sendMessage(session, message);
        }
    }

    private void sendToGroup(ReceiverInfo receiver, String message) {

        List<String> ids = sessionIdUserMap.entrySet().stream()
                .filter(a -> {
                    try {
                        return a.getValue().getUserGroupRoleModels().stream().filter(b -> b.getGroupId().equals(receiver.getGroupId())).isParallel();
                    } catch (SQLException e) {
                        log.error(e);
                    }

                    return false;
                }).map(c->c.getKey()).collect(Collectors.toList());

        List<Session> list = sessionSet.entrySet().stream()
                .filter(a->ids.contains(a.getKey())).map(b->b.getValue())
                .collect(Collectors.toList());

        for (Session session : list) {
            sendMessage(session, message);
        }
    }

    private void sendToUser(ReceiverInfo receiver, String message) {

        List<String> ids = sessionIdUserMap.entrySet().stream()
                .filter(a->receiver.getUsernames().contains(a.getValue().getUsername()))
                .map(b->b.getKey()).collect(Collectors.toList());

        List<Session> list = sessionSet.entrySet().stream()
                .filter(a->ids.contains(a.getKey())).map(b->b.getValue())
                .collect(Collectors.toList());

        for (Session session : list) {
            sendMessage(session, message);
        }
    }

    /**
     * 出现错误
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error(LOG_PRE_SUFFIX + String.format("发生错误：%s，Session ID： %s",error.getMessage(),session.getId()));
    }

    /**
     * 发送消息，实践表明，每次浏览器刷新，session会发生变化。
     * @param session
     * @param message
     */
    public static void sendMessage(Session session, String message) {
        try {
            if(session == null) {
                return;
            }

            if(!session.isOpen()) {
                return;
            }

            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            log.error(LOG_PRE_SUFFIX + String.format("发送消息出错：%s, 消息内容：%s", e.getMessage(), message));
            e.printStackTrace();
        }
    }

    /**
     * 群发消息
     * @param message
     * @throws IOException
     */
    public static void sendToAll(String message) throws Exception {
        for (Entry<String, Session> session : sessionSet.entrySet()) {
            sendMessage(session.getValue(), message);
        }
    }

    /**
     * 指定Session发送消息
     * @param httpSessionId
     * @param message
     * @throws IOException
     */
    public static void sendMessage(String httpSessionId,String message) throws Exception {
        Session session = sessionSet.get( httpSessionId );
        if(session!=null){
            sendMessage(session, message);
        }
        else{
            log.warn(LOG_PRE_SUFFIX + String.format("没有找到你指定ID的会话：%s",httpSessionId));
        }
    }
}
