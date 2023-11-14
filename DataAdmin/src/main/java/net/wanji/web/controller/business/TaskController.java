package net.wanji.web.controller.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiOperationSort;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TaskBo;
import net.wanji.business.domain.dto.RoutingPlanDto;
import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.dto.device.TaskSaveDto;
import net.wanji.business.domain.vo.CaseContinuousVo;
import net.wanji.business.domain.vo.TaskListVo;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.service.TjTaskService;
import net.wanji.common.constant.HttpStatus;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private TjTaskCaseService taskCaseService;

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
        return AjaxResult.success(tjTaskService.hasUnSubmitTask());
    }

    public static void main(String[] args) {
        String a = "[{\"latitude\":\"31.291317348001133\",\"longitude\":\"121.20177044909862\"},{\"latitude\":\"31.291426346998925\",\"longitude\":\"121.20192271008506\"},{\"latitude\":\"31.291544968845898\",\"longitude\":\"121.20207388346185\"},{\"latitude\":\"31.291595159998042\",\"longitude\":\"121.2021781919333\"},{\"latitude\":\"31.291592241003322\",\"longitude\":\"121.20223324403736\"},{\"latitude\":\"31.29160603799389\",\"longitude\":\"121.20230376101958\"},{\"latitude\":\"31.29158416899701\",\"longitude\":\"121.20234568806515\"},{\"latitude\":\"31.29136515999998\",\"longitude\":\"121.20255562197296\"},{\"latitude\":\"31.291350074681237\",\"longitude\":\"121.20261943133603\"},{\"latitude\":\"31.291400301001538\",\"longitude\":\"121.2026167470535\"},{\"latitude\":\"31.291632041993555\",\"longitude\":\"121.20240607693178\"},{\"latitude\":\"31.291672736826516\",\"longitude\":\"121.202398167056\"},{\"latitude\":\"31.291765908007974\",\"longitude\":\"121.20238939005648\"},{\"latitude\":\"31.291907268992738\",\"longitude\":\"121.20253326196028\"},{\"latitude\":\"31.292439300961398\",\"longitude\":\"121.2032275279761\"}]";

        System.out.println(JSON.toJSONString(a));
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
//    @ApiOperationSort(4)
//    @ApiOperation(value = "获取状态")
//    @GetMapping("/getStatus")
//    @ApiImplicitParam(name = "taskCaseId", value = "任务用例ID", required = true, dataType = "Integer", paramType = "query", example = "278")
//    public AjaxResult getStatus(@RequestParam("taskCaseId") Integer taskCaseId) throws BusinessException {
//        return AjaxResult.success(taskCaseService.getStatus(taskCaseId));
//    }
//
//    @ApiOperationSort(5)
//    @ApiOperation(value = "准备")
//    @GetMapping("/prepare")
//    @ApiImplicitParam(name = "taskCaseId", value = "任务用例ID", required = true, dataType = "Integer", paramType = "query", example = "278")
//    public AjaxResult prepare(@RequestParam("taskCaseId") Integer taskCaseId) throws BusinessException {
//        return AjaxResult.success(taskCaseService.prepare(taskCaseId));
//    }
//
//    @ApiOperationSort(6)
//    @ApiOperation(value = "开始")
//    @GetMapping("/start")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "recordId", value = "测试记录ID", required = true, dataType = "Integer", paramType = "query", example = "499"),
//            @ApiImplicitParam(name = "action", value = "动作（1：开始；2：结束）", required = true, dataType = "Integer", paramType = "query", example = "1")
//    })
//    public AjaxResult start(@RequestParam("recordId") Integer recordId,
//                            @RequestParam("action") Integer action) throws BusinessException, IOException {
//        return AjaxResult.success(taskCaseService.start(recordId, action));
//    }
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
//    @ApiOperationSort(8)
//    @ApiOperation(value = "测试结果")
//    @GetMapping("/getResult")
//    @ApiImplicitParam(name = "recordId", value = "测试记录ID", required = true, dataType = "Integer", paramType = "query", example = "499")
//    public AjaxResult getResult(@RequestParam("recordId") Integer recordId) throws BusinessException {
//        return AjaxResult.success(taskCaseService.getResult(recordId));
//    }
//
//    @ApiOperationSort(9)
//    @ApiOperation(value = "图形列表")
//    @GetMapping("/communicationDelay")
//    @ApiImplicitParam(name = "recordId", value = "测试记录ID", required = true, dataType = "Integer", paramType = "query", example = "499")
//    public AjaxResult communicationDelayVo(@RequestParam Integer recordId) {
//        return AjaxResult.success(taskCaseService.communicationDelayVo(recordId));
//    }
//
//    @ApiOperationSort(10)
//    @ApiOperation(value = "导出")
//    @PostMapping("/report")
//    @ApiImplicitParam(name = "taskId", value = "任务ID", required = true, dataType = "Integer", paramType = "query", example = "499")
//    public void report(HttpServletResponse response, @RequestParam("taskId") Integer taskId) throws IOException {
//        tjTaskService.export(response, taskId);
//    }
}
