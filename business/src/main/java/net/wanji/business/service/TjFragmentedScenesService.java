package net.wanji.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.dto.SceneTreeTypeDto;
import net.wanji.business.domain.dto.TjFragmentedSceneDetailDto;
import net.wanji.business.domain.dto.TjFragmentedScenesDto;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.core.domain.SimpleSelect;

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
    Map<String, List<SimpleSelect>> init();

    /**
     * 初始化编辑页
     * @param type
     * @return
     */
    Map<String, Object> initEditPage();

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
    List<TjFragmentedScenes> selectUsingScenes(String type);

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
    List<BusinessTreeSelect> buildSceneTreeSelect(List<TjFragmentedScenes> scenes, String name);

    /**
     * 根据id删除场景（逻辑删除）
     * @param id
     * @return
     */
    boolean deleteSceneById(Integer id) throws BusinessException;

    /**
     * 克隆场景
     * @param id
     * @return
     * @throws BusinessException
     */
    Integer cloneScene(Integer id) throws BusinessException;

    /**
     * 保存场景树
     * @param fragmentedScenesDto
     * @return
     */
    boolean saveSceneTree(TjFragmentedScenesDto fragmentedScenesDto);

    /**
     * 场景构建完成
     * @param sceneDetailDto
     * @return
     */
    boolean completeScene(TjFragmentedSceneDetailDto sceneDetailDto) throws BusinessException;
}
