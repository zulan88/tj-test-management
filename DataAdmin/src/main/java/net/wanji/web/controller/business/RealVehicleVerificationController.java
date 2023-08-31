package net.wanji.web.controller.business;

import net.wanji.business.common.Constants.MasterControl;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TestingService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/24 9:13
 * @Descriptoin:
 */
@RestController
@RequestMapping("/testing")
public class RealVehicleVerificationController extends BaseController {

    @Autowired
    private TestingService testingService;

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
    public AjaxResult start(@RequestParam("recordId") Integer recordId) throws BusinessException {
        return  AjaxResult.success(testingService.start(recordId, MasterControl.START));
    }

//    @PreAuthorize("@ss.hasPermi('testing:getResult')")
//    @GetMapping("/getResult")
//    public AjaxResult getResult(@RequestParam("caseId") Integer caseId) throws BusinessException {
//        return testingService.start(caseId, MasterControl.START) ? AjaxResult.success() : AjaxResult.error("失败");
//    }
}
