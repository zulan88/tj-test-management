package net.wanji.business.service;

import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.TaskCaseConfigBo;
import net.wanji.business.domain.dto.device.DeviceReadyStateDto;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.TestStartParam;
import net.wanji.business.domain.vo.CaseContinuousVo;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/9 15:25
 * @Descriptoin:
 */

public interface RestService {

    /**
     * 开始仿真
     */
    boolean start(TestStartParam startParam);

    /**
     * 多场景路径规划
     */
    boolean startRoutingPlan(List<CaseContinuousVo> caseContinuousVos);

    /**
     * 查询设备信息
     * @param ip
     * @param method
     * @return
     */
    Map<String, Object> searchDeviceInfo(String ip, HttpMethod method);

    /**
     * 查询设备准备状态
     * @param deviceReadyStateParam
     * @return
     */
    boolean selectDeviceReadyState(DeviceReadyStateParam deviceReadyStateParam);

    /**
     * 主控交互
     * @param caseRuleControl
     * @return
     */
    boolean sendRuleUrl(CaseRuleControl caseRuleControl);

    Object imitateClientUrl(List<CaseConfigBo> param);

    Object taskClientUrl(List<TaskCaseConfigBo> param);
}
