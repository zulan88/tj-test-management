package net.wanji.web.controller.approve;

import net.wanji.approve.entity.AppointmentRecord;
import net.wanji.approve.service.AppointmentRecordService;
import net.wanji.business.domain.bo.TaskBo;
import net.wanji.business.entity.TjTask;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.service.TjTaskService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/taskop")
public class TaskOpController extends BaseController {

    @Autowired
    private TjTaskService tjTaskService;

    @Autowired
    private AppointmentRecordService appointmentRecordService;

    @Autowired
    private TjTaskCaseService taskCaseService;

    @PostMapping("/prestore/{id}")
    public AjaxResult prestore(@RequestParam("id")Integer id) throws ParseException, BusinessException {
        AppointmentRecord record = appointmentRecordService.getById(id);
        TaskBo taskBo = new TaskBo();
        taskBo.getAvDeviceIds().add(record.getMeasurandId());
        taskBo.setClient(record.getUnitName());
        taskBo.setConsigner(record.getContactPerson());
        taskBo.setContract(record.getPhoneNumber());
        List<String> dates = Arrays.asList(record.getExpectedDate().split(","));
        dates.sort(new StringDateComparator());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        taskBo.setStartTime(dateFormat.parse(dates.get(0)));
        taskBo.setEndTime(dateFormat.parse(dates.get(dates.size() - 1)));
        taskBo.setProcessNode(1);
        taskBo.setTestType("virtualRealFusion"); // 先写死
        int taskId = tjTaskService.saveTask(taskBo);
        List<Integer> caseIds = Arrays.stream(record.getCaseIds().split(",")).map(Integer::parseInt).collect(Collectors.toList());
        taskCaseService.addTaskCase(taskId, caseIds);
        TaskBo taskBo2 = new TaskBo();
        taskBo2.setId(taskId);
        taskBo2.setProcessNode(2);
        tjTaskService.saveTask(taskBo2);
        return AjaxResult.success(taskId);
    }

    @PostMapping("/recheckorcancel")
    public AjaxResult recheckorcancel(@RequestBody TjTask tjTask) throws BusinessException {
        return toAjax(tjTaskService.updateById(tjTask));
    }

}

class StringDateComparator implements Comparator<String> {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public int compare(String date1, String date2) {
        try {
            // 将日期字符串解析为Date对象，然后比较
            return dateFormat.parse(date1).compareTo(dateFormat.parse(date2));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format", e);
        }
    }
}
