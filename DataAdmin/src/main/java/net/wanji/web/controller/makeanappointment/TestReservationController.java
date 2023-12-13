package net.wanji.web.controller.makeanappointment;

import io.swagger.annotations.*;
import net.wanji.approve.entity.AppointmentRecord;
import net.wanji.approve.service.AppointmentRecordService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.makeanappointment.domain.vo.TestObjectVo;
import net.wanji.makeanappointment.domain.vo.TestTypeVo;
import net.wanji.makeanappointment.service.TestReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName TestReservationController
 * @Description
 * @Author liruitao
 * @Date 2023-12-06
 * @Version 1.0
 **/
@Api(tags = "测试预约平台-测试预约申请")
@RestController
@RequestMapping("/testReservation")
public class TestReservationController extends BaseController {

    private static Logger logger = LoggerFactory.getLogger(TestReservationController.class);

    @Autowired
    private TestReservationService testReservationService;

    @Autowired
    private AppointmentRecordService appointmentRecordService;

    /**
     * 获取测试类型
     */
    @ApiOperationSort(1)
    @ApiOperation(value = "1.获取测试类型")
    @GetMapping("/getTestType")
    public AjaxResult getTestType() {
        try {
            return AjaxResult.success(testReservationService.getTestType());
        }catch (Exception e){
            logger.error("获取测试类型失败", e);
            return AjaxResult.error("获取测试类型失败!");
        }
    }

    @ApiOperation(value = "2.新增预约")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appointmentRecord", value = "预约信息", required = true, dataType = "AppointmentRecord")
    })
    @PostMapping("/addApprove")
    public AjaxResult addOrUpdateApprove(@RequestBody AppointmentRecord appointmentRecord) {
        try {
            return AjaxResult.success(appointmentRecordService.addApprove(appointmentRecord));
        }catch (Exception e){
            logger.error("新增预约失败", e);
            return AjaxResult.error("新增预约失败!");
        }
    }

    @ApiOperation(value = "3.根据用例id计算测试费用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "caseIds", value = "用例id", required = true)
    })
    @GetMapping("/getExpenseByCaseIds")
    public AjaxResult getExpenseByCaseIds(String caseIds) {
        try {
            return AjaxResult.success(appointmentRecordService.getExpenseByCaseIds(caseIds));
        }catch (Exception e){
            logger.error("新增预约失败", e);
            return AjaxResult.error("新增预约失败!");
        }
    }


}
