package net.wanji.business.domain.param;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/23 17:49
 * @Descriptoin:
 */
@AllArgsConstructor
@Data
public class CaseRuleControl {
    /**
     * 时间戳ms
     */
    private Long timestamp;
    /**
     * 任务id
     */
    private String id;
    /**
     * 任务类型
     */
    private Integer taskType;
    /**
     * 规则
     */
    private List<DeviceConnRule> rules;
}
