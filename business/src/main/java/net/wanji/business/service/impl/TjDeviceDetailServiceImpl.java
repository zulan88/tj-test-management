package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.DeviceStatusEnum;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.dto.TjDeviceDetailDto;
import net.wanji.business.domain.vo.DeviceDetailVo;
import net.wanji.business.entity.TjDeviceDetail;
import net.wanji.business.mapper.TjDeviceDetailMapper;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.system.service.ISysDictDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author guanyuduo
 * @description 针对表【tj_device】的数据库操作Service实现
 * @createDate 2023-08-17 10:56:39
 */
@Service
public class TjDeviceDetailServiceImpl extends ServiceImpl<TjDeviceDetailMapper, TjDeviceDetail> implements TjDeviceDetailService {

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private TjDeviceDetailMapper deviceDetailMapper;

    @Override
    public List<DeviceDetailVo> getAllDevices(TjDeviceDetailDto deviceDto) {
        List<DeviceDetailVo> devices = deviceDetailMapper.selectByCondition(deviceDto);
        return CollectionUtils.emptyIfNull(devices).stream().map(this::translate).collect(Collectors.toList());
    }

    @Override
    public DeviceDetailVo getDeviceDetail(TjDeviceDetailDto deviceDto) {
        List<DeviceDetailVo> devices = deviceDetailMapper.selectByCondition(deviceDto);
        DeviceDetailVo detailVo = CollectionUtils.isEmpty(devices) ? new DeviceDetailVo() : devices.get(0);
        return this.translate(detailVo);
    }

    private DeviceDetailVo translate(DeviceDetailVo detailVo) {
        detailVo.setStatusName(DeviceStatusEnum.getDescByCode(detailVo.getStatus()));
        detailVo.setSupportRolesName(dictDataService.selectDictLabel(SysType.PART_ROLE, detailVo.getSupportRoles()));
        return detailVo;
    }

    @Override
    public boolean saveDevice(TjDeviceDetailDto deviceDetailDto) {
        TjDeviceDetail deviceDetail = new TjDeviceDetail();
        if (ObjectUtils.isEmpty(deviceDetailDto.getDeviceId())) {
            // todo 设备状态刷新接口
            BeanUtils.copyProperties(deviceDetailDto, deviceDetail);
            deviceDetail.setStatus(YN.Y_INT);
            deviceDetail.setLastOnlineDate(LocalDateTime.now());
            deviceDetail.setCreatedBy(SecurityUtils.getUsername());
            deviceDetail.setCreatedDate(LocalDateTime.now());
            return this.save(deviceDetail);
        }
        deviceDetail.setDeviceId(deviceDetailDto.getDeviceId());
        deviceDetail.setDeviceName(deviceDetailDto.getDeviceName());
        deviceDetail.setSupportRoles(deviceDetailDto.getSupportRoles());
        deviceDetail.setIp(deviceDetailDto.getIp());
        deviceDetail.setServiceAddress(deviceDetailDto.getServiceAddress());
        deviceDetail.setDataChannel(deviceDetailDto.getDataChannel());
        deviceDetail.setCommandChannel(deviceDetailDto.getCommandChannel());
        deviceDetail.setUpdatedBy(SecurityUtils.getUsername());
        deviceDetail.setUpdatedDate(LocalDateTime.now());
        return this.updateById(deviceDetail);
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
