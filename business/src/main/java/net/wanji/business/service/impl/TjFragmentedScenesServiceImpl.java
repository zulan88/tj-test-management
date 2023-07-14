package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.UsingStatus;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.dto.SceneTreeTypeDto;
import net.wanji.business.domain.dto.TjFragmentedScenesDto;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjFragmentedScenesMapper;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.business.service.TjFragmentedScenesService;
import net.wanji.common.core.domain.entity.SysDictData;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.bean.BeanUtils;
import net.wanji.system.service.ISysDictDataService;
import net.wanji.system.service.ISysDictTypeService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 片段式场景表 服务实现类
 * </p>
 *
 * @author wj
 * @since 2023-06-27
 */
@Service
public class TjFragmentedScenesServiceImpl extends ServiceImpl<TjFragmentedScenesMapper, TjFragmentedScenes>
        implements TjFragmentedScenesService {

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private TjFragmentedSceneDetailService sceneDetailService;

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private TjFragmentedScenesMapper fragmentedScenesMapper;

    @Override
    public Map<String, Object> init() {
        List<SysDictData> sysDictData = dictTypeService.selectDictDataByType(SysType.SCENE_TREE_TYPE);
        Map<String, Object> result = new HashMap<>(1);
        result.put("sceneTreeType", sysDictData);
        return result;
    }

    @Override
    public boolean saveSceneTreeType(SceneTreeTypeDto treeTypeDto) throws BusinessException {
        List<SysDictData> sysDictData = dictTypeService.selectDictDataByType(SysType.SCENE_TREE_TYPE);
        SysDictData dictData = null;
        if (ObjectUtils.isEmpty(treeTypeDto.getDictCode())) {
            // 添加
            List<String> labelList = CollectionUtils.emptyIfNull(sysDictData).stream().map(SysDictData::getDictLabel)
                    .collect(Collectors.toList());
            if (labelList.contains(treeTypeDto.getDictLabel())) {
                throw new BusinessException("名称重复");
            }
            dictData = new SysDictData();
            BeanUtils.copyBeanProp(dictData, treeTypeDto);
            long value = CollectionUtils.emptyIfNull(sysDictData).stream().mapToLong(SysDictData::getDictSort).max()
                    .orElse(0) + 1;
            dictData.setDictSort(value);
            dictData.setDictValue(String.valueOf(value));
            dictData.setDictType(SysType.SCENE_TREE_TYPE);
            dictData.setIsDefault(YN.N);
            dictData.setStatus(String.valueOf(UsingStatus.ENABLE));
            dictData.setCreateBy(SecurityUtils.getUsername());
            dictData.setCreateTime(new Date());
            if (dictDataService.insertDictData(dictData) < 0) {
                throw new BusinessException("添加失败");
            }
        } else {
            SysDictData originData = dictDataService.selectDictDataById(treeTypeDto.getDictCode());
            if (ObjectUtils.isEmpty(originData)) {
                throw new BusinessException("未查询到对应类型");
            }
            // 修改
            List<String> otherNames = CollectionUtils.emptyIfNull(sysDictData).stream().filter(item ->
                            !Objects.equals(item.getDictCode(), treeTypeDto.getDictCode())).map(SysDictData::getDictLabel)
                    .collect(Collectors.toList());
            if (otherNames.contains(treeTypeDto.getDictLabel())) {
                throw new BusinessException("名称重复");
            }
            originData.setCssClass(treeTypeDto.getCssClass());
            originData.setDictLabel(treeTypeDto.getDictLabel());
            originData.setRemark(treeTypeDto.getRemark());
            originData.setUpdateBy(SecurityUtils.getUsername());
            originData.setUpdateTime(new Date());
            if (dictDataService.updateDictData(originData) < 0) {
                throw new BusinessException("修改失败");
            }
        }
        return true;
    }

    @Override
    public boolean deleteTreeType(Long dictCode) throws BusinessException {
        // todo 删除逻辑
        SysDictData dictData = dictDataService.selectDictDataById(dictCode);
        if (ObjectUtils.isEmpty(dictData)) {
            return false;
        }
        // 1.检查该类型下的所有场景是否存在测试用例
        QueryWrapper<TjFragmentedScenes> scenesQueryWrapper = new QueryWrapper<>();
        scenesQueryWrapper.eq(ColumnName.TYPE_COLUMN, dictData.getDictValue()).eq(ColumnName.IS_FOLDER_COLUMN, YN.N_INT);
        List<TjFragmentedScenes> scenesList = this.list(scenesQueryWrapper);
        List<Integer> sceneIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(scenesList)) {
            sceneIds = scenesList.stream().map(TjFragmentedScenes::getId).collect(Collectors.toList());
            QueryWrapper<TjCase> caseQueryWrapper = new QueryWrapper<>();
            caseQueryWrapper.in(ColumnName.SCENE_DETAIL_ID_COLUMN, sceneIds);
            List<TjCase> caseList = caseMapper.selectList(caseQueryWrapper);
            if (CollectionUtils.isNotEmpty(caseList)) {
                throw new BusinessException("此类型下存在测试用例，无法删除");
            }
        }
        // 2.删除场景详情
        if (CollectionUtils.isNotEmpty(sceneIds)) {
            QueryWrapper<TjFragmentedSceneDetail> detailQueryWrapper = new QueryWrapper<>();
            detailQueryWrapper.in(ColumnName.SCENE_DETAIL_ID_COLUMN, sceneIds);
            boolean removeDetail = sceneDetailService.remove(detailQueryWrapper);
            if (!removeDetail) {
                throw new BusinessException("删除详情失败");
            }
        }
        // 3.删除场景
        QueryWrapper<TjFragmentedScenes> scenesDeleteWrapper = new QueryWrapper<>();
        scenesDeleteWrapper.eq(ColumnName.TYPE_COLUMN, dictData.getDictValue());
        this.remove(scenesDeleteWrapper);
        // 4.删除场景树类型
        dictDataService.deleteDictDataByIds(new Long[]{dictCode});
        return true;
    }

    @Override
    public List<TjFragmentedScenes> selectUsingScenes(String type) {
        List<TjFragmentedScenes> result = fragmentedScenesMapper.selectByCondition(type);
        return CollectionUtils.emptyIfNull(result).stream().sorted(Comparator.comparing(TjFragmentedScenes::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<TjFragmentedScenes> buildSceneTree(List<TjFragmentedScenes> scenes) {
        List<TjFragmentedScenes> returnList = new ArrayList<>();
        if (CollectionUtils.isEmpty(scenes)) {
            return returnList;
        }
        List<Integer> tempList = new ArrayList<>();
        for (TjFragmentedScenes item : scenes) {
            tempList.add(item.getId());
        }
        for (Iterator<TjFragmentedScenes> iterator = scenes.iterator(); iterator.hasNext(); ) {
            TjFragmentedScenes scene = iterator.next();
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(scene.getParentId())) {
                recursionFn(scenes, scene);
                returnList.add(scene);
            }
        }
        if (returnList.isEmpty()) {
            returnList = scenes;
        }
        return returnList;
    }

    @Override
    public List<BusinessTreeSelect> buildSceneTreeSelect(List<TjFragmentedScenes> scenes, String name) {
        List<TjFragmentedScenes> sceneTrees = buildSceneTree(scenes);
        return sceneTrees.stream().map(BusinessTreeSelect::new).map(tree -> fuzzySearch(tree, name))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteSceneById(Integer id) throws BusinessException {
        TjFragmentedScenes scenes = this.getById(id);
        if (ObjectUtils.isEmpty(scenes)) {
            throw new BusinessException("节点不存在");
        }
        // 1.删除场景
        if (YN.N_INT == scenes.getIsFolder()) {
            int count = caseMapper.selectCountBySceneId(scenes.getId());
            if (count > 0) {
                throw new BusinessException("当前场景下存在测试用例，无法删除");
            }
            QueryWrapper<TjFragmentedSceneDetail> detailQueryWrapper = new QueryWrapper<>();
            detailQueryWrapper.eq(ColumnName.SCENE_DETAIL_ID_COLUMN, scenes.getId());
            sceneDetailService.remove(detailQueryWrapper);
            this.removeById(id);
            return true;
        }
        // 2.删除文件夹
        // 2.1.查询文件夹下所有场景和文件夹
        List<TjFragmentedScenes> deleteCollector = new ArrayList<>();
        selectChildrenFromFolder(scenes.getId(), deleteCollector);
        // 2.2.筛选掉文件夹，校验所有场景节点是否存在测试用例，没有测试用例则删除所有场景详情
        List<Integer> sceneIds = deleteCollector.stream().filter(item -> YN.N_INT == item.getIsFolder())
                .map(TjFragmentedScenes::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(sceneIds)) {
            QueryWrapper<TjCase> queryWrapper = new QueryWrapper<>();
            queryWrapper.in(ColumnName.SCENE_DETAIL_ID_COLUMN, sceneIds);
            List<TjCase> cases = caseMapper.selectList(queryWrapper);
            if (CollectionUtils.isNotEmpty(cases)) {
                throw new BusinessException("当前文件夹下的场景存在测试用例，无法删除");
            }
            QueryWrapper<TjFragmentedSceneDetail> detailQueryWrapper = new QueryWrapper<>();
            detailQueryWrapper.in(ColumnName.SCENE_DETAIL_ID_COLUMN, sceneIds);
            sceneDetailService.remove(detailQueryWrapper);
        }
        // 2.3.删掉文件夹及其所有子节点
        deleteCollector.add(scenes);
        QueryWrapper<TjFragmentedScenes> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.in(ColumnName.ID_COLUMN,
                deleteCollector.stream().map(TjFragmentedScenes::getId).collect(Collectors.toList()));
        return this.remove(deleteWrapper);
    }

    private void selectChildrenFromFolder(Integer id, List<TjFragmentedScenes> collector) {
        QueryWrapper<TjFragmentedScenes> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.PARENT_ID_COLUMN, id);
        List<TjFragmentedScenes> childrenList = this.list(queryWrapper);

        if (CollectionUtils.isNotEmpty(childrenList)) {
            collector.addAll(childrenList);
            for (TjFragmentedScenes scenes : childrenList) {
                selectChildrenFromFolder(scenes.getId(), collector);
            }
        }
    }

    @Override
    public boolean saveSceneTree(TjFragmentedScenesDto fragmentedScenesDto) {
        TjFragmentedScenes tjFragmentedScenes;
        if (ObjectUtils.isEmpty(fragmentedScenesDto.getId())) {
            tjFragmentedScenes = new TjFragmentedScenes();
            BeanUtils.copyBeanProp(tjFragmentedScenes, fragmentedScenesDto);
            tjFragmentedScenes.setStatus(UsingStatus.ENABLE);
            tjFragmentedScenes.setCreatedBy(SecurityUtils.getUsername());
            tjFragmentedScenes.setCreatedDate(LocalDateTime.now());
        } else {
            tjFragmentedScenes = this.getById(fragmentedScenesDto.getId());
            tjFragmentedScenes.setName(fragmentedScenesDto.getName());
            tjFragmentedScenes.setParentId(fragmentedScenesDto.getParentId());
            tjFragmentedScenes.setLevel(fragmentedScenesDto.getLevel());
            tjFragmentedScenes.setUpdatedBy(SecurityUtils.getUsername());
            tjFragmentedScenes.setUpdatedDate(LocalDateTime.now());
        }
        return saveOrUpdate(tjFragmentedScenes);
    }

    /**
     * 递归列表
     *
     * @param list
     * @param t
     */
    private void recursionFn(List<TjFragmentedScenes> list, TjFragmentedScenes t) {
        // 得到子节点列表
        List<TjFragmentedScenes> childList = getChildList(list, t);
        t.setChildren(childList);
        for (TjFragmentedScenes tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<TjFragmentedScenes> getChildList(List<TjFragmentedScenes> list, TjFragmentedScenes t) {
        List<TjFragmentedScenes> tlist = new ArrayList<>();
        Iterator<TjFragmentedScenes> it = list.iterator();
        while (it.hasNext()) {
            TjFragmentedScenes n = (TjFragmentedScenes) it.next();
            if (n.getParentId().longValue() == t.getId().longValue()) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<TjFragmentedScenes> list, TjFragmentedScenes t) {
        return getChildList(list, t).size() > 0;
    }

    private BusinessTreeSelect fuzzySearch(BusinessTreeSelect node, String query) {
        if (node.getName().contains(query)) {
            // 如果这个节点匹配查询，返回这个节点和它的所有子节点...
            return node;
        } else {
            // 如果这个节点不匹配查询，对它的每个子节点执行模糊查询...
            List<BusinessTreeSelect> matchingChildren = new ArrayList<>();
            for (BusinessTreeSelect child : node.getChildren()) {
                BusinessTreeSelect matchingChild = fuzzySearch(child, query);
                if (matchingChild != null) {
                    matchingChildren.add(matchingChild);
                }
            }
            if (!matchingChildren.isEmpty()) {
                // 如果有任何匹配的子节点，创建一个新的节点，包含这些子节点...
                BusinessTreeSelect newNode = new BusinessTreeSelect();
                newNode.setId(node.getId());
                newNode.setParentId(node.getParentId());
                newNode.setName(node.getName());
                newNode.setChildren(matchingChildren);
                return newNode;
            } else {
                // 如果没有任何匹配的子节点，返回null...
                return null;
            }
        }
    }


}
