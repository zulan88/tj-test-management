package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants;
import net.wanji.business.domain.bo.SaveCustomIndexWeightBo;
import net.wanji.business.domain.bo.SaveCustomScenarioWeightBo;
import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.vo.CasePageVo;
import net.wanji.business.entity.TjInfinityTask;
import net.wanji.business.entity.TjTaskDataConfig;
import net.wanji.business.mapper.TjDeviceDetailMapper;
import net.wanji.business.mapper.TjInfinityMapper;
import net.wanji.business.schedule.SceneLabelMap;
import net.wanji.business.service.TjInfinityTaskService;
import net.wanji.business.service.TjTaskDataConfigService;
import net.wanji.common.utils.StringUtils;
import net.wanji.system.service.ISysDictDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hcy
 * @version 1.0
 * @className TjInfinityTaskServiceImpl
 * @description TODO
 * @date 2024/3/11 13:12
 **/
@Service
public class TjInfinityTaskServiceImpl extends ServiceImpl<TjInfinityMapper, TjInfinityTask> implements TjInfinityTaskService {

    @Resource
    private TjInfinityMapper tjInfinityMapper;

    @Value("${tess.testReportOuterChain}")
    private String testReportOuterChain;

    @Autowired
    private TjTaskDataConfigService tjTaskDataConfigService;

    @Override
    public Map<String, Long> selectCount(TaskDto taskDto) {
        List<Map<String, String>> statusMaps = tjInfinityMapper.selectCountByStatus(taskDto);
        Map<String, Long> statusCountMap = CollectionUtils.emptyIfNull(statusMaps).stream().map(t -> t.get("status")).collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        Map<String, Long> result = new HashMap<>();
        for (String status : Constants.TaskStatusEnum.getPageCountList()) {
            result.put(status, statusCountMap.getOrDefault(status, 0L));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> pageList(TaskDto in) {

        List<Map<String, Object>> pageList = tjInfinityMapper.getPageList(in);
        for (Map<String, Object> task : pageList) {
            String id = task.get("id").toString();
            List<TjTaskDataConfig> list = tjTaskDataConfigService.list(new QueryWrapper<TjTaskDataConfig>().eq("task_id", id));
            task.put("avDeviceIds", list);

            // TODO 任务历史记录
            List<Map<String, Object>> historyRecords = new ArrayList<>();
            historyRecords.add(new HashMap<String, Object>() {{
                put("taskStartTime", "2024-03-15 00:00:00");
                put("taskRunningTime", "00:15:24");
                put("record", "1");
            }});

            historyRecords.add(new HashMap<String, Object>() {{
                put("taskStartTime", "2024-03-15 00:00:00");
                put("taskRunningTime", "00:15:24");
                put("record", "2");
            }});

            task.put("historyRecords", historyRecords);
        }


        return pageList;
    }

    @Override
    public List<CasePageVo> getTaskCaseList(Integer taskId) {
        return null;
    }

    @Override
    public String getTestReportOuterChain(HttpServletRequest request) {
        return StringUtils.isEmpty(testReportOuterChain) ? "" : testReportOuterChain;
    }

    @Override
    public int saveTask(Map<String, Object> task) {
        String caseId = task.get("caseId").toString();
        tjInfinityMapper.saveTask(task);
        int id = Integer.parseInt(task.get("id").toString());

        // 保存参与者数据
        List<Map<String, Object>> configList = (List<Map<String, Object>>) task.get("avDeviceIds");
        for (Map<String, Object> configMap : configList) {
            TjTaskDataConfig newAvConfig = new TjTaskDataConfig();
            newAvConfig.setDeviceId(Integer.parseInt(configMap.get("deviceId").toString()));
            newAvConfig.setType(configMap.get("type").toString());
            newAvConfig.setParticipatorId(configMap.get("participatorId").toString());
            newAvConfig.setParticipatorName(configMap.get("participatorName").toString());
            newAvConfig.setTaskId(id);
            newAvConfig.setCaseId(Integer.parseInt(caseId));
            tjTaskDataConfigService.save(newAvConfig);
        }

        return id;
    }


    @Override
    public void saveCustomScenarioWeight(SaveCustomScenarioWeightBo saveCustomScenarioWeightBo) {
        String weights = JSON.toJSONString(saveCustomScenarioWeightBo.getWeights());
        tjInfinityMapper.saveCustomScenarioWeight(saveCustomScenarioWeightBo.getTask_id(), weights, "0");
    }

    @Override
    public void saveCustomIndexWeight(SaveCustomIndexWeightBo saveCustomIndexWeightBo) {
        String weights = JSON.toJSONString(saveCustomIndexWeightBo.getList());
        tjInfinityMapper.saveCustomScenarioWeight(saveCustomIndexWeightBo.getTask_id(), weights, "1");
    }

}
