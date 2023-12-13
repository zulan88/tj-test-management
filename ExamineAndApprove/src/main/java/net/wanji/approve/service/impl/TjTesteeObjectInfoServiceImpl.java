package net.wanji.approve.service.impl;

import net.wanji.approve.entity.TjTesteeObjectInfo;
import net.wanji.approve.mapper.TjTesteeObjectInfoMapper;
import net.wanji.approve.service.TjTesteeObjectInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.domain.dto.TjDeviceDetailDto;
import net.wanji.business.service.TjDeviceDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 测试预约申请-被测对象列表 服务实现类
 * </p>
 *
 * @author wj
 * @since 2023-12-06
 */
@Service
public class TjTesteeObjectInfoServiceImpl extends ServiceImpl<TjTesteeObjectInfoMapper, TjTesteeObjectInfo> implements TjTesteeObjectInfoService {

    @Autowired
    private TjDeviceDetailService deviceDetailService;

    @Override
    public void adddevice(Integer id, String dataChannel, String commandChannel) {
        TjTesteeObjectInfo tjTesteeObjectInfo = this.getById(id);
        TjDeviceDetailDto tjDeviceDetailDto = new TjDeviceDetailDto();
        tjDeviceDetailDto.setDeviceName(tjTesteeObjectInfo.getTesteeObjectName());
        tjDeviceDetailDto.setDeviceType(tjTesteeObjectInfo.getTesteeObjectType());
        tjDeviceDetailDto.setSupportRoles("av");
        tjDeviceDetailDto.setDataChannel(dataChannel);
        tjDeviceDetailDto.setCommandChannel(commandChannel);
        deviceDetailService.saveDevice(tjDeviceDetailDto);
        tjTesteeObjectInfo.setDeviceId(tjDeviceDetailDto.getDeviceId());
        this.updateById(tjTesteeObjectInfo);
    }
}
