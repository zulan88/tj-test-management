package net.wanji.business.service;

import net.wanji.business.domain.PartConfigSelect;
import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.vo.CaseRealTestVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.RealVehicleVerificationPageVo;
import net.wanji.business.exception.BusinessException;

import java.io.IOException;
import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/24 9:57
 * @Descriptoin:
 */

public interface TestingService {

    /**
     * 获取测试服务列表
     *
     * @param caseDto
     * @return
     * @throws BusinessException
     */
    List<CaseConfigBo> list(TjCaseDto caseDto) throws BusinessException;

    /**
     * 查询配置详情
     * @param sceneDetailId
     * @param caseId
     * @throws BusinessException
     */
    List<PartConfigSelect> configDetail(Integer sceneDetailId, Integer caseId) throws BusinessException;

    /**
     * 配置角色
     *
     * @param caseDto
     * @return
     * @throws BusinessException
     */
    boolean configRole(TjCaseDto caseDto) throws BusinessException;

    /**
     * 修改状态
     *
     * @param caseId
     * @return
     * @throws BusinessException
     */
    boolean updateState(Integer caseId) throws BusinessException;

    /**
     * 删除
     *
     * @param caseId
     * @return
     * @throws BusinessException
     */
    boolean delete(Integer caseId) throws BusinessException;

    /**
     * 配置设备
     *
     * @param caseDto
     * @return
     * @throws BusinessException
     */
    boolean configDevice(TjCaseDto caseDto) throws BusinessException;

    /**
     * 获取用例中参与者状态
     *
     * @param caseId
     * @return
     */
    RealVehicleVerificationPageVo getStatus(Integer caseId) throws BusinessException;

    /**
     * 准备（开始倒计时）
     *
     * @param caseId
     * @return
     */
    CaseRealTestVo prepare(Integer caseId) throws BusinessException;

    /**
     * 开始/结束/回放
     *
     * @param recordId
     * @param action
     * @return
     */
    CaseRealTestVo start(Integer recordId, Integer action) throws BusinessException, IOException;

    void playback(Integer recordId, Integer action) throws BusinessException, IOException;

    /**
     * 获取实车验证结果
     *
     * @param recordId
     * @return
     */
    RealTestResultVo getResult(Integer recordId) throws BusinessException;

    /**
     * 通信时延
     *
     * @param recordId
     * @return
     */
    CommunicationDelayVo communicationDelayVo(Integer recordId);
}
