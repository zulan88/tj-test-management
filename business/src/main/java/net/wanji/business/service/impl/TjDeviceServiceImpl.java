package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.DeviceStatusEnum;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.dto.TjDeviceDto;
import net.wanji.business.domain.vo.DeviceVo;
import net.wanji.business.entity.TjDevice;
import net.wanji.business.mapper.TjDeviceMapper;
import net.wanji.business.service.TjDeviceService;
import net.wanji.common.core.domain.SimpleSelect;
import net.wanji.common.core.domain.entity.SysDictData;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.system.service.ISysDictDataService;
import net.wanji.system.service.ISysDictTypeService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author guanyuduo
* @description 针对表【tj_device】的数据库操作Service实现
* @createDate 2023-08-17 10:56:39
*/
@Service
public class TjDeviceServiceImpl extends ServiceImpl<TjDeviceMapper, TjDevice> implements TjDeviceService {

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private TjDeviceMapper deviceMapper;

    @Override
    public Map<String, List<SimpleSelect>> init() {
        List<SysDictData> deviceType = dictTypeService.selectDictDataByType(SysType.DEVICE_TYPE);
        Map<String, List<SimpleSelect>> result = new HashMap<>(1);
        result.put(SysType.DEVICE_TYPE, CollectionUtils.emptyIfNull(deviceType).stream()
                .map(SimpleSelect::new).collect(Collectors.toList()));
        return result;
    }

    @Override
    public List<DeviceVo> getAllDevices(TjDeviceDto deviceDto) {
        List<TjDevice> devices = deviceMapper.selectByCondition(deviceDto);
        return CollectionUtils.emptyIfNull(devices).stream().map(this::transToVo).collect(Collectors.toList());
    }

    @Override
    public DeviceVo getDeviceDetail(TjDeviceDto deviceDto) {
        return this.transToVo(this.getById(deviceDto.getDeviceId()));
    }

    private DeviceVo transToVo(TjDevice device) {
        DeviceVo deviceVo = new DeviceVo();
        BeanUtils.copyProperties(device, deviceVo);
        deviceVo.setDeviceTypeName(dictDataService.selectDictLabel(SysType.DEVICE_TYPE, device.getDeviceType()));
        deviceVo.setStatusName(DeviceStatusEnum.getDescByCode(device.getStatus()));
        return deviceVo;
    }

    @Override
    public boolean saveDevice(TjDeviceDto deviceDto) {
        TjDevice device = new TjDevice();
        BeanUtils.copyProperties(deviceDto, device);
        if (ObjectUtils.isEmpty(deviceDto.getDeviceId())) {
            // todo 设备状态刷新接口
            device.setStatus(YN.Y_INT);
            device.setLastOnlineDate(LocalDateTime.now());
            device.setCreatedBy(SecurityUtils.getUsername());
            device.setCreatedDate(LocalDateTime.now());
            return this.save(device);
        }

        device.setUpdatedBy(SecurityUtils.getUsername());
        device.setUpdatedDate(LocalDateTime.now());
        return this.updateById(device);
    }

    @Override
    public boolean deleteDevice(Integer deviceId) {
        return this.removeById(deviceId);
    }

    @Override
    public boolean batchDeleteDevice(List<Integer> deviceIds) {
        return this.removeByIds(deviceIds);
    }
}
