package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.entity.TjShardingChangeRecord;
import net.wanji.business.mapper.TjShardingChangeRecordMapper;
import net.wanji.business.service.TjShardingChangeRecordService;
import org.springframework.stereotype.Service;

/**
 * @author hcy
 * @version 1.0
 * @className TjShardingChangeRecordServiceImpl
 * @description TODO
 * @date 2024/3/12 18:23
 **/
@Service
public class TjShardingChangeRecordServiceImpl
    extends ServiceImpl<TjShardingChangeRecordMapper, TjShardingChangeRecord>
    implements TjShardingChangeRecordService {
}
