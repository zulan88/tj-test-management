package net.wanji.business.service;

import java.util.List;
import net.wanji.business.domain.Labels;

/**
 * labelsService接口
 * 
 * @author wanji
 * @date 2023-10-16
 */
public interface ILabelsService 
{
    /**
     * 查询labels
     * 
     * @param id labelsID
     * @return labels
     */
    public Labels selectLabelsById(Long id);

    /**
     * 查询labels列表
     * 
     * @param labels labels
     * @return labels集合
     */
    public List<Labels> selectLabelsList(Labels labels);

    /**
     * 新增labels
     * 
     * @param labels labels
     * @return 结果
     */
    public int insertLabels(Labels labels);

    /**
     * 修改labels
     * 
     * @param labels labels
     * @return 结果
     */
    public int updateLabels(Labels labels);

    /**
     * 批量删除labels
     * 
     * @param ids 需要删除的labelsID
     * @return 结果
     */
    public int deleteLabelsByIds(Long[] ids);

    /**
     * 删除labels信息
     * 
     * @param id labelsID
     * @return 结果
     */
    public int deleteLabelsById(Long id);
}
