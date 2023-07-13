package net.wanji.socket.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/11 15:54
 * @Descriptoin:
 */

public class WebSocketManage {

    private static final Logger log = LoggerFactory.getLogger("business");

    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private static int onlineCount = 0;
    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的CumWebSocket对象。
     */
    private static Map<String, WebSocketServer> clients = new ConcurrentHashMap<String, WebSocketServer>();

    public static void join(String id, WebSocketServer socketServer) {
        clients.put(id, socketServer);
        addOnlineCount();
    }

    public static void close(String id) {
        if (!clients.containsKey(id)) {
            log.error("客户端不存在");
            return;
        }
        clients.get(id).onClose();
    }

    public static void remove(String id) {
        if (!clients.containsKey(id)) {
            log.error("客户端不存在");
            return;
        }
        clients.remove(id);
        subOnlineCount();
    }

    public static void sendInfo(String id, String message) {
        if (!clients.containsKey(id)) {
            log.error("客户端不存在");
            return;
        }
        clients.get(id).getSession().getAsyncRemote().sendText(message);
    }

    /**
     * 群发
     *
     * @param message
     */
    private static void sendAll(String message) {
        for (Map.Entry<String, WebSocketServer> client : clients.entrySet()) {
            try {
                client.getValue().getSession().getAsyncRemote().sendText(message);
            } catch (IllegalStateException e) {
                log.error("发送websocket错误",e);
            } catch (Exception e) {
                log.error("发送websocket错误", e);
            }
        }
    }


    /**
     * 减少在线人数
     */
    private static void subOnlineCount() {
        onlineCount--;
        log.info("连接断开。当前在线人数为：" + getOnlineCount());
    }

    /**
     * 添加在线人数
     */
    private static void addOnlineCount() {
        onlineCount++;
        log.info("新连接接入。当前在线人数为：" + getOnlineCount());
    }

    /**
     * 当前在线人数
     *
     * @return
     */
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

}
