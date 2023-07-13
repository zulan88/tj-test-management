package net.wanji.dataset.controller;


import io.swagger.annotations.ApiOperation;
import net.wanji.common.annotation.DataSource;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.enums.DataSourceType;
import net.wanji.dataset.entity.InspectionStatus;
import net.wanji.dataset.service.InspectionStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * inspection_status 前端控制器
 * </p>
 *
 * @author wj
 * @since 2022-11-09
 */
@RestController
@RequestMapping("/inspection-status")
public class InspectionStatusController {

    @Autowired
    InspectionStatusService inspectionStatusService;

    @PostMapping
    @ApiOperation("车型异常数据审查")
    @DataSource(value = DataSourceType.SLAVE)
    public AjaxResult add(@RequestBody InspectionStatus inspectionStatus){
        inspectionStatusService.save(inspectionStatus);
        return AjaxResult.success();
    }
    @GetMapping("/updateInspectionStatus")
    @ApiOperation("车型异常确认审核")
    @ResponseBody
    @DataSource(value = DataSourceType.SLAVE)
    public AjaxResult confirmedToBeTrue( String recordId,String beTrue) {
        InspectionStatus inspectionStatus = new InspectionStatus();
        inspectionStatus.setRecordId(recordId);
        inspectionStatus.setBeTrue(beTrue);
        inspectionStatus.setCheckCondition("1");
        inspectionStatusService.updateInspectionStatus(inspectionStatus);
            return AjaxResult.success();


    }

}
