package net.wanji.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.vo.CasePartConfigVo;
import net.wanji.business.entity.TjCasePartConfig;
import net.wanji.business.exception.BusinessException;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author wj
 * @since 2023-06-29
 */
public interface TjCasePartConfigService extends IService<TjCasePartConfig> {
    /**
     * 查询用例角色配置
     *
     * @param caseId
     * @return
     */
    Map<String, List<CasePartConfigVo>> getConfigInfo(Integer caseId) throws BusinessException;

    /**
     * 场景点位信息转换为配置信息
     * @param sceneTrajectoryBo
     * @return
     */
    Map<String, List<CasePartConfigVo>> trajectory2Config(SceneTrajectoryBo sceneTrajectoryBo);

}
