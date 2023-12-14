package net.wanji.business.domain.param;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import net.wanji.business.common.Constants.ChannelBuilder;

import java.util.Map;

/**
 * @author: guanyuduo
 * @date: 2023/12/7 15:23
 * @descriptoin: 与tess交互的参数
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
public class TessParam {

    /**
     * 请求类型
     * 2：仿真验证；3：实车试验；4：路径规划；5：多场景试验
     */
    private Integer simulateType;

    /**
     * 路网编号（simulateType = 2,3,4,5）
     */
    private Integer roadNum;

    /**
     * 数据通道
     */
    private String dataChannel;

    /**
     * 控制通道（simulateType = 3,5）
     */
    private String commandChannel;

    /**
     * 评价数据通道
     */
    private String evaluateChannel;

    /**
     * 路径规划通道simulateType = 4
     */
    private String routingChannel;

    /**
     * 状态通道（simulateType = 3,5）
     */
    private String statusChannel;

    /**
     * 自定义参数（simulateType = 2,4）
     */
    private Object params;

    public TessParam() {
    }

    public TessParam buildSimulationParam(Integer roadNum, String dataChannel, Object params) {
        this.simulateType = ChannelBuilder.SIMULATION;
        this.roadNum = roadNum;
        this.dataChannel = dataChannel;
        this.params = params;

        this.commandChannel = "1";
        this.statusChannel = "1";
        this.routingChannel = "1";
        this.evaluateChannel = "1";

        return this;
    }

    public TessParam buildRealTestParam(Integer roadNum, String dataChannel, String commandChannel, String evaluateChannel,
                                   String statusChannel) {
        this.simulateType = ChannelBuilder.REAL;
        this.roadNum = roadNum;
        this.dataChannel = dataChannel;
        this.commandChannel = commandChannel;
        this.evaluateChannel = evaluateChannel;
        this.statusChannel = statusChannel;

        this.routingChannel = "1";
        this.params = JSONObject.parseObject("{\"params\":{\"params\": []}}");
        return this;
    }

    public TessParam buildRoutingPlanParam(Integer roadNum, String routingChannel, Object params) {
        this.simulateType = ChannelBuilder.PLAN;
        this.roadNum = roadNum;
        this.routingChannel = routingChannel;
        this.params = params;

        this.dataChannel = "1";
        this.commandChannel = "1";
        this.evaluateChannel = "1";
        this.statusChannel = "1";
        return this;
    }

    public TessParam buildTaskParam(Integer roadNum, String dataChannel, String commandChannel, String evaluateChannel,
                                    String statusChannel) {
        this.simulateType = ChannelBuilder.TASK;
        this.roadNum = roadNum;
        this.dataChannel = dataChannel;
        this.commandChannel = commandChannel;
        this.evaluateChannel = evaluateChannel;
        this.statusChannel = statusChannel;

        this.routingChannel = "1";
        this.params = JSONObject.parseObject("{\"params\":{\"params\": []}}");
        return this;
    }
}
