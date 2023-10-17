package net.wanji.business.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.wanji.business.mapper.LabelsMapper;
import net.wanji.business.domain.Labels;
import net.wanji.business.service.ILabelsService;

/**
 * labelsService业务层处理
 * 
 * @author wanji
 * @date 2023-10-16
 */
@Service
public class LabelsServiceImpl implements ILabelsService 
{
    @Autowired
    private LabelsMapper labelsMapper;

    /**
     * 查询labels
     * 
     * @param id labelsID
     * @return labels
     */
    @Override
    public Labels selectLabelsById(Long id)
    {
        return labelsMapper.selectLabelsById(id);
    }

    /**
     * 查询labels列表
     * 
     * @param labels labels
     * @return labels
     */
    @Override
    public List<Labels> selectLabelsList(Labels labels)
    {
        return labelsMapper.selectLabelsList(labels);
    }

    /**
     * 新增labels
     * 
     * @param labels labels
     * @return 结果
     */
    @Override
    public int insertLabels(Labels labels)
    {
        return labelsMapper.insertLabels(labels);
    }

    /**
     * 修改labels
     * 
     * @param labels labels
     * @return 结果
     */
    @Override
    public int updateLabels(Labels labels)
    {
        return labelsMapper.updateLabels(labels);
    }

    /**
     * 批量删除labels
     * 
     * @param ids 需要删除的labelsID
     * @return 结果
     */
    @Override
    public int deleteLabelsByIds(Long[] ids)
    {
        return labelsMapper.deleteLabelsByIds(ids);
    }

    /**
     * 删除labels信息
     * 
     * @param id labelsID
     * @return 结果
     */
    @Override
    public int deleteLabelsById(Long id)
    {
        return labelsMapper.deleteLabelsById(id);
    }
}
