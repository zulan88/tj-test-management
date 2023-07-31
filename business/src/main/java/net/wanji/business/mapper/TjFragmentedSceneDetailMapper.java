package net.wanji.business.mapper;

import net.wanji.business.domain.dto.SceneQueryDto;
import net.wanji.business.domain.dto.TjFragmentedSceneDetailDto;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 * 片段式场景定义 Mapper 接口
 * </p>
 *
 * @author wj
 * @since 2023-06-27
 */
public interface TjFragmentedSceneDetailMapper extends BaseMapper<TjFragmentedSceneDetail> {

    List<FragmentedScenesDetailVo> selectByCondition(SceneQueryDto sceneQueryDto);
}
