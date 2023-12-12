package net.wanji.web.controller.makeanappointment;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiOperationSort;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.makeanappointment.domain.vo.TestObjectVo;
import net.wanji.makeanappointment.domain.vo.TestTypeVo;
import net.wanji.makeanappointment.service.TestReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class TestReservationController {

    private static Logger logger = LoggerFactory.getLogger(TestReservationController.class);

    @Autowired
    private TestReservationService testReservationService;

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

}
