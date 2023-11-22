package net.wanji.web.controller.business;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiOperationSort;
import net.wanji.business.domain.bo.SaveCustomIndexWeightBo;
import net.wanji.business.domain.bo.SaveCustomScenarioWeightBo;
import net.wanji.business.domain.bo.SaveTaskSchemeBo;
import net.wanji.business.domain.bo.TaskBo;
import net.wanji.business.domain.dto.RoutingPlanDto;
import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.dto.device.TaskSaveDto;
import net.wanji.business.domain.vo.PlatformSSDto;
import net.wanji.business.domain.vo.TaskListVo;
import net.wanji.business.entity.TjTask;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.RestService;
import net.wanji.business.service.TestingService;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.service.TjTaskService;
import net.wanji.common.constant.HttpStatus;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: guowenhao
 * @date: 2023/8/30 16:10
 * @description: 测试任务控制器
 */
@Api(tags = "特色测试服务-测试任务")
@RestController
@RequestMapping("/task")
public class TaskController extends BaseController {

    @Autowired
    private TjTaskService tjTaskService;

    @Autowired
    private RestService restService;

    @Autowired
    private TjTaskCaseService taskCaseService;

    @Autowired
    private TestingService testingService;

    @ApiOperationSort(1)
    @ApiOperation(value = "1.节点初始化")
    @GetMapping("/initProcessed")
    @ApiImplicitParam(name = "processNode", value = "节点", required = true, dataType = "Integer", paramType = "query", example = "1")
    public AjaxResult initProcessed(Integer processNode) {
        return AjaxResult.success(tjTaskService.initProcessed(processNode));
    }

    @ApiOperationSort(2)
    @ApiOperation(value = "2.节点信息")
    @PostMapping("/processedInfo")
    public AjaxResult processedInfo(@RequestBody TaskSaveDto taskSaveDto) throws BusinessException {
        return AjaxResult.success(tjTaskService.processedInfo(taskSaveDto));
    }

    @ApiOperationSort(3)
    @ApiOperation(value = "3.保存")
    @PostMapping("/save")
    public AjaxResult save(@Validated @RequestBody TaskBo dto) throws BusinessException {
        int id = tjTaskService.saveTask(dto);
        if (id == 0) {
            return AjaxResult.error("保存失败");
        }
        return AjaxResult.success(id);
    }

    @ApiOperationSort(4)
    @ApiOperation(value = "4.路径优化")
    @PostMapping("/routingPlan")
    public AjaxResult routingPlan(@Validated @RequestBody RoutingPlanDto routingPlanDto) throws BusinessException {
        tjTaskService.routingPlan(routingPlanDto);
        return AjaxResult.success("开始进行路径优规划");
    }

    @ApiOperationSort(5)
    @ApiOperation(value = "5.列表")
    @PostMapping("/pageList")
    public Map<String, Object> pageList(@Validated @RequestBody TaskDto taskDto) throws BusinessException {
        Map<String, Object> result = new HashMap<>();
        Map<String, Long> countMap = tjTaskService.selectCount(taskDto);
        result.put("statistics", countMap);

        PageHelper.startPage(taskDto.getPageNum(), taskDto.getPageSize());
        TableDataInfo tableDataInfo = new TableDataInfo();
        tableDataInfo.setCode(HttpStatus.SUCCESS);
        tableDataInfo.setMsg("查询成功");
        List<TaskListVo> list = tjTaskService.pageList(taskDto);
        tableDataInfo.setData(list);
        tableDataInfo.setTotal(new PageInfo(list).getTotal());
        result.put("tableData", tableDataInfo);
        return result;
    }

    @ApiOperationSort(6)
    @ApiOperation(value = "6.列表页初始化")
    @GetMapping("/initPage")
    public AjaxResult initPage() {
        return AjaxResult.success(tjTaskService.initPage());
    }

    @ApiOperationSort(7)
    @ApiOperation(value = "7.判断是否存在待提交的任务")
    @GetMapping("/hasSubmitTask")
    public AjaxResult hasSubmitTask() {
        TjTask task = tjTaskService.hasUnSubmitTask();
        if(task==null) {
            return AjaxResult.success(0);
        }else {
            return AjaxResult.success(task);
        }
    }

    @ApiOperationSort(8)
    @ApiOperation(value = "8.根据场景权重选择权重详情")
    @GetMapping("/getWeightDetailsById")
    public AjaxResult getWeightDetailsById(String id, Integer type) throws BusinessException {
        if(StringUtils.isEmpty(id)){
            return AjaxResult.error("场景权重id为空!");
        }
        if(type == null){
            return AjaxResult.error("指标或指标类型为空!");
        }
        //0 场景分类方案 1 指标方案
        if(type == 0){
            return AjaxResult.success(restService.getSceneWeightDetailsById(id));
        }else if(type == 1){
            return AjaxResult.success(restService.getIndexWeightDetailsById(id));
        }
        return AjaxResult.success();
    }

    @ApiOperationSort(8)
    @ApiOperation(value = "8.评价指标自定义评价权重")
    @GetMapping("/getValuationIndexCustomWeight")
    public AjaxResult getValuationIndexCustomWeight() throws BusinessException {
        return AjaxResult.success(restService.getValuationIndexCustomWeight());
    }

    @ApiOperationSort(9)
    @ApiOperation(value = "9.创建任务和方案关联")
    @PostMapping("/saveTaskScheme")
    public AjaxResult saveTaskScheme(@RequestBody SaveTaskSchemeBo saveTaskSchemeBo) throws BusinessException {
        Map<String, String> map = restService.saveTaskScheme(saveTaskSchemeBo);
        if ("500".equals(map.get("code"))){
            return AjaxResult.error(map.get("msg"));
        }
//        TaskBo taskBo = new TaskBo();
//        taskBo.setId(Integer.valueOf(saveTaskSchemeBo.getTaskId()));
//        taskBo.setProcessNode(TaskProcessNode.VIEW_PLAN);
//        tjTaskService.saveTask(taskBo);
        return AjaxResult.success("成功");
    }

    @ApiOperationSort(10)
    @ApiOperation(value = "10.自定义-场景权重创建")
    @PostMapping("/saveCustomScenarioWeight")
    public AjaxResult saveCustomScenarioWeight(@RequestBody SaveCustomScenarioWeightBo saveCustomScenarioWeightBo) throws BusinessException {
        Map<String, String> map = restService.saveCustomScenarioWeight(saveCustomScenarioWeightBo);
        if ("500".equals(map.get("code"))){
           return AjaxResult.error(map.get("msg"));
        }
        tjTaskService.saveCustomScenarioWeight(saveCustomScenarioWeightBo);
        return AjaxResult.success();
    }

    @ApiOperationSort(11)
    @ApiOperation(value = "11.自定义-指标权重创建")
    @PostMapping("/saveCustomIndexWeight")
    public AjaxResult saveCustomIndexWeight(@RequestBody SaveCustomIndexWeightBo saveCustomIndexWeightBo) throws BusinessException {
        Map<String, String> map = restService.saveCustomIndexWeight(saveCustomIndexWeightBo);
        if ("500".equals(map.get("code"))){
            return AjaxResult.error(map.get("msg"));
        }
        tjTaskService.saveCustomIndexWeight(saveCustomIndexWeightBo);
        return AjaxResult.success();
    }
    @ApiOperationSort(12)
    @ApiOperation(value = "12.测试任务用例列表")
    @GetMapping("/getTaskCaseList")
    @ApiImplicitParam(name = "taskId", value = "任务id", required = true, dataType = "Integer", paramType = "query", example = "1")
    public AjaxResult getTaskCaseList(@RequestParam("taskId") Integer taskId) throws BusinessException {
        return AjaxResult.success(tjTaskService.getTaskCaseList(taskId));
    }

    @ApiOperationSort(13)
    @ApiOperation(value = "13.下载测试报告")
    @GetMapping("/downloadTestReport")
    public AjaxResult downloadTestReport(HttpServletResponse response) throws BusinessException {
        restService.downloadTestReport(response);
        return AjaxResult.success();
    }


//    @ApiOperationSort(1)
//    @ApiOperation(value = "创建任务")
//    @PostMapping("/create")
//    @ApiImplicitParam(name = "caseIds", value = "用例ID集合", required = true, dataType = "List", paramType = "query", example = "[1,2,3]")
//    public AjaxResult create(@RequestParam("caseIds") List<Integer> caseIds) {
//        TaskVo taskVo = null;
//        try {
//            taskVo = tjTaskService.createTask(caseIds);
//        } catch (BusinessException | ExecutionException | InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        return AjaxResult.success(taskVo);
//    }
//
//    @ApiOperationSort(2)
//    @ApiOperation(value = "任务列表")
//    @GetMapping("/pageList")
//    public TableDataInfo pageList(TaskDto bo) {
//        TableDataInfo tableDataInfo = tjTaskService.pageList(bo);
//        tableDataInfo.setCode(200);
//        tableDataInfo.setMsg("操作成功");
//        return tableDataInfo;
//    }
//
//
    @ApiOperationSort(4)
    @ApiOperation(value = "获取状态")
    @PostMapping("/getStatus")
    public AjaxResult getStatus(@RequestBody TjTaskCase param) throws BusinessException {
        return AjaxResult.success(taskCaseService.getStatus(param));
    }
//
    @ApiOperationSort(5)
    @ApiOperation(value = "准备")
    @PostMapping("/prepare")
    public AjaxResult prepare(@RequestBody TjTaskCase param) throws BusinessException {
        return AjaxResult.success(taskCaseService.prepare(param));
    }
//
    @ApiOperationSort(6)
    @ApiOperation(value = "/controlTask")
    @GetMapping("/controlTask")
    public AjaxResult start(Integer taskId, Integer id, Integer action) throws BusinessException, IOException {
        return AjaxResult.success(taskCaseService.controlTask(taskId, id, action));
    }

    @ApiOperationSort(9)
    @ApiOperation(value = "测试用例开始结束控制接口")
    @PostMapping("/caseStartEnd")
    public AjaxResult caseStartEnd(@RequestBody PlatformSSDto platformSSDto){
        if (platformSSDto.getTaskId()==0) {
            try {
                if(platformSSDto.getState()==1) {
                    testingService.start(platformSSDto.getCaseId(), platformSSDto.getState(), (String) platformSSDto.getContext().get("key"),(String) platformSSDto.getContext().get("username"));
                }else {
                    testingService.end(platformSSDto.getCaseId(), (String) platformSSDto.getContext().get("channel"), platformSSDto.getState());
                }
            } catch (BusinessException | IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                taskCaseService.caseStartEnd(platformSSDto.getTaskId(),
                        platformSSDto.getCaseId(), platformSSDto.getState(),
                        platformSSDto.isTaskEnd(), platformSSDto.getContext());
            } catch (BusinessException | IOException e) {
                throw new RuntimeException(e);
            }
        }
      return null;
    }
//
//    @ApiOperationSort(7)
//    @ApiOperation(value = "回放")
//    @GetMapping("/playback")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "recordId", value = "测试记录ID", required = true, dataType = "Integer", paramType = "query", example = "499"),
//            @ApiImplicitParam(name = "action", value = "动作（1：开始；2：结束）", required = true, dataType = "Integer", paramType = "query", example = "1")
//    })
//    public AjaxResult playback(@RequestParam("recordId") Integer recordId, @RequestParam("action") Integer action)
//            throws BusinessException, IOException {
//        taskCaseService.playback(recordId, action);
//        return AjaxResult.success();
//    }
//
    @ApiOperationSort(8)
    @ApiOperation(value = "测试结果")
    @GetMapping("/getResult")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "任务ID", required = true, dataType = "Integer", paramType = "query", example = "499"),
            @ApiImplicitParam(name = "id", value = "子列表ID", required = false, dataType = "Integer", paramType = "query", example = "499")
    })
    public AjaxResult getResult(Integer taskId, Integer id) throws BusinessException {
        return AjaxResult.success(taskCaseService.getResult(taskId, id));
    }
//
    @ApiOperationSort(9)
    @ApiOperation(value = "图形列表")
    @GetMapping("/communicationDelay")
    @ApiImplicitParam(name = "recordId", value = "测试记录ID", required = true, dataType = "Integer", paramType = "query", example = "499")
    public AjaxResult communicationDelayVo(@RequestParam Integer recordId) {
        return AjaxResult.success(taskCaseService.communicationDelayVo(recordId));
    }
//
//    @ApiOperationSort(10)
//    @ApiOperation(value = "导出")
//    @PostMapping("/report")
//    @ApiImplicitParam(name = "taskId", value = "任务ID", required = true, dataType = "Integer", paramType = "query", example = "499")
//    public void report(HttpServletResponse response, @RequestParam("taskId") Integer taskId) throws IOException {
//        tjTaskService.export(response, taskId);
//    }

    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id){
        return tjTaskService.removeById(id)
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }

    @PostMapping("/remove")
    public AjaxResult removeEnity(){
        LambdaQueryWrapper<TjTask> wrapper = new LambdaQueryWrapper<TjTask>();
        wrapper.eq(TjTask::getStatus, "save");
        return tjTaskService.remove(wrapper)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    @GetMapping("/taskinfo")
    public AjaxResult taskinfo(@RequestParam Integer taskId) throws BusinessException {
        return AjaxResult.success(taskCaseService.gettaskInfo(taskId));
    }

}
