package net.wanji.business.service;

import net.wanji.business.domain.bo.SaveCustomIndexWeightBo;
import net.wanji.business.domain.bo.SaveCustomScenarioWeightBo;
import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.vo.CasePageVo;
import net.wanji.business.domain.vo.TaskListVo;
import net.wanji.business.entity.TjInfinityTask;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author hcy
 * @version 1.0
 * @interfaceName TjInfinityTaskService
 * @description TODO
 * @date 2024/3/11 13:11
 **/
@Service
public interface TjInfinityTaskService {


    /**
     * 任务数量统计
     *
     * @return
     */
    Map<String, Long> selectCount(TaskDto taskDto);

    /**
     * 页面列表
     *
     * @param in
     * @return
     */
    List<Map<String, Object>> pageList(TaskDto in);

    /**
     * 查询任务用例
     *
     * @param taskId
     * @return
     */
    List<CasePageVo> getTaskCaseList(Integer taskId);

    /**
     * 测试报告的跳转外链
     *
     * @param request
     * @return
     */
    String getTestReportOuterChain(HttpServletRequest request);

    int saveTask(Map<String, Object> task);


    void saveCustomScenarioWeight(SaveCustomScenarioWeightBo saveCustomScenarioWeightBo);

    void saveCustomIndexWeight(SaveCustomIndexWeightBo saveCustomIndexWeightBo);

    int updateTaskStatus(String status, int id);
}
