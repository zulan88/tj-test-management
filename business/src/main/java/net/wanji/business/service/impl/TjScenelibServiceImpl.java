package net.wanji.business.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants;
import net.wanji.business.domain.vo.ScenelibVo;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.mapper.TjFragmentedSceneDetailMapper;
import net.wanji.common.utils.CounterUtil;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.annotations.Param;
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

    @Autowired
    private TjFragmentedSceneDetailMapper sceneDetailMapper;

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
        List<String> labellist = new ArrayList<>();
        if(tjScenelib.getLabels().split(",").length>0) {
            for (String id : tjScenelib.getLabels().split(",")) {
                labellist.addAll(sceneDetailMapper.getalllabel(id));
            }
        }
        tjScenelib.setAllStageLabels(CollectionUtils.isNotEmpty(labellist)
                ? labellist.stream().distinct().collect(Collectors.joining(","))
                : null);
        tjScenelib.setCreateBy("admin");
        tjScenelib.setCreateDatetime(LocalDateTime.now());
        tjScenelib.setSceneSource(0);
        tjScenelib.setSceneStatus(1);
        tjScenelib.setNumber(StringUtils.format(Constants.ContentTemplate.SCENE_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                CounterUtil.getRandomChar()));
        return tjScenelibMapper.insertTjScenelib(tjScenelib);
    }

    @Override
    public boolean insertTjScenelibBatch(List<TjScenelib> tjScenelibs) {
        for (TjScenelib tjScenelib:tjScenelibs){
            List<String> labellist = new ArrayList<>();
            if(tjScenelib.getLabels().split(",").length>0) {
                for (String id : tjScenelib.getLabels().split(",")) {
                    labellist.addAll(sceneDetailMapper.getalllabel(id));
                }
            }
            tjScenelib.setAllStageLabels(CollectionUtils.isNotEmpty(labellist)
                    ? labellist.stream().distinct().collect(Collectors.joining(","))
                    : null);
            tjScenelib.setCreateBy("admin");
            tjScenelib.setCreateDatetime(LocalDateTime.now());
            tjScenelib.setNumber(StringUtils.format(Constants.ContentTemplate.SCENE_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                    CounterUtil.getRandomChar()));
        }
        return this.saveBatch(tjScenelibs);
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
        List<String> labellist = new ArrayList<>();
        if(tjScenelib.getLabels()!=null&&tjScenelib.getLabels().split(",").length>0) {
            for (String id : tjScenelib.getLabels().split(",")) {
                labellist.addAll(sceneDetailMapper.getalllabel(id));
            }
        }
        tjScenelib.setAllStageLabels(CollectionUtils.isNotEmpty(labellist)
                ? labellist.stream().distinct().collect(Collectors.joining(","))
                : null);
        tjScenelib.setUpdateBy("admin");
        tjScenelib.setUpdateDatetime(LocalDateTime.now());
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

    @Override
    public List<ScenelibVo> selectTjSceneDetailListAnd(List<Integer> labellist, Integer treeId) {
        return tjScenelibMapper.selectTjSceneDetailListAnd(labellist, treeId);
    }

    @Override
    public List<ScenelibVo> selectTjSceneDetailListOr(List<Integer> labellist, Integer treeId) {
        return tjScenelibMapper.selectTjSceneDetailListOr(labellist, treeId);
    }
}
