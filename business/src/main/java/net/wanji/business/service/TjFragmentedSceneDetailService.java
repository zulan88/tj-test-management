package net.wanji.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.dto.SceneDebugDto;
import net.wanji.business.domain.dto.SceneQueryDto;
import net.wanji.business.domain.dto.TjFragmentedSceneDetailDto;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.domain.vo.SceneDetailVo;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.exception.BusinessException;

import java.io.IOException;
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

    /**
     * 列表查询场景卡片
     * @param queryDto
     * @return
     * @throws BusinessException
     */
    List<FragmentedScenesDetailVo> selectScene(SceneQueryDto queryDto) throws BusinessException;

    /**
     * 删除场景详情
     * @param id
     * @return
     * @throws BusinessException
     */
    boolean deleteSceneDetail(Integer id) throws BusinessException;

    /**
     * 在线调试
     * @param sceneDebugDto
     * @throws BusinessException
     * @throws IOException
     */
    void debugging(SceneDebugDto sceneDebugDto) throws BusinessException, IOException;

    List<SceneDetailVo> selectTjFragmentedSceneDetailList(SceneDetailVo sceneDetailVo) throws BusinessException;

}
