package net.wanji.web.controller.approve;

import net.wanji.approve.entity.vo.DateScheduleVo;
import net.wanji.approve.service.TjDateScheduleService;
import net.wanji.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dateSchedule")
public class DateScheduleController {

    @Autowired
    private TjDateScheduleService dateScheduleService;

    @GetMapping("/getschedule")
    public AjaxResult getSchedule(String year, Integer quarter){
        List<DateScheduleVo> list = dateScheduleService.takeDateScheduleByDate(year,quarter);
        return AjaxResult.success(list);
    }


}
