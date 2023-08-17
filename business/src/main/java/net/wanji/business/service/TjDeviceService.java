package net.wanji.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.dto.TjDeviceDto;
import net.wanji.business.domain.vo.DeviceVo;
import net.wanji.business.entity.TjDevice;
import net.wanji.common.core.domain.SimpleSelect;

import java.util.List;
import java.util.Map;

/**
* @author guanyuduo
* @description 针对表【tj_device】的数据库操作Service
* @createDate 2023-08-17 10:56:39
*/
public interface TjDeviceService extends IService<TjDevice> {

    /**
     * 初始化
     * @return
     */
    Map<String, List<SimpleSelect>> init();

    /**
     * 设备列表查询
     * @param deviceDto
     * @return
     */
    List<DeviceVo> getAllDevices(TjDeviceDto deviceDto);

    /**
     * 设备列表查询
     * @param deviceDto
     * @return
     */
    DeviceVo getDeviceDetail(TjDeviceDto deviceDto);

    /**
     * 保存设备信息
     * @param deviceDto
     * @return
     */
    boolean saveDevice(TjDeviceDto deviceDto);

    /**
     * 删除设备
     * @param deviceId
     * @return
     */
    boolean deleteDevice(Integer deviceId);


    /**
     * 批量删除设备
     * @param deviceIds
     * @return
     */
    boolean batchDeleteDevice(List<Integer> deviceIds);
}
