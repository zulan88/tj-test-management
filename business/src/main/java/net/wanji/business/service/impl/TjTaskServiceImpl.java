package net.wanji.business.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.domain.bo.TaskBo;
import net.wanji.business.domain.dto.CreateTaskDto;
import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.vo.TaskCaseVo;
import net.wanji.business.domain.vo.TaskListVo;
import net.wanji.business.domain.vo.TaskVo;
import net.wanji.business.entity.TjTask;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.entity.TjTaskDataConfig;
import net.wanji.business.entity.TjTaskDc;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjTaskCaseMapper;
import net.wanji.business.mapper.TjTaskDataConfigMapper;
import net.wanji.business.mapper.TjTaskDcMapper;
import net.wanji.business.mapper.TjTaskMapper;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjTaskService;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
* @author guowenhao
* @description 针对表【tj_task(测试任务表)】的数据库操作Service实现
* @createDate 2023-08-31 17:39:16
*/
@Service
public class TjTaskServiceImpl extends ServiceImpl<TjTaskMapper, TjTask>
    implements TjTaskService{

    @Autowired
    private TjTaskMapper tjTaskMapper;

    @Autowired
    private TjTaskCaseMapper tjTaskCaseMapper;

    @Autowired
    private TjTaskDataConfigMapper tjTaskDataConfigMapper;

    @Autowired
    private TjTaskDcMapper tjTaskDcMapper;

    @Autowired
    private TjCaseService tjCaseService;

    private static final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");

    @Override
    public TableDataInfo pageList(TaskBo in) {
        TableDataInfo tableDataInfo = new TableDataInfo();
        QueryWrapper<TjTask> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(in.getTaskName()))
            wrapper.eq("task_name", in.getTaskName());
        if (ObjectUtil.isNotEmpty(in.getStartTime()))
            wrapper.gt("start_time", in.getStartTime());
        if (ObjectUtil.isNotEmpty(in.getEndTime()))
            wrapper.lt("end_time", in.getEndTime());
        Page<TjTask> tjTaskPage = tjTaskMapper.selectPage(
            new Page<>(in.getPageNum(), in.getPageSize()), wrapper);
        tableDataInfo.setTotal(tjTaskPage.getTotal());
        List<TaskListVo> taskListVos = new ArrayList<>();
        for (TjTask record : tjTaskPage.getRecords()) {
            TaskListVo taskListVo = new TaskListVo();
            TjTaskDataConfig tjTaskDataConfig = tjTaskDataConfigMapper.selectOne(
                new QueryWrapper<TjTaskDataConfig>().eq("task_id",
                    record.getId()).eq("type", "av"));
            List<TaskCaseVo> taskCaseVos = tjTaskCaseMapper.gstList(record.getId());
            int num = 0;
            for (TaskCaseVo taskCaseVo : taskCaseVos) {
                taskCaseVo.setMainCarName(tjTaskDataConfig.getParticipatorName());
                if (taskCaseVo.getStatus().equals("待测试"))
                    num++;
            }
            BeanUtils.copyBeanProp(taskListVo, record);
            if (num == 0)
                num = taskCaseVos.size();
            taskListVo.setStatus(num + "/" + taskCaseVos.size());
            taskListVo.setMainCarName(tjTaskDataConfig.getParticipatorName());
            taskListVo.setTaskCaseVos(taskCaseVos);
            taskListVos.add(taskListVo);
        }
        tableDataInfo.setData(taskListVos);
        return tableDataInfo;
    }

    @Override
    public TaskVo createTask(CreateTaskDto in)
        throws BusinessException, ExecutionException, InterruptedException {
        TaskVo taskVo = new TaskVo();
        String[] split = in.getCaseIds().split(",");
        taskVo.setCaseCount(in.getCaseIds().split(",").length);
        taskVo.setTaskName("task-" + sf.format(new Date()));
        taskVo.setCaseIds(in.getCaseIds());
        taskVo.setDataConfigs(tjCaseService.getTaskConfigDetail(Integer.parseInt(split[0])));
        return taskVo;
    }

    @Override
    @Transactional
    public int saveTask(TaskDto in) {
        String[] cases = in.getCaseIds().split(",");
        TjTask tjTask = new TjTask();
        tjTask.setTaskName(in.getTaskName());
        tjTask.setCaseCount(cases.length);
        tjTask.setCreateTime(new Date());
        tjTaskMapper.insert(tjTask);
        for (String aCase : cases) {
            TjTaskCase tjTaskCase = new TjTaskCase();
            tjTaskCase.setTaskId(tjTask.getId());
            tjTaskCase.setCaseId(Integer.parseInt(aCase));
            tjTaskCase.setCreateTime(new Date());
            tjTaskCase.setStatus("待测试");
            tjTaskCaseMapper.insert(tjTaskCase);
        }
        List<TjTaskDataConfig> dataConfigs = in.getDataConfigs();
        for (TjTaskDataConfig dataConfig : dataConfigs) {
            dataConfig.setTaskId(tjTask.getId());
            tjTaskDataConfigMapper.insert(dataConfig);
        }
        List<TjTaskDc> diadynamicCriterias = in.getDiadynamicCriterias();
        for (TjTaskDc diadynamicCriteria : diadynamicCriterias) {
            diadynamicCriteria.setTaskId(tjTask.getId());
            tjTaskDcMapper.insert(diadynamicCriteria);
        }
        return 1;
    }
}




