package net.wanji.business.service;

import net.wanji.business.domain.InfinteMileScenceExo;
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

    Boolean saveInfinteMileScence(InfinteMileScenceExo infinteMileScence);

    void debugging(InfinteMileScenceExo infinteMileScence) throws BusinessException;

}
