package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.common.Constants.ChannelBuilder;
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
import net.wanji.business.trajectory.DeviceStateListener;
import net.wanji.common.utils.SecurityUtils;
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

    @Autowired
    private DeviceStateListener deviceStateListener;

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
            deviceDetail.setAttribute2(SecurityUtils.getUsername());
            deviceDetail.setCreatedDate(LocalDateTime.now());
            if (!ObjectUtils.isEmpty(deviceDetailDto.getAttribute2())) {
                deviceDetail.setUpdatedBy(deviceDetailDto.getAttribute2());
            }
            return this.save(deviceDetail);
        }
        deviceDetail.setDeviceId(deviceDetailDto.getDeviceId());
        deviceDetail.setDeviceName(deviceDetailDto.getDeviceName());
        deviceDetail.setSupportRoles(deviceDetailDto.getSupportRoles());
        deviceDetail.setIp(deviceDetailDto.getIp());
        deviceDetail.setServiceAddress(deviceDetailDto.getServiceAddress());
        deviceDetail.setDataChannel(deviceDetailDto.getDataChannel());
        deviceDetail.setCommandChannel(deviceDetailDto.getCommandChannel());
        if (!ObjectUtils.isEmpty(deviceDetailDto.getAttribute2())) {
            deviceDetail.setUpdatedBy(deviceDetailDto.getAttribute2());
        }
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
    public Integer selectDeviceState(Integer deviceId, String commandChannel, boolean wait) {
        Integer state = deviceStateToRedis.query(deviceId, ChannelBuilder.DEFAULT_STATUS_CHANNEL, DeviceStateToRedis.DEVICE_STATE_PREFIX);
        if (!ObjectUtils.isEmpty(state)) {
            return state;
        }
        return handDeviceState(deviceId, commandChannel, wait);
    }

    @Override
    public Integer handDeviceState(Integer deviceId, String commandChannel, boolean wait) {
        DeviceStateDto deviceStateDto = new DeviceStateDto();
        deviceStateDto.setDeviceId(deviceId);
        deviceStateDto.setType(0);
        deviceStateDto.setTimestamp(System.currentTimeMillis());
        log.info("发送数据：查询设备{}状态  {}", deviceId, JSONObject.toJSONString(deviceStateDto));
        deviceStateSendService.sendData(commandChannel, deviceStateDto);
        if (!wait) {
            return deviceStateToRedis.query(deviceId, ChannelBuilder.DEFAULT_STATUS_CHANNEL, DeviceStateToRedis.DEVICE_STATE_PREFIX);
        }
        String key =  DeviceStateToRedis.DEVICE_STATE_PREFIX + "_" + deviceId + "_" + ChannelBuilder.DEFAULT_STATUS_CHANNEL;
        try {
            StatusManage.addCountDownLatch(key, 50);
        } catch (InterruptedException e) {
            log.error("查询设备状态异常", e);
        }
        return (Integer) StatusManage.getValue(key);
    }

    @Override
    public Integer selectDeviceReadyState(Integer deviceId, String statusChannel, DeviceReadyStateParam stateParam, boolean wait) {
        Integer state = deviceStateToRedis.query(deviceId, statusChannel, DeviceStateToRedis.DEVICE_READY_STATE_PREFIX);
        if (!ObjectUtils.isEmpty(state) && state == 1) {
            return state;
        }
        return handDeviceReadyState(deviceId, statusChannel, stateParam, wait);
    }

    @Override
    public Integer handDeviceReadyState(Integer deviceId, String statusChannel, DeviceReadyStateParam stateParam, boolean wait) {
        if (!ChannelBuilder.DEFAULT_STATUS_CHANNEL.equals(statusChannel)) {
            deviceStateListener.addDeviceStateListener(statusChannel);
        }
        restService.selectDeviceReadyState(stateParam);
        if (!wait) {
            Integer readyState = deviceStateToRedis.query(deviceId, statusChannel, DeviceStateToRedis.DEVICE_READY_STATE_PREFIX);
            return ObjectUtils.isEmpty(readyState) ? 0 : readyState;
        }
        String key =  DeviceStateToRedis.DEVICE_READY_STATE_PREFIX + "_" + deviceId + "_" + statusChannel;
        try {
            StatusManage.addCountDownLatch(key, 50);
        } catch (InterruptedException e) {
            log.error("查询设备准备状态异常", e);
        }
        return (Integer) StatusManage.getValue(key);
    }
}
