package net.wanji.business.service;

import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.SaveCustomIndexWeightBo;
import net.wanji.business.domain.bo.SaveCustomScenarioWeightBo;
import net.wanji.business.domain.bo.SaveTaskSchemeBo;
import net.wanji.business.domain.bo.TaskCaseConfigBo;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.dto.device.TaskSaveDto;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.CaseTrajectoryParam;
import net.wanji.business.domain.param.TestStartParam;
import net.wanji.business.domain.vo.CaseContinuousVo;
import net.wanji.business.domain.vo.IndexCustomWeightVo;
import net.wanji.business.domain.vo.IndexWeightDetailsVo;
import net.wanji.business.domain.vo.SceneIndexSchemeVo;
import net.wanji.business.domain.vo.SceneWeightDetailsVo;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletResponse;
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
    boolean start(String ip, Integer port, TestStartParam startParam);

    /**
     * 多场景路径规划
     */
    boolean startRoutingPlan(String ip, Integer port, List<CaseContinuousVo> caseContinuousVos);

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

    /**
     * 向主控发送用例轨迹信息
     * @param param
     * @return
     */
    boolean sendCaseTrajectoryInfo(CaseTrajectoryParam param);

    /**
     * 获取济达场景&指标方案列表
     * @param taskSaveDto
     * @return {@link java.util.List<net.wanji.business.domain.vo.SceneIndexSchemeVo>}
     * @author liruitao
     * @date 2023-11-14
     */
    List<SceneIndexSchemeVo> getSceneIndexSchemeList(TaskSaveDto taskSaveDto);

    /**
     * 获取场景权重详情
     * @param id
     * @return {@link java.lang.String}
     * @author liruitao
     * @date 2023-11-14
     */
    List<SceneWeightDetailsVo> getSceneWeightDetailsById(String id);

    /**
     * 获取指标权重详情
     * @param id
     * @return {@link String}
     * @author liruitao
     * @date 2023-11-14
     */
    List<IndexWeightDetailsVo> getIndexWeightDetailsById(String id);

    /**
     * 获取自定义指标权重详情
     * @param
     * @return {@link List<IndexCustomWeightVo>}
     * @author liruitao
     * @date 2023-11-14
     */
    List<IndexCustomWeightVo> getValuationIndexCustomWeight();

    /**
     * 创建任务和方案关联
     * @param saveTaskSchemeBo
     * @return {@link boolean}
     * @author liruitao
     * @date 2023-11-15
     */
    Map<String, String> saveTaskScheme(SaveTaskSchemeBo saveTaskSchemeBo);

    /**
     * 自定义-场景权重创建
     * @param saveCustomScenarioWeightBo
     * @return {@link boolean}
     * @author liruitao
     * @date 2023-11-15
     */
    Map<String, String> saveCustomScenarioWeight(SaveCustomScenarioWeightBo saveCustomScenarioWeightBo);

    /**
     * 自定义-指标权重创建
     * @param saveCustomIndexWeightBo
     * @return {@link String}
     * @author liruitao
     * @date 2023-11-16
     */
    Map<String, String> saveCustomIndexWeight(SaveCustomIndexWeightBo saveCustomIndexWeightBo);

    /**
     * 下载测试报告
     * @return {@link String}
     * @author liruitao
     * @date 2023-11-22
     */
    void downloadTestReport(HttpServletResponse response);
}
