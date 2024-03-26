package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.entity.TjSceneScore;
import net.wanji.business.evaluation.EvalContext;
import net.wanji.business.evaluation.RedisChannelDataProcessor;
import net.wanji.business.mapper.TjSceneScoreMapper;
import net.wanji.business.service.TjSceneScoreService;
import net.wanji.business.util.RedisCacheUtils;
import net.wanji.common.core.redis.RedisCache;
import org.springframework.stereotype.Service;

/**
 * @author hcy
 * @version 1.0
 * @className TjSceneScoreServiceImpl
 * @description TODO
 * @date 2024/3/26 14:52
 **/
@Service
@Slf4j
public class TjSceneScoreServiceImpl
    extends ServiceImpl<TjSceneScoreMapper, TjSceneScore>
    implements TjSceneScoreService, RedisChannelDataProcessor {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RedisCache redisCache;

  public TjSceneScoreServiceImpl(RedisCache redisCache) {
    this.redisCache = redisCache;
  }

  @Override
  public void data(Object data, EvalContext contextDto) {
    try {
      String recordCacheId = RedisCacheUtils.createRecordCacheId(
          contextDto.getTaskId(), contextDto.getCaseId());
      if (redisCache.hasKey(recordCacheId)) {
        String recordId = redisCache.getCacheObject(recordCacheId);
        TjSceneScore tjSceneScore = objectMapper.readValue(String.valueOf(data),
            TjSceneScore.class);
        tjSceneScore.setRecordId(recordId);
        tjSceneScore.setTaskId(contextDto.getTaskId());
        tjSceneScore.setCaseId(contextDto.getCaseId());
        this.save(tjSceneScore);
      } else {
        if (log.isErrorEnabled()) {
          log.error("There is no corresponding record id[{}]", contextDto);
        }
      }

    } catch (JsonProcessingException e) {
      if (log.isErrorEnabled()) {
        log.error("tjSceneScore parser error!", e);
      }
    }
  }

  @Override
  public void data(String channel, Object data, EvalContext contextDto) {

  }

  @Override
  public String dataId(Object data) {
    return null;
  }
}
