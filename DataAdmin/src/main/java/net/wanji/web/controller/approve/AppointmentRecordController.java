package net.wanji.web.controller.approve;

import net.wanji.approve.entity.AppointmentRecord;
import net.wanji.approve.service.AppointmentRecordService;
import net.wanji.approve.service.RecordReService;
import net.wanji.approve.utils.CacheTools;
import net.wanji.business.domain.vo.CasePageVo;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;

@RestController
@RequestMapping("/approve")
public class AppointmentRecordController extends BaseController {

    @Autowired
    private AppointmentRecordService appointmentRecordService;

    @Autowired
    RecordReService recordReService;

    @PostConstruct
    public void init() {
        List<AppointmentRecord> list = appointmentRecordService.list();
        for (AppointmentRecord appointmentRecord : list){
            CacheTools.put(appointmentRecord.getId(), appointmentRecord.getUnitName()+" "+appointmentRecord.getType()+" "+appointmentRecord.getMeasurandType());
        }
    }

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
    public TableDataInfo getcase(Integer id, Integer treeId) throws BusinessException {
        startPage();
        List<CasePageVo> list = appointmentRecordService.pageList(id, treeId);
        return getDataTable(list);
    }

    @GetMapping("/expense")
    public AjaxResult getcaselist(Integer id) throws BusinessException {
        return AjaxResult.success(appointmentRecordService.getExpense(id));
    }

    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Integer id) throws BusinessException {
        CacheTools.remove(id);
        return toAjax(appointmentRecordService.removeById(id));
    }

    @GetMapping("/devicetoschedule")
    public AjaxResult devicetoschedule(Integer deviceId) throws BusinessException {
        List<AppointmentRecord> list = recordReService.getrecordBydevice(deviceId);
        return AjaxResult.success(list);
    }

}
