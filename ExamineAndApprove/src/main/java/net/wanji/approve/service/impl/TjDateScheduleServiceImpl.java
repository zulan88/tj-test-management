package net.wanji.approve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.approve.entity.TjDateSchedule;
import net.wanji.approve.entity.vo.DateScheduleVo;
import net.wanji.approve.entity.vo.ScheduleVo;
import net.wanji.approve.mapper.TjDateScheduleMapper;
import net.wanji.approve.service.TjDateScheduleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.approve.utils.CacheTools;
import net.wanji.common.utils.bean.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
}
