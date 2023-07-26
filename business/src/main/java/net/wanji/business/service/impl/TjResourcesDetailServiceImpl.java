package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.FileTypeEnum;
import net.wanji.business.domain.dto.TjResourcesDetailDto;
import net.wanji.business.domain.vo.ResourcesDetailVo;
import net.wanji.business.entity.TjResources;
import net.wanji.business.entity.TjResourcesDetail;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjResourcesDetailMapper;
import net.wanji.business.mapper.TjResourcesMapper;
import net.wanji.business.service.TjResourcesDetailService;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.bean.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private TjCaseMapper caseMapper;


    @Override
    public List<ResourcesDetailVo> getDetailList(Integer resourceId, String name) {
        QueryWrapper<TjResourcesDetail> queryWrapper = new QueryWrapper();
        queryWrapper.eq(ColumnName.RESOURCES_ID_COLUMN, resourceId);
        if (StringUtils.isNotEmpty(name)) {
            queryWrapper.like(ColumnName.NAME_COLUMN, name);
        }
        List<TjResourcesDetail> details = this.list(queryWrapper);
        return CollectionUtils.emptyIfNull(details).stream().map(detail -> {
            ResourcesDetailVo detailVo = new ResourcesDetailVo();
            BeanUtils.copyBeanProp(detailVo, detail);
            return detailVo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean saveResourcesDetail(TjResourcesDetailDto resourcesDetailDto) {
        if (ObjectUtils.isEmpty(resourcesDetailDto.getId())) {
            TjResourcesDetail detail = new TjResourcesDetail();
            BeanUtils.copyBeanProp(detail, resourcesDetailDto);
            detail.setCode(StringUtils.generateRandomString());
            detail.setFormat(FileTypeEnum.getTypeByExt(FilenameUtils.getExtension(detail.getFilePath())));
            detail.setCreatedBy(SecurityUtils.getUsername());
            detail.setCreatedDate(LocalDateTime.now());
            return this.save(detail);
        } else {
            TjResourcesDetail resourcesDetail = this.getById(resourcesDetailDto.getId());
            resourcesDetail.setName(resourcesDetailDto.getName());
            resourcesDetail.setFilePath(resourcesDetailDto.getFilePath());
            resourcesDetail.setFormat(FileTypeEnum.getTypeByExt(FilenameUtils.getExtension(resourcesDetail.getFilePath())));
            resourcesDetail.setImgPath(resourcesDetailDto.getImgPath());
            resourcesDetail.setUpdatedBy(SecurityUtils.getUsername());
            resourcesDetail.setUpdatedDate(LocalDateTime.now());
            return this.updateById(resourcesDetail);
        }
    }

    @Override
    public boolean deleteByDetailId(Integer resourceDetailId) throws BusinessException {
        List<Integer> usingDetailIds = caseMapper.selectUsingResources();
        if (CollectionUtils.emptyIfNull(usingDetailIds).contains(resourceDetailId)) {
            throw new BusinessException("存在使用此资源的测试用例");
        }
        return this.removeById(resourceDetailId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteByResourceId(Integer resourceId) throws BusinessException {
        List<Integer> usingDetailIds = caseMapper.selectUsingResources();
        QueryWrapper<TjResourcesDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.RESOURCES_ID_COLUMN, resourceId);
        List<TjResourcesDetail> resourceDetails = this.list(queryWrapper);
        List<Integer> ids = CollectionUtils.emptyIfNull(resourceDetails).stream().map(TjResourcesDetail::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(usingDetailIds) && CollectionUtils.isNotEmpty(ids)) {
            for (Integer usingId : usingDetailIds) {
                if (ids.contains(usingId)) {
                    throw new BusinessException("此资源节点中存在使用中的资源");
                }
            }
        }
        Map<String, Object> param = new HashMap<>(1);
        param.put(ColumnName.RESOURCES_ID_COLUMN, resourceId);
        return this.removeByMap(param);
    }

    @Override
    public List<TjResourcesDetail> getMapSelect(String resourceType, String roadType, String roadPoint,
                                                Integer laneNum) {
        List<TjResourcesDetail> details = new ArrayList<>();
        QueryWrapper<TjResources> resourceQueryWrapper = new QueryWrapper();
        resourceQueryWrapper.eq(ColumnName.TYPE_COLUMN, resourceType).select(ColumnName.ID_COLUMN);
        List<TjResources> tjResources = resourcesMapper.selectList(resourceQueryWrapper);
        if (CollectionUtils.isEmpty(tjResources)) {
            return details;
        }
        QueryWrapper<TjResourcesDetail> detailQueryWrapper = new QueryWrapper();
        detailQueryWrapper.in(ColumnName.RESOURCES_ID_COLUMN,
                tjResources.stream().map(TjResources::getId).collect(Collectors.toList()));
        details = this.list(detailQueryWrapper);
        return details.stream().filter(item -> {
            boolean a = StringUtils.isEmpty(roadType) || (!ObjectUtils.isEmpty(item.getAttribute1())
                    && StringUtils.equals(item.getAttribute1(), roadType));
            boolean b = StringUtils.isEmpty(roadPoint) || (!ObjectUtils.isEmpty(item.getAttribute2())
                    && StringUtils.equals(item.getAttribute2(), roadPoint));
            boolean c = ObjectUtils.isEmpty(laneNum) || (!ObjectUtils.isEmpty(item.getAttribute3())
                    && StringUtils.equals(item.getAttribute3(), String.valueOf(laneNum)));
            return a && b && c;
        }).collect(Collectors.toList());
    }
}
