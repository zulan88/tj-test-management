package net.wanji.business.service;

import net.wanji.business.domain.dto.TjFragmentedSceneDetailDto;
import net.wanji.business.domain.dto.TjResourcesDetailDto;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.domain.vo.ResourcesDetailVo;
import net.wanji.business.entity.TjResourcesDetail;
import com.baomidou.mybatisplus.extension.service.IService;

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
     * 根据资源Id查询资源详情
     * @param resourcesId
     * @return
     */
    ResourcesDetailVo getDetailVo(Integer resourcesId);

    /**
     * 保存资源详情
     * @param resourcesDetailDto
     * @return
     */
    boolean saveResourcesDetail(TjResourcesDetailDto resourcesDetailDto);

    /**
     * 查询资源列表
     * @param type
     * @return
     */
    List<TjResourcesDetail> getResourceSelect(String type);
}
