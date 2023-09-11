package net.wanji.web.controller.business;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import net.wanji.business.common.Constants.BatchGroup;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.vo.CaseVo;
import net.wanji.business.domain.vo.DiadynamicDetailVo;
import net.wanji.business.domain.vo.DiadynamicVo;
import net.wanji.business.domain.vo.TaskCaseReportVo;
import net.wanji.business.domain.vo.TaskReportVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.poi.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import net.wanji.business.domain.bo.TaskBo;
import net.wanji.business.domain.dto.CreateTaskDto;
import net.wanji.business.domain.dto.TaskDto;
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
@RestController
@RequestMapping("/task")
public class TaskController extends BaseController {

    @Autowired
    private TjTaskService tjTaskService;

    @Autowired
    private TjTaskCaseService taskCaseService;

    /**
     * 页面列表
     * @param bo
     * @return
     */
    @PreAuthorize("@ss.hasPermi('task:pageList')")
    @GetMapping("/pageList")
    public TableDataInfo pageList(TaskBo bo)
    {
        TableDataInfo tableDataInfo = tjTaskService.pageList(bo);
        tableDataInfo.setCode(200);
        tableDataInfo.setMsg("操作成功");
        return tableDataInfo;
    }

    /**
     * 创建任务
     * @return
     */
    @PreAuthorize("@ss.hasPermi('task:create')")
    @PostMapping("/create")
    public AjaxResult create(@RequestBody CreateTaskDto dto)
    {
        TaskVo taskVo = null;
        try {
            taskVo = tjTaskService.createTask(dto);
        } catch (BusinessException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return AjaxResult.success(taskVo);
    }

    /**
     * 保存测试任务
     * @return
     */
    @PreAuthorize("@ss.hasPermi('task:save')")
    @PostMapping("/save")
    public AjaxResult edit(@RequestBody TaskDto dto)
    {
        int i = tjTaskService.saveTask(dto);
        if (i == 1)
            return AjaxResult.success();
        return AjaxResult.error();
    }


    @PreAuthorize("@ss.hasPermi('task:getStatus')")
    @GetMapping("/getStatus")
    public AjaxResult getStatus(@RequestParam("caseId") Integer caseId) throws BusinessException {
        return AjaxResult.success(taskCaseService.getStatus(caseId));
    }

    @PreAuthorize("@ss.hasPermi('task:prepare')")
    @GetMapping("/prepare")
    public AjaxResult prepare(@RequestParam("caseId") Integer caseId) throws BusinessException {
        return AjaxResult.success(taskCaseService.prepare(caseId));
    }

    @PreAuthorize("@ss.hasPermi('task:start')")
    @GetMapping("/start")
    public AjaxResult start(@RequestParam("recordId") Integer recordId,
                            @RequestParam("action") Integer action) throws BusinessException, IOException {
        return  AjaxResult.success(taskCaseService.start(recordId, action));
    }

    @PreAuthorize("@ss.hasPermi('task:playback')")
    @GetMapping("/playback")
    public AjaxResult playback(@RequestParam("recordId") Integer recordId, @RequestParam("action") Integer action)
            throws BusinessException, IOException {
        taskCaseService.playback(recordId, action);
        return  AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('task:getResult')")
    @GetMapping("/getResult")
    public AjaxResult getResult(@RequestParam("recordId") Integer recordId) throws BusinessException {
        return AjaxResult.success(taskCaseService.getResult(recordId));
    }

    @PreAuthorize("@ss.hasPermi('task:communicationDelay')")
    @GetMapping("/communicationDelay")
    public AjaxResult communicationDelayVo(@RequestParam Integer recordId) {
        return AjaxResult.success(taskCaseService.communicationDelayVo(recordId));
    }

    @PreAuthorize("@ss.hasPermi('task:report')")
    @PostMapping("/report")
    public void report(HttpServletResponse response, @RequestParam("taskId") Integer taskId) throws IOException {
        tjTaskService.export(response, taskId);
    }
}
