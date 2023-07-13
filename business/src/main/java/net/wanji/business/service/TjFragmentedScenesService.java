package net.wanji.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.dto.SceneTreeTypeDto;
import net.wanji.business.domain.dto.TjFragmentedScenesDto;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.core.domain.TreeSelect;
import net.wanji.common.core.domain.entity.SysMenu;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 片段式场景表 服务类
 * </p>
 *
 * @author wj
 * @since 2023-06-27
 */
public interface TjFragmentedScenesService extends IService<TjFragmentedScenes> {

    /**
     * 初始化
     * @return
     */
    Map<String, Object> init();

    /**
     * 添加场景树类型
     * @param treeTypeDto
     * @return
     */
    boolean saveSceneTreeType(SceneTreeTypeDto treeTypeDto) throws BusinessException ;

    /**
     * 删除树类型
     * @param dictCode
     * @return
     */
    boolean deleteTreeType(Long dictCode) throws BusinessException;

    /**
     * 获取所有使用中的场景
     * @param type
     * @param name
     * @return
     */
    List<TjFragmentedScenes> selectUsingScenes(String type, String name);

    /**
     * 构建树结构
     * @param scenes
     * @return
     */
    List<TjFragmentedScenes> buildSceneTree(List<TjFragmentedScenes> scenes);

    /**
     * 构建前端所需要下拉树结构
     *
     * @param scenes 场景列表
     * @return 下拉树结构列表
     */
    List<BusinessTreeSelect> buildSceneTreeSelect(List<TjFragmentedScenes> scenes);

    /**
     * 根据id删除场景（逻辑删除）
     * @param id
     * @return
     */
    boolean deleteSceneById(Integer id) throws BusinessException;

    /**
     * 保存场景树
     * @param fragmentedScenesDto
     * @return
     */
    boolean saveSceneTree(TjFragmentedScenesDto fragmentedScenesDto) ;
}
