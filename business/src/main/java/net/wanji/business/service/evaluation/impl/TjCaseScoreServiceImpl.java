package net.wanji.business.service.evaluation.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.entity.evaluation.TjCaseSceneItemScore;
import net.wanji.business.entity.evaluation.TjCaseSceneScore;
import net.wanji.business.entity.evaluation.TjCaseScore;
import net.wanji.business.enumeration.EvaluationItemType;
import net.wanji.business.evaluation.EvalContext;
import net.wanji.business.evaluation.RedisChannelDataProcessor;
import net.wanji.business.mapper.evaluation.TjCaseSceneItemScoreMapper;
import net.wanji.business.mapper.evaluation.TjCaseSceneScoreMapper;
import net.wanji.business.mapper.evaluation.TjCaseScoreMapper;
import net.wanji.business.service.evaluation.TjCaseScoreService;
import net.wanji.business.util.RedisCacheUtils;
import net.wanji.common.core.redis.RedisCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author hcy
 * @version 1.0
 * @className TjCaseScoreServiceImpl
 * @description TODO
 * @date 2024/3/27 16:49
 **/
@Slf4j
@Service
public class TjCaseScoreServiceImpl
    extends ServiceImpl<TjCaseScoreMapper, TjCaseScore>
    implements TjCaseScoreService {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RedisCache redisCache;
  @Resource
  private TjCaseScoreMapper tjCaseScoreMapper;
  @Resource
  private TjCaseSceneScoreMapper tjCaseSceneScoreMapper;
  @Resource
  private TjCaseSceneItemScoreMapper tjCaseSceneItemScoreMapper;

  public TjCaseScoreServiceImpl(RedisCache redisCache) {
    this.redisCache = redisCache;
  }

  @Override
  @Transactional
  public void data(Object data, EvalContext contextDto) {
    try {
      String recordCacheId = RedisCacheUtils.createRecordCacheId(
          contextDto.getTaskId(), contextDto.getCaseId());
      if (redisCache.hasKey(recordCacheId)) {
        Integer recordId = redisCache.getCacheObject(recordCacheId);
        TjCaseScore tjCaseScore = objectMapper.readValue(String.valueOf(data),
            TjCaseScore.class);

        evaluationInfoSave(tjCaseScore, recordId, tjCaseScore.getTaskId(),
            contextDto.getCaseId());
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
  public boolean save(TjCaseScore entity) {
    QueryWrapper<TjCaseScore> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("business_id", entity.getBusinessId());
    TjCaseScore tjCaseScore = this.getOne(queryWrapper);
    if (null == tjCaseScore) {
      return tjCaseScoreMapper.insert(tjCaseScore) > 0;
    }
    return true;
  }

  @Override
  public void data(String channel, Object data, EvalContext contextDto) {

  }

  @Override
  public String dataId(Object data) {
    return null;
  }

  public void evaluationInfoSave(TjCaseScore tjCaseScore, Integer recordId,
      Integer taskId, Integer caseId) {
    tjCaseScore.setRecordId(recordId);
    tjCaseScore.setBusinessId(createBusinessId(taskId, caseId));
    this.save(tjCaseScore);

    List<TjCaseSceneScore> testScene = tjCaseScore.getTestScene();
    for (TjCaseSceneScore tjCaseSceneScore : testScene) {
      tjCaseSceneScoreMapper.insert(tjCaseSceneScore);
      Map<String, List<TjCaseSceneItemScore>> infoMap = tjCaseSceneScore.getInfo();
      for (Map.Entry<String, List<TjCaseSceneItemScore>> info : infoMap.entrySet()) {
        String itemType = info.getKey();
        List<TjCaseSceneItemScore> items = info.getValue();
        for (TjCaseSceneItemScore item : items) {
          item.setItemType(EvaluationItemType.valueOf(itemType));
          tjCaseSceneItemScoreMapper.insert(item);
        }
      }
    }
  }

  private String createBusinessId(Integer taskId, Integer caseId) {
    return taskId + "_" + caseId;
  }
}
