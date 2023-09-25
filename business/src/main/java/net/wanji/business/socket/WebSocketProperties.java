package net.wanji.business.socket;

import java.io.IOException;

/**
 * @Auther: guanyuduo
 * @Date: 2023/9/19 14:36
 * @Descriptoin:
 */

public class WebSocketProperties {
    private final String userName;
    private final String token;
    private final String id;
    private final String clientType;
    private final String signId;
    private final String key;
    private final long createTime;

    public WebSocketProperties(String userName, String token, String id, String clientType,
                               String signId, long createTime) {
        this.token = token;
        this.userName = userName;
        this.id = id;
        this.clientType = clientType;
        this.signId = signId;
        this.key = WebSocketManage.buildKey(userName, id, clientType, signId);
        this.createTime = createTime;
    }

    public String getUserName() {
        return userName;
    }

    public String getToken() {
        return token;
    }

    public String getId() {
        return id;
    }

    public String getClientType() {
        return clientType;
    }

    public String getsignId() {
        return signId;
    }

    public String getKey() {
        return key;
    }

    public long getCreateTime() {
        return createTime;
    }
}
