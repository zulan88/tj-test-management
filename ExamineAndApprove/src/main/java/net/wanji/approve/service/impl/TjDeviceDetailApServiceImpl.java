package net.wanji.approve.service.impl;

import net.wanji.approve.entity.TjDeviceDetail;
import net.wanji.approve.entity.vo.DeviceListVo;
import net.wanji.approve.entity.vo.TjDeviceDetailVo;
import net.wanji.approve.mapper.TjDeviceDetailApMapper;
import net.wanji.approve.service.TjDeviceDetailApService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.common.utils.bean.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Override
    public List<DeviceListVo> getDevices(Set<Integer> set) {
        List<TjDeviceDetailVo> list = this.list().stream().map(item -> {
            TjDeviceDetailVo tjDeviceDetailVo = new TjDeviceDetailVo();
            BeanUtils.copyBeanProp(tjDeviceDetailVo, item);
            if(set.contains(item.getDeviceId())){
                tjDeviceDetailVo.setOccstatus(1);
            }else {
                tjDeviceDetailVo.setOccstatus(0);
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
}
