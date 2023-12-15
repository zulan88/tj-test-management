package net.wanji.approve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.approve.entity.AppointmentRecord;
import net.wanji.approve.entity.RecordRe;
import net.wanji.approve.entity.TjDeviceDetail;
import net.wanji.approve.entity.vo.DeviceListVo;
import net.wanji.approve.entity.vo.TjDeviceDetailVo;
import net.wanji.approve.mapper.TjDeviceDetailApMapper;
import net.wanji.approve.service.RecordReService;
import net.wanji.approve.service.TjDeviceDetailApService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.entity.TjCasePartConfig;
import net.wanji.business.service.TjCasePartConfigService;
import net.wanji.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class TjDeviceDetailApServiceImpl extends ServiceImpl<TjDeviceDetailApMapper, TjDeviceDetail> implements TjDeviceDetailApService {

    @Autowired
    TjCasePartConfigService tjCasePartConfigService;

    @Autowired
    RecordReService recordReService;

    @Override
    public List<DeviceListVo> getDevices(Set<Integer> set, Integer recordId) {
        RecordRe recordRe = recordReService.getById(recordId);
        List<Integer> selectdeviceIds = new ArrayList<>();
        if(recordRe!=null) {
            selectdeviceIds = Arrays.stream(recordRe.getDeviceIds().split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }
        List<Integer> finalSelectdeviceIds = selectdeviceIds;
        QueryWrapper<TjDeviceDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_inner",0);
        List<TjDeviceDetailVo> list = this.list(queryWrapper).stream().map(item -> {
            TjDeviceDetailVo tjDeviceDetailVo = new TjDeviceDetailVo();
            BeanUtils.copyBeanProp(tjDeviceDetailVo, item);
            if(set.contains(item.getDeviceId())){
                tjDeviceDetailVo.setOccstatus(1);
            }else {
                tjDeviceDetailVo.setOccstatus(0);
            }
            if(finalSelectdeviceIds.contains(item.getDeviceId())){
                tjDeviceDetailVo.setIsSelect(1);
            }else {
                tjDeviceDetailVo.setIsSelect(0);
            }
            return tjDeviceDetailVo;
        }).collect(Collectors.toList());
        Map<String, List<TjDeviceDetailVo>> map = list.stream().collect(Collectors.groupingBy(TjDeviceDetailVo::getDeviceType));
        List<DeviceListVo> result = new ArrayList<>();
        for (Map.Entry<String, List<TjDeviceDetailVo>> entry : map.entrySet()){
            DeviceListVo deviceListVo = new DeviceListVo();
            deviceListVo.setType(entry.getKey());
            deviceListVo.setTjDeviceDetailVos(entry.getValue());
            result.add(deviceListVo);
        }
        return result;
    }

    @Override
    public List<DeviceListVo> addInfo(List<DeviceListVo> list, List<Integer> deviceIds, AppointmentRecord appointmentRecord) {
        List<Integer> caseIds = Arrays.stream(appointmentRecord.getCaseIds().split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        for (DeviceListVo deviceListVo:list){
            QueryWrapper<TjDeviceDetail> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("device_type", deviceListVo.getType());
            queryWrapper.in("device_id", deviceIds);
            List<Integer> typeDeviceIds = this.list(queryWrapper).stream().map(TjDeviceDetail::getDeviceId).collect(Collectors.toList());
            deviceListVo.setMaxDeviceCount(typeDeviceIds.size());
            QueryWrapper<TjCasePartConfig> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.in("case_id", caseIds);
            if(typeDeviceIds.size()>0) {
                queryWrapper1.in("device_id", typeDeviceIds);
            }
            queryWrapper1.groupBy("case_id");
            queryWrapper1.select("case_id");
            deviceListVo.setCaseCount(Math.toIntExact(tjCasePartConfigService.list(queryWrapper1).size()));
        }
        return list;
    }
}
