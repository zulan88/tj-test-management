package net.wanji.business.domain.param;

import lombok.Data;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/23 17:43
 * @Descriptoin:
 */
@Data
public class DeviceConnInfo {
    /**
     * 设备id
     */
    private String id;
    /**
     * 控制频道
     */
    private String controlChannel;
    /**
     * 通信频道
     */
    private String channel;
}
