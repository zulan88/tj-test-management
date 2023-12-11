package net.wanji.approve.mapper;

import net.wanji.approve.entity.RecordRe;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author wj
 * @since 2023-12-07
 */
public interface RecordReMapper extends BaseMapper<RecordRe> {

    List<Integer> selectBydevice(Integer deviceId);

    List<Integer> selectByperson(Integer personId);

}
