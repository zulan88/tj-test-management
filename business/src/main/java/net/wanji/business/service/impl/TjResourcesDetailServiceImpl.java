package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.domain.dto.TjResourcesDetailDto;
import net.wanji.business.domain.vo.ResourcesDetailVo;
import net.wanji.business.entity.TjResources;
import net.wanji.business.entity.TjResourcesDetail;
import net.wanji.business.mapper.TjResourcesDetailMapper;
import net.wanji.business.mapper.TjResourcesMapper;
import net.wanji.business.service.TjResourcesDetailService;
import net.wanji.common.utils.bean.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 资源详情表 服务实现类
 * </p>
 *
 * @author wj
 * @since 2023-06-27
 */
@Service
public class TjResourcesDetailServiceImpl extends ServiceImpl<TjResourcesDetailMapper, TjResourcesDetail>
        implements TjResourcesDetailService {

    @Autowired
    private TjResourcesMapper resourcesMapper;

    @Override
    public ResourcesDetailVo getDetailVo(Integer resourcesId) {
        QueryWrapper<TjResourcesDetail> queryWrapper = new QueryWrapper();
        queryWrapper.eq(ColumnName.RESOURCES_ID_COLUMN, resourcesId);
        TjResourcesDetail detail = this.getOne(queryWrapper);
        ResourcesDetailVo detailVo = new ResourcesDetailVo();
        if (!ObjectUtils.isEmpty(detail)) {
            BeanUtils.copyBeanProp(detailVo, detail);
        }
        return detailVo;
    }

    @Override
    public boolean saveResourcesDetail(TjResourcesDetailDto resourcesDetailDto) {
        TjResourcesDetail detail = new TjResourcesDetail();
        BeanUtils.copyBeanProp(detail, resourcesDetailDto);
        boolean success = this.saveOrUpdate(detail);
        return success;
    }

    @Override
    public List<TjResourcesDetail> getResourceSelect(String type) {
        List<TjResourcesDetail> details = new ArrayList<>();
        QueryWrapper<TjResources> resourceQueryWrapper = new QueryWrapper();
        resourceQueryWrapper.eq(ColumnName.TYPE_COLUMN, type).select(ColumnName.ID_COLUMN);
        List<TjResources> tjResources = resourcesMapper.selectList(resourceQueryWrapper);
        if (CollectionUtils.isEmpty(tjResources)) {
            return details;
        }

        QueryWrapper<TjResourcesDetail> detailQueryWrapper = new QueryWrapper();
        detailQueryWrapper.in(ColumnName.RESOURCES_ID_COLUMN,
                tjResources.stream().map(TjResources::getId).collect(Collectors.toList()));
        details = this.list(detailQueryWrapper);
        return details;
    }
}
