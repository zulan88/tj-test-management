package net.wanji.business.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.domain.vo.ScenelibVo;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.wanji.business.mapper.TjScenelibMapper;
import net.wanji.business.entity.TjScenelib;
import net.wanji.business.service.ITjScenelibService;

/**
 * scenelibService业务层处理
 * 
 * @author wanji
 * @date 2023-10-31
 */
@Service
public class TjScenelibServiceImpl extends ServiceImpl<TjScenelibMapper,TjScenelib> implements ITjScenelibService
{
    @Autowired
    private TjScenelibMapper tjScenelibMapper;

    /**
     * 查询scenelib
     * 
     * @param id scenelibID
     * @return scenelib
     */
    @Override
    public TjScenelib selectTjScenelibById(Long id)
    {
        return tjScenelibMapper.selectTjScenelibById(id);
    }

    /**
     * 查询scenelib列表
     * 
     * @param tjScenelib scenelib
     * @return scenelib
     */
    @Override
    public List<TjScenelib> selectTjScenelibList(TjScenelib tjScenelib)
    {
        return tjScenelibMapper.selectTjScenelibList(tjScenelib);
    }

    /**
     * 新增scenelib
     * 
     * @param tjScenelib scenelib
     * @return 结果
     */
    @Override
    public int insertTjScenelib(TjScenelib tjScenelib)
    {
        return tjScenelibMapper.insertTjScenelib(tjScenelib);
    }

    /**
     * 修改scenelib
     * 
     * @param tjScenelib scenelib
     * @return 结果
     */
    @Override
    public int updateTjScenelib(TjScenelib tjScenelib)
    {
        return tjScenelibMapper.updateTjScenelib(tjScenelib);
    }

    /**
     * 批量删除scenelib
     * 
     * @param ids 需要删除的scenelibID
     * @return 结果
     */
    @Override
    public int deleteTjScenelibByIds(Long[] ids)
    {
        return tjScenelibMapper.deleteTjScenelibByIds(ids);
    }

    /**
     * 删除scenelib信息
     * 
     * @param id scenelibID
     * @return 结果
     */
    @Override
    public int deleteTjScenelibById(Long id)
    {
        return tjScenelibMapper.deleteTjScenelibById(id);
    }

    @Override
    public List<ScenelibVo> selectScenelibVoList(ScenelibVo scenelibVo) {
        return tjScenelibMapper.selectScenelibVoList(scenelibVo);
    }

    @Override
    public boolean updateBatch(List<TjScenelib> scenelibs) {
        return this.updateBatchById(scenelibs);
    }
}
