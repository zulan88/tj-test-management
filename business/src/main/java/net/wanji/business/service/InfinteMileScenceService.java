package net.wanji.business.service;

import net.wanji.business.domain.InfinteMileScenceExo;
import net.wanji.business.domain.SiteSlice;
import net.wanji.business.domain.TrafficFlow;
import net.wanji.business.entity.InfinteMileScence;
import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.exception.BusinessException;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author wj
 * @since 2024-02-23
 */
public interface InfinteMileScenceService extends IService<InfinteMileScence> {

    List<InfinteMileScenceExo> selectInfinteMileScenceList();

    String buildSceneNumber();

    Integer saveInfinteMileScence(InfinteMileScenceExo infinteMileScence);

    void debugging(InfinteMileScenceExo infinteMileScence) throws BusinessException;

    /**
     * 根据id查询
     * @param id
     * @return
     */
    InfinteMileScenceExo selectInfinteMileScenceById(Integer id);

    InfinteMileScenceExo selectInfinteMileScenceById2(Integer id);

    /**
     * 处理实体格式
     * @param infinteMileScenceExo
     */
    void dualInfiniteSimulation(InfinteMileScenceExo infinteMileScenceExo);

    /**
     * 停止仿真
     * @param id
     * @return
     * @throws BusinessException
     */
    boolean stopInfinteSimulation(Integer id) throws BusinessException;

    List<TrafficFlow> getTrafficFlow(Integer mapid) throws BusinessException;

    List<SiteSlice> getSiteSlice(Integer id);

}
