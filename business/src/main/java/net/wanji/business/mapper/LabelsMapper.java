package net.wanji.business.mapper;

import java.util.List;
import net.wanji.business.domain.Labels;

/**
 * labelsMapper接口
 * 
 * @author wanji
 * @date 2023-10-16
 */
public interface LabelsMapper 
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
     * 删除labels
     * 
     * @param id labelsID
     * @return 结果
     */
    public int deleteLabelsById(Long id);

    /**
     * 批量删除labels
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLabelsByIds(Long[] ids);
}
