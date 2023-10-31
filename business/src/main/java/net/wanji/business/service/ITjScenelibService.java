package net.wanji.business.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.vo.ScenelibVo;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjScenelib;

/**
 * scenelibService接口
 * 
 * @author wanji
 * @date 2023-10-31
 */
public interface ITjScenelibService extends IService<TjScenelib>
{
    /**
     * 查询scenelib
     * 
     * @param id scenelibID
     * @return scenelib
     */
    public TjScenelib selectTjScenelibById(Long id);

    /**
     * 查询scenelib列表
     * 
     * @param tjScenelib scenelib
     * @return scenelib集合
     */
    public List<TjScenelib> selectTjScenelibList(TjScenelib tjScenelib);

    /**
     * 新增scenelib
     * 
     * @param tjScenelib scenelib
     * @return 结果
     */
    public int insertTjScenelib(TjScenelib tjScenelib);

    /**
     * 修改scenelib
     * 
     * @param tjScenelib scenelib
     * @return 结果
     */
    public int updateTjScenelib(TjScenelib tjScenelib);

    /**
     * 批量删除scenelib
     * 
     * @param ids 需要删除的scenelibID
     * @return 结果
     */
    public int deleteTjScenelibByIds(Long[] ids);

    /**
     * 删除scenelib信息
     * 
     * @param id scenelibID
     * @return 结果
     */
    public int deleteTjScenelibById(Long id);

    public List<ScenelibVo>selectScenelibVoList(ScenelibVo scenelibVo);

    boolean updateBatch(List<TjScenelib> sceneDetails);
}
