package net.wanji.business.socket;

import net.wanji.business.common.Constants.ChannelBuilder;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;

/**
 * @Auther: guanyuduo
 * @Date: 2023/9/18 17:51
 * @Descriptoin:
 */
@Component
public class MyWebSocketHandle extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger("business");

    /**
     * socket 建立成功事件 @OnOpen
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userName = (String) session.getAttributes().get("userName");
        String token = (String) session.getAttributes().get("token");
        String id = (String) session.getAttributes().get("id");
        Integer clientType = Integer.parseInt((String) session.getAttributes().get("clientType"));
        String signId = (String) session.getAttributes().get("signId");
        long createTime = (long) session.getAttributes().get("createTime");
        if (ObjectUtils.isEmpty(id)) {
            session.close();
            return;
        }
        WebSocketManage.join(new WebSocketProperties(userName, token, id, clientType, signId,
                buildKey(userName, id, clientType, signId), createTime, session));
    }

    /**
     * 接收消息事件 @OnMessage
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 获得客户端传来的消息
        String payload = message.getPayload();
        System.out.println("server 接收到发送的 " + payload);
        session.sendMessage(new TextMessage("server 发送消息 " + payload + " " + LocalDateTime.now()));
    }

    /**
     * socket 断开连接时 @OnClose
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("断开连接 ");
        String userName = (String) session.getAttributes().get("userName");
        String id = (String) session.getAttributes().get("id");
        Integer clientType = Integer.parseInt((String) session.getAttributes().get("clientType")) ;
        String signId = (String) session.getAttributes().get("signId");
        WebSocketManage.remove(buildKey(userName, id, clientType, signId));
    }

    private String buildKey(String userName, String id, int clientType, String signId) throws BusinessException {
        if (ChannelBuilder.SCENE_PREVIEW == clientType) {
            return ChannelBuilder.buildScenePreviewChannel(userName, Integer.valueOf(id));
        }
        if (ChannelBuilder.SIMULATION == clientType) {
            return ChannelBuilder.buildSimulationChannel(userName, id);
        }
        if (ChannelBuilder.REAL == clientType) {
            return ChannelBuilder.buildTestingDataChannel(userName, Integer.valueOf(id));
        }
        if (ChannelBuilder.PLAN == clientType) {
            return ChannelBuilder.buildRoutingPlanChannel(userName, Integer.valueOf(id));
        }
        if (ChannelBuilder.TASK == clientType) {
            return ChannelBuilder.buildTaskDataChannel(userName, Integer.valueOf(id));
        }
        if (ChannelBuilder.TASK_PREVIEW == clientType) {
            return ChannelBuilder.buildTaskPreviewChannel(userName, Integer.valueOf(id));
        }
        if (ChannelBuilder.TESTING_PREVIEW == clientType) {
            return ChannelBuilder.buildTestingPreviewChannel(userName, Integer.valueOf(id));
        }
        throw new BusinessException("无法创建ws连接：客户端类型异常");
    }
}
