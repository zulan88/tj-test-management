package net.wanji.web.controller.approve;

import net.wanji.approve.entity.AppointmentRecord;
import net.wanji.approve.service.AppointmentRecordService;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/approve")
public class AppointmentRecordController extends BaseController {

    @Autowired
    private AppointmentRecordService appointmentRecordService;

    @GetMapping("/list")
    public TableDataInfo list(AppointmentRecord appointmentRecord) {
        startPage();
        List<AppointmentRecord> list = appointmentRecordService.list();
        return getDataTable(list);
    }

    @PutMapping("/edit")
    public AjaxResult edit(AppointmentRecord appointmentRecord) {
        return toAjax(appointmentRecordService.updateById(appointmentRecord));
    }

    @GetMapping("/getinfo/{id}")
    public AjaxResult getinfo(@RequestParam Integer id) throws BusinessException {
        return AjaxResult.success(appointmentRecordService.getInfoById(id));
    }

    @GetMapping("/getcase")
    public AjaxResult getcase(Integer id) throws BusinessException {
        return AjaxResult.success(appointmentRecordService.pageList(id));
    }

    @GetMapping("/expense")
    public AjaxResult getcaselist(Integer id) throws BusinessException {
        return AjaxResult.success(appointmentRecordService.getExpense(id));
    }

}
