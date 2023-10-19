package net.wanji.business.mapper;

import net.wanji.business.domain.dto.SceneQueryDto;
import net.wanji.business.domain.dto.TjFragmentedSceneDetailDto;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.domain.vo.SceneDetailVo;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

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

    List<Integer> selectUsingResources();

    int deleteBySceneIds(@Param("sceneIds") List<Integer> sceneIds);

    List<SceneDetailVo> selectTjFragmentedSceneDetailList(SceneDetailVo sceneDetailVo);
}
