package net.wanji.web.controller.business;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TestingService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/24 9:13
 * @Descriptoin:
 */
@Api("测试服务")
@RestController
@RequestMapping("/testing")
public class TestingController extends BaseController {

    @Autowired
    private TestingService testingService;

    @ApiOperation("测试服务列表")
    @PreAuthorize("@ss.hasPermi('testing:list')")
    @PostMapping("/list")
    public AjaxResult list(@RequestBody TjCaseDto caseDto) throws BusinessException {
        return AjaxResult.success(testingService.list(caseDto));
    }

//    @ApiOperation("配置详情")
//    @PreAuthorize("@ss.hasPermi('testing:configDetail')")
//    @PostMapping("/configDetail")
//    public AjaxResult configDetail(@RequestParam("sceneDetailId") Integer sceneDetailId, Integer caseId) throws BusinessException {
//        return AjaxResult.success(testingService.configDetail(sceneDetailId, caseId) ? "成功" : "失败");
//    }

    @ApiOperation("配置角色")
    @PreAuthorize("@ss.hasPermi('testing:configRole')")
    @PostMapping("/configRole")
    public AjaxResult configRole(@RequestBody TjCaseDto caseDto) throws BusinessException {
        return AjaxResult.success(testingService.configRole(caseDto) ? "成功" : "失败");
    }

    @ApiOperation("修改状态")
    @PreAuthorize("@ss.hasPermi('testing:updateState')")
    @GetMapping("/updateState")
    public AjaxResult updateState(@RequestParam("caseId") Integer caseId) throws BusinessException {
        return AjaxResult.success(testingService.updateState(caseId) ? "成功" : "失败");
    }

    @ApiOperation("删除")
    @PreAuthorize("@ss.hasPermi('testing:delete')")
    @GetMapping("/delete")
    public AjaxResult delete(@RequestParam("caseId") Integer caseId) throws BusinessException {
        return AjaxResult.success(testingService.delete(caseId) ? "成功" : "失败");
    }

    @ApiOperation("配置设备")
    @PreAuthorize("@ss.hasPermi('testing:configDevice')")
    @PostMapping("/configDevice")
    public AjaxResult configDevice(@RequestBody TjCaseDto caseDto) throws BusinessException {
        return AjaxResult.success(testingService.configDevice(caseDto) ? "成功" : "失败");
    }


    @PreAuthorize("@ss.hasPermi('testing:getStatus')")
    @GetMapping("/getStatus")
    public AjaxResult getStatus(@RequestParam("caseId") Integer caseId) throws BusinessException {
        return AjaxResult.success(testingService.getStatus(caseId));
    }

    @PreAuthorize("@ss.hasPermi('testing:prepare')")
    @GetMapping("/prepare")
    public AjaxResult prepare(@RequestParam("caseId") Integer caseId) throws BusinessException {
        return AjaxResult.success(testingService.prepare(caseId));
    }

    @PreAuthorize("@ss.hasPermi('testing:start')")
    @GetMapping("/start")
    public AjaxResult start(@RequestParam("recordId") Integer recordId,
                            @RequestParam("action") Integer action) throws BusinessException, IOException {
        return  AjaxResult.success(testingService.start(recordId, action));
    }

    @PreAuthorize("@ss.hasPermi('testing:playback')")
    @GetMapping("/playback")
    public AjaxResult playback(@RequestParam("recordId") Integer recordId, @RequestParam("action") Integer action)
            throws BusinessException, IOException {
        testingService.playback(recordId, action);
        return  AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('testing:getResult')")
    @GetMapping("/getResult")
    public AjaxResult getResult(@RequestParam("recordId") Integer recordId) throws BusinessException {
        return AjaxResult.success(testingService.getResult(recordId));
    }

    @PreAuthorize("@ss.hasPermi('testing:communicationDelay')")
    @GetMapping("/communicationDelay")
    public AjaxResult communicationDelayVo(@RequestParam Integer recordId) {
        return AjaxResult.success(testingService.communicationDelayVo(recordId));
    }
}
