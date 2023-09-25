package net.wanji.business.socket;

import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/11 15:54
 * @Descriptoin:
 */

@Validated
public class WebSocketManage {

    private static final Logger log = LoggerFactory.getLogger("business");

    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的CumWebSocket对象。
     */
    private static final ConcurrentHashMap<String, MyWebSocketHandle> CLIENTS = new ConcurrentHashMap<>();

    public static final String SIMULATION = "2";
    public static final String REAL = "3";
    public static final String TASK = "5";

    public static Map<String, String> CLIENT_TYPE = new HashMap<>();

    static {
        CLIENT_TYPE.put(SIMULATION, "仿真验证");
        CLIENT_TYPE.put(REAL, "实车验证");
        CLIENT_TYPE.put(TASK, "任务测试");
    }


    public static String buildKey(String userName, String id, String clientType, String signId) {
        return StringUtils.isEmpty(signId)
                ? StringUtils.format(ContentTemplate.SIMULATION_KEY_TEMPLATE, userName, id, clientType)
                : StringUtils.format(ContentTemplate.REAL_KEY_TEMPLATE, userName, id, clientType, signId);
    }

    public static void join(@NotNull MyWebSocketHandle webSocketHandle) {
        if (!CLIENT_TYPE.containsKey(webSocketHandle.getProperties().getClientType())) {
            log.error(StringUtils.format("客户端类型{}不合法", webSocketHandle.getProperties().getClientType()));
            webSocketHandle.closeSession();
            return;
        }
        String key = webSocketHandle.getProperties().getKey();
        if (CLIENTS.containsKey(key)) {
            log.info(StringUtils.format("{}客户端{}已加入", CLIENT_TYPE.get(webSocketHandle.getProperties().getClientType()), key));
            webSocketHandle.closeSession();
            return;
        }
        CLIENTS.put(key, webSocketHandle);
        log.info(StringUtils.format("客户端{}加入，当前在线数量：{}", key, getOnlineCount()));
    }

    public static void remove(String key) {
        if (!CLIENTS.containsKey(key)) {
            log.error(StringUtils.format("remove:客户端{}不存在", key));
            return;
        }
        CLIENTS.remove(key);
    }

    public static void sendInfo(String key, String message) {
        if (!CLIENTS.containsKey(key)) {
            log.error(StringUtils.format("sendInfo:客户端{}不存在", key));
            return;
        }
        CLIENTS.get(key).sendMessage(message);
    }

    /**
     * 群发
     *
     * @param message
     */
    private static void sendAll(String message) {
        for (Map.Entry<String, MyWebSocketHandle> client : CLIENTS.entrySet()) {
            try {
                client.getValue().sendMessage(message);
            } catch (Exception e) {
                log.error("发送websocket错误",e);
            }
        }
    }

    /**
     * 当前在线人数
     *
     * @return
     */
    public static synchronized int getOnlineCount() {
        return CLIENTS.size();
    }

}
