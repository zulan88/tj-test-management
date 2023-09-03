package net.wanji.business.service;

import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.TaskCaseConfigBo;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.TestStartParam;
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
     * 查询设备信息
     * @param ip
     * @param method
     * @return
     */
    Map<String, Object> searchDeviceInfo(String ip, HttpMethod method);

    /**
     * 主控交互
     * @param caseRuleControl
     * @return
     */
    Object sendRuleUrl(CaseRuleControl caseRuleControl);

    Object imitateClientUrl(List<CaseConfigBo> param);

    Object taskClientUrl(List<TaskCaseConfigBo> param);
}
