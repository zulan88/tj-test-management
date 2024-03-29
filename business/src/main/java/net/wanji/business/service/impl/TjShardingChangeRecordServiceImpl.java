package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.domain.InfinteMileScenceExo;
import net.wanji.business.domain.vo.task.infinity.ShardingResultVo;
import net.wanji.business.entity.infity.TjInfinityTask;
import net.wanji.business.entity.infity.TjShardingChangeRecord;
import net.wanji.business.entity.infity.TjShardingResult;
import net.wanji.business.mapper.TjInfinityMapper;
import net.wanji.business.mapper.TjShardingChangeRecordMapper;
import net.wanji.business.service.InfinteMileScenceService;
import net.wanji.business.service.TjShardingChangeRecordService;
import net.wanji.business.util.RedisCacheUtils;
import net.wanji.common.core.redis.RedisCache;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
  private final InfinteMileScenceService infinteMileScenceService;
  @Resource
  private TjShardingChangeRecordMapper shardingChangeRecordMapper;
  @Resource
  private TjInfinityMapper tjInfinityMapper;

  public TjShardingChangeRecordServiceImpl(RedisCache redisCache,
      InfinteMileScenceService infinteMileScenceService) {
    this.redisCache = redisCache;
    this.infinteMileScenceService = infinteMileScenceService;
  }

  @Override
  public void start(Integer taskId, Integer caseId) {
    String recordCacheId = RedisCacheUtils.createRecordCacheId(taskId, caseId);
    if (redisCache.hasKey(recordCacheId)) {
      redisCache.deleteObject(recordCacheId);
    }

    redisCache.setCacheObject(recordCacheId, createRecordId());
  }

  @Override
  public boolean saveShardingInOut(TjShardingChangeRecord record) {
    record.setRecordId(redisCache.getCacheObject(
        RedisCacheUtils.createRecordCacheId(record.getTaskId(),
            record.getCaseId())));
    return this.save(record);
  }

  @Override
  public void stop(Integer taskId, Integer caseId) {
    redisCache.deleteObject(
        RedisCacheUtils.createRecordCacheId(taskId, caseId));
  }

  @Override
  public void stateControl(Integer taskId, Integer caseId, Integer state) {
    if (state > 0) {
      this.start(taskId, caseId);
    } else {
      this.stop(taskId, caseId);
    }
  }

  @Override
  public List<ShardingResultVo> shardingResult(Integer taskId, Integer caseId) {
    String recordCacheId = RedisCacheUtils.createRecordCacheId(taskId, caseId);
    if (redisCache.hasKey(recordCacheId)) {
      Integer recordId = redisCache.getCacheObject(recordCacheId);
      List<TjShardingResult> tjShardingResults = shardingChangeRecordMapper.shardingResult(
          taskId, caseId, recordId);

      TjInfinityTask tjInfinityTask = tjInfinityMapper.selectById(caseId);
      InfinteMileScenceExo infinteMileScenceExo = infinteMileScenceService.selectInfinteMileScenceById(
          tjInfinityTask.getCaseId());
      HashMap<Integer, String> sliceIdNameMap = infinteMileScenceExo.getSiteSlices()
          .stream().collect(HashMap::new,
              (m, v) -> m.put(v.getSliceId(), v.getSliceName()),
              HashMap::putAll);

      return tjShardingResults.stream().map(e -> {
        ShardingResultVo shardingResultVo = new ShardingResultVo();
        shardingResultVo.setTime(e.getTime());
        shardingResultVo.setEvaluationScore(e.getEvaluationScore());
        shardingResultVo.setShardingName(sliceIdNameMap.get(e.getShardingId()));
        return shardingResultVo;
      }).collect(Collectors.toList());
    }
    return new ArrayList<>();

  }

  private Integer createRecordId() {
    return Math.abs(new Random().nextInt());
  }
}
