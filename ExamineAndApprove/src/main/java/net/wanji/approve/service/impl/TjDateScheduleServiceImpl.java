package net.wanji.approve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.approve.entity.AppointmentRecord;
import net.wanji.approve.entity.RecordRe;
import net.wanji.approve.entity.TjDateSchedule;
import net.wanji.approve.entity.dto.ScheduleDto;
import net.wanji.approve.entity.vo.DateScheduleVo;
import net.wanji.approve.entity.vo.ScheduleVo;
import net.wanji.approve.mapper.TjDateScheduleMapper;
import net.wanji.approve.service.AppointmentRecordService;
import net.wanji.approve.service.RecordReService;
import net.wanji.approve.service.TjDateScheduleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.approve.utils.CacheTools;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wj
 * @since 2023-12-07
 */
@Service
public class TjDateScheduleServiceImpl extends ServiceImpl<TjDateScheduleMapper, TjDateSchedule> implements TjDateScheduleService {

    @Autowired
    RecordReService recordReService;

    @Autowired
    AppointmentRecordService appointmentRecordService;

    @Override
    public List<DateScheduleVo> takeDateScheduleByDate(String year, Integer quarter) {
        QueryWrapper<TjDateSchedule> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("year", year);
        queryWrapper.eq("quarter", quarter);
        List<TjDateSchedule> tjDateSchedules = this.list(queryWrapper);
        List<ScheduleVo> result = tjDateSchedules.stream().map(item -> {
            ScheduleVo dateScheduleVo = new ScheduleVo();
            BeanUtils.copyBeanProp(dateScheduleVo, item);
            for (String id : item.getAppointmentIds().split(",")){
                dateScheduleVo.getRecordList().add(CacheTools.get(Integer.valueOf(id)));
            }
            return dateScheduleVo;
        }).collect(Collectors.toList());
        Map<String, List<ScheduleVo>> map = result.stream().collect(Collectors.groupingBy(ScheduleVo::getMouth));
        List<DateScheduleVo> dateScheduleVos = new ArrayList<>();
        for (Map.Entry<String, List<ScheduleVo>> entry : map.entrySet()) {
            DateScheduleVo dateScheduleVo = new DateScheduleVo();
            dateScheduleVo.setDate(entry.getKey());
            dateScheduleVo.setScheduleList(entry.getValue());
            dateScheduleVos.add(dateScheduleVo);
        }
        return dateScheduleVos;
    }

    @Override
    public Set<Integer> takeDeviceIds(ScheduleDto scheduleDto) {
        QueryWrapper<TjDateSchedule> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("date", scheduleDto.getDates());
        List<TjDateSchedule> tjDateSchedules = this.list(queryWrapper);
        Set<Integer> set = new HashSet<>();
        for (TjDateSchedule tjDateSchedule : tjDateSchedules) {
            set.addAll(Arrays.stream(tjDateSchedule.getAppointmentIds().split(",")).map(Integer::parseInt).collect(Collectors.toList()));
        }
        set.remove(scheduleDto.getRecordId());
        List<Integer> ids = new ArrayList<>(set);
        QueryWrapper<RecordRe> queryWrapperre = new QueryWrapper<>();
        queryWrapperre.in("id", ids);
        List<RecordRe> recordRes = recordReService.list(queryWrapperre);
        Set<Integer> res = new HashSet<>();
        for (RecordRe recordRe : recordRes) {
            res.addAll(Arrays.stream(recordRe.getDeviceIds().split(",")).map(Integer::parseInt).collect(Collectors.toList()));
        }
        return res;
    }

    @Override
    public void commitSchedule(ScheduleDto scheduleDto){
        QueryWrapper<TjDateSchedule> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("date", scheduleDto.getDates());
        List<TjDateSchedule> tjDateSchedules = this.list(queryWrapper);
        Set<String> dateSet = new HashSet<>();
        for(TjDateSchedule tjDateSchedule : tjDateSchedules){
            dateSet.add(tjDateSchedule.getDate());
            Set<Integer> recordSet = new HashSet<>();
            if(tjDateSchedule.getAppointmentIds()!= null){
                recordSet.addAll(Arrays.stream(tjDateSchedule.getAppointmentIds().split(",")).map(Integer::parseInt).collect(Collectors.toList()));
                recordSet.add(scheduleDto.getRecordId());
                tjDateSchedule.setAppointmentIds(recordSet.stream().map(String::valueOf).collect(Collectors.joining(",")));
            }
        }
        this.updateBatchById(tjDateSchedules);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for(String date : scheduleDto.getDates()){
            if(!dateSet.contains(date)){
                TjDateSchedule tjDateSchedule = new TjDateSchedule();
                tjDateSchedule.setDate(date);
                tjDateSchedule.setAppointmentIds(String.valueOf(scheduleDto.getRecordId()));
                LocalDate dateLocal = LocalDate.parse(date, formatter);
                tjDateSchedule.setYear(String.valueOf(dateLocal.getYear()));
                tjDateSchedule.setQuarter(getQuarter(dateLocal));
                this.save(tjDateSchedule);
            }
        }
        AppointmentRecord appointmentRecord = appointmentRecordService.getById(scheduleDto.getRecordId());
        appointmentRecord.setTestSchedule(String.join(",", scheduleDto.getDates()));
        appointmentRecordService.updateById(appointmentRecord);
        RecordRe recordRe = new RecordRe();
        recordRe.setId(scheduleDto.getRecordId());
        recordRe.setDeviceIds(scheduleDto.getDeviceIds());
        recordRe.setPersonIds(scheduleDto.getPersonIds());
        recordReService.saveOrUpdate(recordRe);
    }

    private static String getQuarter(LocalDate date) {
        int month = date.getMonthValue();
        if (month <= 3) {
            return "1";
        } else if (month <= 6) {
            return "2";
        } else if (month <= 9) {
            return "3";
        } else {
            return "4";
        }
    }
}