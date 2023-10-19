package net.wanji.business.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.baomidou.mybatisplus.extension.service.IService;

import net.wanji.business.domain.PartConfigSelect;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.dto.CaseQueryDto;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.vo.CasePageVo;
import net.wanji.business.domain.vo.CaseVerificationVo;
import net.wanji.business.domain.vo.CaseVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.core.domain.SimpleSelect;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author wj
 * @since 2023-06-29
 */
public interface TjCaseService extends IService<TjCase> {

    /**
     * 测试用例列表页初始化
     *
     * @return
     */
    Map<String, List<SimpleSelect>> init();

    /**
     * 配置页初始化
     *
     * @return sceneDetailId
     * @return caseId
     */
    Map<String, Object> initEditPage(Integer sceneDetailId, Integer caseId) throws BusinessException;

    /**
     * 分页列表
     * @param caseQueryDto
     * @return
     * @throws BusinessException
     */
    List<CasePageVo> pageList(CaseQueryDto caseQueryDto);

    /**
     * 获取用例详情
     *
     * @param caseId
     * @return
     */
    CaseInfoBo getCaseDetail(Integer caseId);


    /**
     * 获取配置信息
     * @param caseId
     * @param sceneTrajectoryBo
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    List<PartConfigSelect> getConfigSelect(Integer caseId, SceneTrajectoryBo sceneTrajectoryBo, boolean deviceConfig);

    /**
     * 获取配置信息
     * @param caseId
     * @param sceneTrajectoryBo
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    List<PartConfigSelect> getConfigSelect(Integer caseId, SceneTrajectoryBo sceneTrajectoryBo);

    /**
     * 查询测试用例中包含的场景
     *
     * @param testType 测试类型
     * @param type     场景树类型
     * @return
     */
    List<TjFragmentedScenes> selectScenesInCase(String testType, String type);

    /**
     * 条件查询
     *
     * @param tjCaseDto
     * @return
     */
    List<CaseVo> getCases(CaseQueryDto caseQueryDto);

    /**
     * 创建测试用例
     *
     * @param tjCaseDto
     * @return
     */
    Integer saveCase(TjCaseDto tjCaseDto) throws BusinessException;

    /**
     * 轨迹回放
     *
     * @param id            用例ID
     * @param participantId 参与者ID
     * @param action        操作 PlaybackAction
     * @throws BusinessException
     * @throws IOException
     */
    void playback(Integer id, String participantId, int action) throws BusinessException, IOException;

    /**
     * 克隆用例
     *
     * @param tjCaseDto
     * @return
     */
    boolean cloneCase(TjCaseDto tjCaseDto);

    /**
     * 删除用例
     *
     * @param tjCaseDto
     * @return
     */
    boolean deleteCase(TjCaseDto tjCaseDto);

    /**
     * 导出
     *
     * @param cases
     * @param fileName
     * @throws IOException
     */
    void exportCases(List<TjCase> cases, String fileName) throws IOException;

    /**
     * 修改状态
     *
     * @param tjCaseDto
     */
    boolean updateStatus(TjCaseDto tjCaseDto) throws BusinessException;


    /**
     * 查询用例仿真详情
     *
     * @param caseId
     * @return
     */
    CaseVerificationVo getSimulationDetail(Integer caseId) throws BusinessException;

    /**
     * 获取配置详情
     * @param caseId
     * @return
     * @throws BusinessException
     */
    List<PartConfigSelect> getConfigDetail(Integer caseId) throws BusinessException, InterruptedException, ExecutionException;

    /**
     * 获取测试任务配置详情
     * @param caseId
     * @return
     * @throws BusinessException
     */
    List<PartConfigSelect> getTaskConfigDetail(Integer caseId) throws BusinessException, InterruptedException, ExecutionException;



}
