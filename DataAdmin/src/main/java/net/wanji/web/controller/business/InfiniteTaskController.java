package net.wanji.web.controller.business;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.domain.bo.SaveCustomIndexWeightBo;
import net.wanji.business.domain.bo.SaveCustomScenarioWeightBo;
import net.wanji.business.domain.bo.SaveTaskSchemeBo;
import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.vo.task.infinity.InfinityTaskInitVo;
import net.wanji.business.domain.vo.task.infinity.InfinityTaskPreparedVo;
import net.wanji.business.domain.vo.task.infinity.ShardingInOutVo;
import net.wanji.business.entity.TjShardingChangeRecord;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.RestService;
import net.wanji.business.service.TjInfinityTaskService;
import net.wanji.business.service.TjShardingChangeRecordService;
import net.wanji.common.constant.HttpStatus;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hcy
 * @version 1.0
 * @className InfiniteTaskController
 * @description TODO
 * @date 2024/3/11 10:54
 **/
@Api(tags = "特色测试服务-测试任务-无限里程")
@Slf4j
@RestController
@RequestMapping("/taskInfinite")
public class InfiniteTaskController {

    private final RestService restService;

    private final TjInfinityTaskService tjInfinityTaskService;
    private final TjShardingChangeRecordService tjShardingChangeRecordService;

    public InfiniteTaskController(RestService restService,
        TjInfinityTaskService tjInfinityTaskService,
        TjShardingChangeRecordService tjShardingChangeRecordService) {
        this.restService = restService;
        this.tjInfinityTaskService = tjInfinityTaskService;
        this.tjShardingChangeRecordService = tjShardingChangeRecordService;
    }

    // 任务删除

    // 任务更新

    // 任务查询
    @ApiOperationSort(5)
    @ApiOperation(value = "5.列表")
    @PostMapping("/pageList")
    public Map<String, Object> pageList(@Validated @RequestBody TaskDto taskDto, HttpServletRequest request) {
        taskDto.setCreatedBy(SecurityUtils.getUsername());
        Map<String, Object> result = new HashMap<>();
        Map<String, Long> countMap = tjInfinityTaskService.selectCount(taskDto);
        result.put("statistics", countMap);

        PageHelper.startPage(taskDto.getPageNum(), taskDto.getPageSize());
        TableDataInfo tableDataInfo = new TableDataInfo();
        tableDataInfo.setCode(HttpStatus.SUCCESS);
        tableDataInfo.setMsg("查询成功");
        List<Map<String, Object>> list = tjInfinityTaskService.pageList(taskDto);
        tableDataInfo.setData(list);
        tableDataInfo.setTotal(new PageInfo(list).getTotal());
        result.put("tableData", tableDataInfo);
        // 添加测试报告的跳转外链
        result.put("testReportOuterChain", tjInfinityTaskService.getTestReportOuterChain(request));
        return result;
    }


    // 任务新增
    @ApiOperationSort(3)
    @ApiOperation(value = "3-1.保存")
    @PostMapping("/save")
    public AjaxResult save(@Validated @RequestBody Map<String, Object> taskData, HttpServletRequest request) throws BusinessException {
        try {
            taskData.put("createdBy", SecurityUtils.getUsername());
            taskData.put("status", "waiting");
            taskData.put("orderNumber", "task-" + DateUtils.getTime());
            taskData.put("createdDate", DateUtils.getTime());
            String taskId = String.valueOf(tjInfinityTaskService.saveTask(taskData));
            saveEvaluationScheme(taskData, taskId);
            return AjaxResult.success(0);

        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("保存失败");
        }
    }

    @ApiOperationSort(4)
    @ApiOperation(value = "3-2.修改状态")
    @GetMapping("/updateTaskStatus")
    public AjaxResult updateTaskStatus(String status, int id) throws BusinessException {
        try {
            int msg = tjInfinityTaskService.updateTaskStatus(status, id);
            return AjaxResult.success(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("修改状态失败");
        }
    }

    @ApiOperationSort(9)
    @ApiOperation(value = "3-2.创建任务和方案关联")
    @PostMapping("/saveTaskScheme")
    public AjaxResult saveTaskScheme(@RequestBody SaveTaskSchemeBo saveTaskSchemeBo) throws BusinessException {
        Map<String, String> map = restService.saveTaskScheme(saveTaskSchemeBo);
        if ("500".equals(map.get("code"))) {
            return AjaxResult.error(map.get("msg"));
        }
        return AjaxResult.success("成功");
    }

    @ApiOperationSort(10)
    @ApiOperation(value = "3-3.自定义-场景权重创建")
    @PostMapping("/saveCustomScenarioWeight")
    public AjaxResult saveCustomScenarioWeight(@RequestBody SaveCustomScenarioWeightBo saveCustomScenarioWeightBo) throws BusinessException {
        Map<String, String> map = restService.saveCustomScenarioWeight(saveCustomScenarioWeightBo);
        if ("500".equals(map.get("code"))) {
            return AjaxResult.error(map.get("msg"));
        }
        tjInfinityTaskService.saveCustomScenarioWeight(saveCustomScenarioWeightBo);
        return AjaxResult.success();
    }

    @ApiOperationSort(11)
    @ApiOperation(value = "3-4.自定义-指标权重创建")
    @PostMapping("/saveCustomIndexWeight")
    public AjaxResult saveCustomIndexWeight(@RequestBody SaveCustomIndexWeightBo saveCustomIndexWeightBo) throws BusinessException {
        Map<String, String> map = restService.saveCustomIndexWeight(saveCustomIndexWeightBo);
        if ("500".equals(map.get("code"))) {
            return AjaxResult.error(map.get("msg"));
        }
        tjInfinityTaskService.saveCustomIndexWeight(saveCustomIndexWeightBo);
        return AjaxResult.success();
    }

    private void saveEvaluationScheme(Map<String, Object> taskData, String taskId) throws BusinessException {
        // 创建任务和方案关联
        if (taskData.containsKey("taskScheme")) {
            SaveTaskSchemeBo taskScheme = JSONObject.parseObject(JSONObject.toJSONString(
                    taskData.get("taskScheme")), SaveTaskSchemeBo.class);
            taskScheme.setTaskId(taskId);
            saveTaskScheme(taskScheme);
        }

        // 3-3.自定义-场景权重创建
        if (taskData.containsKey("customScenarioWeight")) {
            SaveCustomScenarioWeightBo customScenarioWeight = JSONObject.parseObject(JSONObject.toJSONString(
                    taskData.get("customScenarioWeight")), SaveCustomScenarioWeightBo.class);
            customScenarioWeight.setTask_id(taskId);
            saveCustomScenarioWeight(customScenarioWeight);
        }

        // 3-3.自定义-指标权重创建
        if (taskData.containsKey("customIndexWeight")) {
            SaveCustomIndexWeightBo customIndexWeight = JSONObject.parseObject(JSONObject.toJSONString(
                    taskData.get("customIndexWeight")), SaveCustomIndexWeightBo.class);
            customIndexWeight.setTask_id(taskId);
            saveCustomIndexWeight(customIndexWeight);
        }
    }

    @ApiOperationSort(12)
    @ApiOperation("分片进出通知")
    @PostMapping("/shardingInOut")
    public AjaxResult shardingRangeInOut(
        @RequestBody ShardingInOutVo shardingInOutVo) {
        TjShardingChangeRecord tjShardingChangeRecord = new TjShardingChangeRecord();
        BeanUtils.copyProperties(shardingInOutVo, tjShardingChangeRecord);
        tjShardingChangeRecord.setCreateTime(new Date());
        tjShardingChangeRecordService.saveShardingInOut(tjShardingChangeRecord);
        return AjaxResult.success();
    }

    @ApiOperation("1、初始化")
    @PostMapping("/init")
    @ApiImplicitParam(name = "taskId",
        value = "任务ID",
        required = true,
        dataType = "integer")
    public AjaxResult init(Integer taskId) {
        try {
            InfinityTaskInitVo infinityTaskInitVo = tjInfinityTaskService.init(
                taskId);
            if (CollectionUtils.isEmpty(
                infinityTaskInitVo.getShardingInfos())) {
                throw new BusinessException("分片信息未设置");
            }
            return AjaxResult.success(infinityTaskInitVo);
        } catch (BusinessException be) {
            return AjaxResult.error(be.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("查询任务配置信息异常！");
        }
    }

    @ApiOperation("2、准备（状态检查）")
    @PostMapping("/prepare")
    public AjaxResult prepare(Integer taskId) {
        try {
            return AjaxResult.success(tjInfinityTaskService.prepare(taskId));
        } catch (BusinessException e) {
            InfinityTaskPreparedVo infinityTaskPreparedVo = new InfinityTaskPreparedVo();
            infinityTaskPreparedVo.setCanStart(false);
            return AjaxResult.error("准备状态异常！", infinityTaskPreparedVo);
        }
        // 1、状态检查
            //	1、设备在线状态 #平台定时获取，存在redis
            //	2、获取设备准备状态 #发送至主控
            //	3、获取任务运行状态 #平台自己管理
        // 2、状态校验
            // 1、可测试，发送重置状态
            // 2、返回不可开始状态
    }

    @ApiOperation("3、任务控制-预开始")
    @PostMapping("/preStart")
    public  AjaxResult preStart(Integer taskId) {
        //	1、发送任务轨迹/规则到主控
        try{
            return AjaxResult.success(tjInfinityTaskService.preStart(taskId));
        }catch (Exception e){
            if(log.isErrorEnabled()){
                log.error("preStart error!", e);
            }
        }
        return AjaxResult.error("无限里程开始异常！");
    }

    //4、主控接管任务
        //	1、开始任务
        //	2、结束任务

    @ApiOperationSort(28)
    @ApiOperation(value = "测试评分结果")
    @GetMapping("/getEvaluationResult")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "任务ID", dataType = "Integer", paramType = "query", example = "499"),
            @ApiImplicitParam(name = "id", value = "任务用例ID", dataType = "Integer", paramType = "query", example = "499")
    })
    public AjaxResult getEvaluationResult(Integer taskId, Integer id) throws BusinessException {
        return AjaxResult.success(null);
    }

}
