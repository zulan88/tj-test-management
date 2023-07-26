package net.wanji.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.vo.CaseVerificationVo;
import net.wanji.business.domain.vo.CaseVo;
import net.wanji.business.domain.vo.SceneBaseVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;

import java.io.IOException;
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
public interface TjCaseService extends IService<TjCase> {

    /**
     * 测试用例列表页初始化
     *
     * @return
     */
    Map<String, Object> init();

    /**
     * 查询测试用例中包含的场景
     *
     * @param testType 测试类型
     * @param type     场景树类型
     * @param name     名称
     * @return
     */
    List<TjFragmentedScenes> selectScenesInCase(String testType, String type);

    /**
     * 获取场景基本信息
     *
     * @param fragmentedSceneId
     * @return
     */
    SceneBaseVo getSceneBaseInfo(Integer fragmentedSceneId) throws BusinessException;

    /**
     * 条件查询
     *
     * @param tjCaseDto
     * @return
     */
    List<CaseVo> getCases(TjCaseDto tjCaseDto);

    /**
     * 创建测试用例
     *
     * @param tjCaseDto
     * @return
     */
    boolean createCase(TjCaseDto tjCaseDto);


    /**
     * 保存仿真信息
     *
     * @param tjCaseDto
     * @return
     */
    Integer saveDetail(TjCaseDto tjCaseDto) throws BusinessException, IOException;


    /**
     * 校验轨迹点位
     *
     * @param id
     */
    boolean verifyTrajectory(Integer id) throws IOException;

    /**
     * 轨迹回放
     *
     * @param id
     */
    void playback(Integer id, String vehicleId, int action) throws BusinessException, IOException;

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
     * 加入测试任务
     *
     * @param ids
     */
    boolean joinTask(List<Integer> ids);

    /**
     * 查询用例仿真详情
     *
     * @param caseId
     * @return
     */
    CaseVerificationVo getDetail(Integer caseId) throws BusinessException;

}
