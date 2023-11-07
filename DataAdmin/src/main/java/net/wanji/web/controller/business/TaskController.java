package net.wanji.web.controller.business;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiOperationSort;
import net.wanji.business.service.TjTaskCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.bo.TaskBo;
import net.wanji.business.domain.vo.TaskVo;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjTaskService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;

import javax.servlet.http.HttpServletResponse;

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
    @ApiOperation(value = "创建任务")
    @PostMapping("/create")
    @ApiImplicitParam(name = "caseIds", value = "用例ID集合", required = true, dataType = "List", paramType = "query", example = "[1,2,3]")
    public AjaxResult create(@RequestParam("caseIds") List<Integer> caseIds)
    {
        TaskVo taskVo = null;
        try {
            taskVo = tjTaskService.createTask(caseIds);
        } catch (BusinessException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return AjaxResult.success(taskVo);
    }

    @ApiOperationSort(2)
    @ApiOperation(value = "任务列表")
    @GetMapping("/pageList")
    public TableDataInfo pageList(TaskDto bo)
    {
        TableDataInfo tableDataInfo = tjTaskService.pageList(bo);
        tableDataInfo.setCode(200);
        tableDataInfo.setMsg("操作成功");
        return tableDataInfo;
    }

    @ApiOperationSort(3)
    @ApiOperation(value = "保存")
    @PostMapping("/save")
    public AjaxResult edit(@RequestBody TaskBo dto)
    {
        int i = tjTaskService.saveTask(dto);
        if (i == 1)
            return AjaxResult.success();
        return AjaxResult.error();
    }

    @ApiOperationSort(4)
    @ApiOperation(value = "获取状态")
    @GetMapping("/getStatus")
    @ApiImplicitParam(name = "taskCaseId", value = "任务用例ID", required = true, dataType = "Integer", paramType = "query", example = "278")
    public AjaxResult getStatus(@RequestParam("taskCaseId") Integer taskCaseId) throws BusinessException {
        return AjaxResult.success(taskCaseService.getStatus(taskCaseId));
    }

    @ApiOperationSort(5)
    @ApiOperation(value = "准备")
    @GetMapping("/prepare")
    @ApiImplicitParam(name = "taskCaseId", value = "任务用例ID", required = true, dataType = "Integer", paramType = "query", example = "278")
    public AjaxResult prepare(@RequestParam("taskCaseId") Integer taskCaseId) throws BusinessException {
        return AjaxResult.success(taskCaseService.prepare(taskCaseId));
    }

    @ApiOperationSort(6)
    @ApiOperation(value = "开始")
    @GetMapping("/start")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "recordId", value = "测试记录ID", required = true, dataType = "Integer", paramType = "query", example = "499"),
            @ApiImplicitParam(name = "action", value = "动作（1：开始；2：结束）", required = true, dataType = "Integer", paramType = "query", example = "1")
    })
    public AjaxResult start(@RequestParam("recordId") Integer recordId,
                            @RequestParam("action") Integer action) throws BusinessException, IOException {
        return  AjaxResult.success(taskCaseService.start(recordId, action));
    }

    @ApiOperationSort(7)
    @ApiOperation(value = "回放")
    @GetMapping("/playback")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "recordId", value = "测试记录ID", required = true, dataType = "Integer", paramType = "query", example = "499"),
            @ApiImplicitParam(name = "action", value = "动作（1：开始；2：结束）", required = true, dataType = "Integer", paramType = "query", example = "1")
    })
    public AjaxResult playback(@RequestParam("recordId") Integer recordId, @RequestParam("action") Integer action)
            throws BusinessException, IOException {
        taskCaseService.playback(recordId, action);
        return  AjaxResult.success();
    }

    @ApiOperationSort(8)
    @ApiOperation(value = "测试结果")
    @GetMapping("/getResult")
    @ApiImplicitParam(name = "recordId", value = "测试记录ID", required = true, dataType = "Integer", paramType = "query", example = "499")
    public AjaxResult getResult(@RequestParam("recordId") Integer recordId) throws BusinessException {
        return AjaxResult.success(taskCaseService.getResult(recordId));
    }

    @ApiOperationSort(9)
    @ApiOperation(value = "图形列表")
    @GetMapping("/communicationDelay")
    @ApiImplicitParam(name = "recordId", value = "测试记录ID", required = true, dataType = "Integer", paramType = "query", example = "499")
    public AjaxResult communicationDelayVo(@RequestParam Integer recordId) {
        return AjaxResult.success(taskCaseService.communicationDelayVo(recordId));
    }

    @ApiOperationSort(10)
    @ApiOperation(value = "导出")
    @PostMapping("/report")
    @ApiImplicitParam(name = "taskId", value = "任务ID", required = true, dataType = "Integer", paramType = "query", example = "499")
    public void report(HttpServletResponse response, @RequestParam("taskId") Integer taskId) throws IOException {
        tjTaskService.export(response, taskId);
    }
}
