package net.wanji.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.dto.TjResourcesDetailDto;
import net.wanji.business.domain.vo.ResourcesDetailVo;
import net.wanji.business.entity.TjResourcesDetail;
import net.wanji.business.exception.BusinessException;

import java.util.List;

/**
 * <p>
 * 资源详情表 服务类
 * </p>
 *
 * @author wj
 * @since 2023-06-27
 */
public interface TjResourcesDetailService extends IService<TjResourcesDetail> {

    /**
     * 根据资源Id查询资源列表
     * @param resourceId
     * @return
     */
    List<ResourcesDetailVo> getDetailList(Integer resourceId, String name);

    /**
     * 保存资源详情
     * @param resourcesDetailDto
     * @return
     */
    boolean saveResourcesDetail(TjResourcesDetailDto resourcesDetailDto);

    /**
     * 删除资源
     * @param resourceDetailId
     * @return
     */
    boolean deleteByDetailId(Integer resourceDetailId) throws BusinessException;

    /**
     * 删除资源节点下所有资源
     * @param resourceId
     * @return
     */
    boolean deleteByResourceId(Integer resourceId) throws BusinessException;

    /**
     * 根据条件查询地图下拉列表
     * @param resourceType
     * @param roadType
     * @param roadPoint
     * @param laneNum
     * @return
     */
    List<TjResourcesDetail> getMapSelect(String resourceType, String roadType, String roadPoint, Integer laneNum);
}
