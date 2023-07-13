package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.UsingStatus;
import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.dto.TjResourcesDto;
import net.wanji.business.entity.TjResources;
import net.wanji.business.mapper.TjResourcesMapper;
import net.wanji.business.service.TjResourcesService;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.bean.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 资源信息表 服务实现类
 * </p>
 *
 * @author wj
 * @since 2023-06-27
 */
@Service
public class TjResourcesServiceImpl extends ServiceImpl<TjResourcesMapper, TjResources> implements TjResourcesService {


    @Override
    public List<TjResources> selectUsingResources(String type) {
        QueryWrapper<TjResources> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.STATUS_COLUMN, UsingStatus.ENABLE).eq(ColumnName.TYPE_COLUMN, type);
        List<TjResources> result = this.list(queryWrapper);
        return CollectionUtils.emptyIfNull(result).stream().sorted(Comparator.comparing(TjResources::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<TjResources> buildResourcesTree(List<TjResources> resources) {
        List<TjResources> returnList = new ArrayList<>();
        if (CollectionUtils.isEmpty(resources)) {
            return returnList;
        }
        List<Integer> tempList = new ArrayList<>();
        for (TjResources item : resources) {
            tempList.add(item.getId());
        }
        for (Iterator<TjResources> iterator = resources.iterator(); iterator.hasNext(); ) {
            TjResources resource = (TjResources) iterator.next();
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(resource.getParentId())) {
                recursionFn(resources, resource);
                returnList.add(resource);
            }
        }
        if (returnList.isEmpty()) {
            returnList = resources;
        }
        return returnList;
    }

    @Override
    public List<BusinessTreeSelect> buildResourcesTreeSelect(List<TjResources> resources) {
        List<TjResources> resourcesTrees = buildResourcesTree(resources);
        return resourcesTrees.stream().map(BusinessTreeSelect::new).collect(Collectors.toList());
    }

    @Override
    public boolean deleteResourcesById(Integer id) {
        TjResources resources = new TjResources();
        resources.setId(id);
        resources.setStatus(UsingStatus.DISABLE);
        resources.setUpdatedBy(SecurityUtils.getUsername());
        resources.setUpdatedDate(LocalDateTime.now());
        return this.updateById(resources);
    }

    @Override
    public boolean saveResource(TjResourcesDto resourcesDto) {
        TjResources TjResources = new TjResources();
        BeanUtils.copyBeanProp(TjResources, resourcesDto);
        if (ObjectUtils.isEmpty(TjResources.getId())) {
            TjResources.setCreatedBy(SecurityUtils.getUsername());
            TjResources.setCreatedDate(LocalDateTime.now());

        } else {
            TjResources.setUpdatedBy(SecurityUtils.getUsername());
            TjResources.setUpdatedDate(LocalDateTime.now());
        }
        return saveOrUpdate(TjResources);
    }

    /**
     * 递归列表
     *
     * @param list
     * @param t
     */
    private void recursionFn(List<TjResources> list, TjResources t) {
        // 得到子节点列表
        List<TjResources> childList = getChildList(list, t);
        t.setChildren(childList);
        for (TjResources tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<TjResources> getChildList(List<TjResources> list, TjResources t) {
        List<TjResources> tlist = new ArrayList<>();
        Iterator<TjResources> it = list.iterator();
        while (it.hasNext()) {
            TjResources n = (TjResources) it.next();
            if (n.getParentId().longValue() == t.getId().longValue()) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<TjResources> list, TjResources t) {
        return getChildList(list, t).size() > 0;
    }
}
