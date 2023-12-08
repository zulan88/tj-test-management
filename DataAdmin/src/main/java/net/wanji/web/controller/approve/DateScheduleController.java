package net.wanji.web.controller.approve;

import net.wanji.approve.entity.dto.ScheduleDto;
import net.wanji.approve.entity.vo.DateScheduleVo;
import net.wanji.approve.entity.vo.DeviceListVo;
import net.wanji.approve.service.TjDateScheduleService;
import net.wanji.approve.service.TjDeviceDetailApService;
import net.wanji.common.core.domain.AjaxResult;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/dateSchedule")
public class DateScheduleController {

    @Autowired
    private TjDateScheduleService dateScheduleService;

    @Autowired
    private TjDeviceDetailApService tjDeviceDetailApService;

    @GetMapping("/getschedule")
    public AjaxResult getSchedule(String year, Integer quarter){
        List<DateScheduleVo> list = dateScheduleService.takeDateScheduleByDate(year,quarter);
        return AjaxResult.success(list);
    }

    @PostMapping("/getdevice")
    public AjaxResult getDevice(@RequestBody ScheduleDto scheduleDto){
        Set<Integer> set = dateScheduleService.takeDeviceIds(scheduleDto);
        List<DeviceListVo> list = tjDeviceDetailApService.getDevices(set);
        return AjaxResult.success(list);
    }

    @PostMapping("/commit")
    public AjaxResult commit(@RequestBody ScheduleDto scheduleDto){
        dateScheduleService.commitSchedule(scheduleDto);
        return AjaxResult.success();
    }


}
