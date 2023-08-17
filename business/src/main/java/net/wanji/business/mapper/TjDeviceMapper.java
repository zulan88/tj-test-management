package net.wanji.business.mapper;

import net.wanji.business.domain.dto.TjDeviceDto;
import net.wanji.business.entity.TjDevice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author guanyuduo
* @description 针对表【tj_device】的数据库操作Mapper
* @createDate 2023-08-17 10:56:39
* @Entity net.wanji.business.entity.TjDevice
*/
public interface TjDeviceMapper extends BaseMapper<TjDevice> {

    /**
     * 条件查询
     * @param deviceDto
     * @return
     */
    List<TjDevice> selectByCondition(TjDeviceDto deviceDto);
}
