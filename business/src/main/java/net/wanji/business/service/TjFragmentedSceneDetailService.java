package net.wanji.business.service;

import net.wanji.business.domain.dto.SceneQueryDto;
import net.wanji.business.domain.dto.TjFragmentedSceneDetailDto;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.exception.BusinessException;

import java.util.List;

/**
 * <p>
 * 片段式场景定义 服务类
 * </p>
 *
 * @author wj
 * @since 2023-06-27
 */
public interface TjFragmentedSceneDetailService extends IService<TjFragmentedSceneDetail> {
    /**
     * 根据场景查询场景详情
     * @param id
     * @return
     */
    FragmentedScenesDetailVo getDetailVo(Integer id) throws BusinessException;

    /**
     * 保存场景详情
     * @param sceneDetailDto
     * @return
     */
    boolean saveSceneDetail(TjFragmentedSceneDetailDto sceneDetailDto) throws BusinessException;

    List<FragmentedScenesDetailVo> selectScene(SceneQueryDto queryDto) throws BusinessException;

    boolean deleteSceneDetail(Integer id) throws BusinessException ;
}
