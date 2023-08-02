package net.wanji.business.domain.vo;

import lombok.Data;
import net.wanji.business.entity.TjCasePartConfig;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/1 10:03
 * @Descriptoin:
 */
@Data
public class CasePartConfigVo extends TjCasePartConfig {
    /**
     * 参与者名称
     */
    private String name;
    /**
     * 参与者角色名称
     */
    private String participantRoleName;
    /**
     * 参与者类型名称
     */
    private String businessTypeName;
    /**
     * 设备名称
     */
    private String deviceName;

}
