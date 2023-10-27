package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.common.Constants.DeviceStatusEnum;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.component.DeviceStateToRedis;
import net.wanji.business.domain.dto.TjDeviceDetailDto;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.dto.device.DeviceStateDto;
import net.wanji.business.domain.vo.DeviceDetailVo;
import net.wanji.business.entity.TjDeviceDetail;
import net.wanji.business.mapper.TjDeviceDetailMapper;
import net.wanji.business.service.DeviceStateSendService;
import net.wanji.business.service.RestService;
import net.wanji.business.service.StatusManage;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.system.service.ISysDictDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author guanyuduo
 * @description 针对表【tj_device】的数据库操作Service实现
 * @createDate 2023-08-17 10:56:39
 */
@Service
@Slf4j
public class TjDeviceDetailServiceImpl extends ServiceImpl<TjDeviceDetailMapper, TjDeviceDetail> implements TjDeviceDetailService {

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private TjDeviceDetailMapper deviceDetailMapper;

    @Autowired
    private DeviceStateToRedis deviceStateToRedis;

    @Autowired
    private DeviceStateSendService deviceStateSendService;

    @Autowired
    private RestService restService;

    @PostConstruct
    public void initDeviceState() {
        List<TjDeviceDetail> deviceDetails = this.list();
        for (TjDeviceDetail deviceDetail : deviceDetails) {
            selectDeviceState(deviceDetail.getDeviceId(), deviceDetail.getCommandChannel(), false);
        }
    }


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

    @Override
    public Integer selectDeviceState(Integer deviceId, String channel, boolean wait) {
        Integer state = deviceStateToRedis.query(deviceId, DeviceStateToRedis.DEVICE_STATE_PREFIX);
        if (!ObjectUtils.isEmpty(state)) {
            return state;
        }
        DeviceStateDto deviceStateDto = new DeviceStateDto();
        deviceStateDto.setDeviceId(deviceId);
        deviceStateDto.setType(0);
        deviceStateDto.setTimestamp(System.currentTimeMillis());
        log.info("发送数据：查询设备{}状态  {}", deviceId, JSONObject.toJSONString(deviceStateDto));
        deviceStateSendService.sendData(channel, deviceStateDto);
        if (!wait) {
            return deviceStateToRedis.query(deviceId, DeviceStateToRedis.DEVICE_STATE_PREFIX);
        }
        String key =  DeviceStateToRedis.DEVICE_STATE_PREFIX + "_" + deviceId;
        try {
            StatusManage.addCountDownLatch(key);
        } catch (InterruptedException e) {
            log.error("查询设备状态异常", e);
        }
        return (Integer) StatusManage.getValue(key);
    }

    @Override
    public Integer selectDeviceReadyState(Integer deviceId, DeviceReadyStateParam stateParam, boolean wait) {
        Integer state = deviceStateToRedis.query(deviceId, DeviceStateToRedis.DEVICE_READY_STATE_PREFIX);
        if (!ObjectUtils.isEmpty(state)) {
            return state;
        }
        restService.selectDeviceReadyState(stateParam);
        if (!wait) {
            return deviceStateToRedis.query(deviceId, DeviceStateToRedis.DEVICE_READY_STATE_PREFIX);
        }
        String key =  DeviceStateToRedis.DEVICE_READY_STATE_PREFIX + "_" + deviceId;
        try {
            StatusManage.addCountDownLatch(key);
        } catch (InterruptedException e) {
            log.error("查询设备准备状态异常", e);
        }
        return (Integer) StatusManage.getValue(key);
    }
}
