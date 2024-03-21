package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.entity.TjShardingChangeRecord;
import net.wanji.business.mapper.TjShardingChangeRecordMapper;
import net.wanji.business.service.TjShardingChangeRecordService;
import net.wanji.common.core.redis.RedisCache;
import net.wanji.common.utils.uuid.UUID;
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

  private final RedisCache redisCache;

  public TjShardingChangeRecordServiceImpl(RedisCache redisCache) {
    this.redisCache = redisCache;
  }

  @Override
  public void start(Integer taskId, Integer caseId) {
    String recordCacheId = createRecordCacheId(taskId, caseId);
    if (redisCache.hasKey(recordCacheId)) {
      redisCache.deleteObject(recordCacheId);
    }
    redisCache.setCacheObject(recordCacheId, createRecordId());
  }

  @Override
  public boolean saveShardingInOut(TjShardingChangeRecord record) {
    record.setRecordId(redisCache.getCacheObject(createRecordId()));
    return this.save(record);
  }

  @Override
  public void stop(Integer taskId, Integer caseId) {
    redisCache.deleteObject(createRecordCacheId(taskId, caseId));
  }

  @Override
  public void stateControl(Integer taskId, Integer caseId, Integer state) {
    if (state > 0) {
      this.start(taskId, caseId);
    } else {
      this.stop(taskId, caseId);
    }
  }

  private String createRecordCacheId(Integer taskId, Integer caseId) {
    return "record_" + taskId + "_" + caseId;
  }

  private String createRecordId() {
    return UUID.fastUUID().toString().replaceAll("-", "");
  }
}
