package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.entity.TjCasePartConfig;
import net.wanji.business.mapper.TjCasePartConfigMapper;
import net.wanji.business.service.TjCasePartConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author wj
 * @since 2023-06-29
 */
@Service
public class TjCasePartConfigServiceImpl extends ServiceImpl<TjCasePartConfigMapper, TjCasePartConfig>
        implements TjCasePartConfigService {

    private static final Logger log = LoggerFactory.getLogger("business");


}
