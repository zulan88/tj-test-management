package net.wanji.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.dto.TjDeviceDetailDto;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.vo.DeviceDetailVo;
import net.wanji.business.entity.TjDeviceDetail;

import java.util.List;

/**
 * @author guanyuduo
 * @description 针对表【tj_device】的数据库操作Service
 * @createDate 2023-08-17 10:56:39
 */
public interface TjDeviceDetailService extends IService<TjDeviceDetail> {

    /**
     * 设备列表查询
     *
     * @param deviceDto
     * @return
     */
    List<DeviceDetailVo> getAllDevices(TjDeviceDetailDto deviceDto);

    /**
     * 设备列表查询
     *
     * @param deviceDto
     * @return
     */
    DeviceDetailVo getDeviceDetail(TjDeviceDetailDto deviceDto);

    /**
     * 保存设备信息
     *
     * @param deviceDto
     * @return
     */
    boolean saveDevice(TjDeviceDetailDto deviceDto);

    /**
     * 删除设备
     *
     * @param deviceId
     * @return
     */
    boolean deleteDevice(Integer deviceId);


    /**
     * 批量删除设备
     *
     * @param deviceIds
     * @return
     */
    boolean batchDeleteDevice(List<Integer> deviceIds);

    /**
     * 查询设备状态
     * @param deviceId 设备id
     * @param commandChannel 控制通道
     * @param wait 是否等待
     * @return
     */
    Integer selectDeviceState(Integer deviceId, String commandChannel, boolean wait);

    /**
     * 手动查询设备状态
     * @param deviceId
     * @param commandChannel
     * @param wait
     * @return
     */
    Integer handDeviceState(Integer deviceId, String commandChannel, boolean wait);


    /**
     * 查询设备准备状态
     *
     * @param deviceId
     * @param statusChannel
     * @param stateParam
     * @param wait
     * @return
     */
    Integer selectDeviceReadyState(Integer deviceId, String statusChannel, DeviceReadyStateParam stateParam, boolean wait);

    /**
     * 手动查询设备准备状态
     * @param deviceId
     * @param statusChannel
     * @param stateParam
     * @param wait
     * @return
     */
    Integer handDeviceReadyState(Integer deviceId, String statusChannel, DeviceReadyStateParam stateParam, boolean wait);

}
