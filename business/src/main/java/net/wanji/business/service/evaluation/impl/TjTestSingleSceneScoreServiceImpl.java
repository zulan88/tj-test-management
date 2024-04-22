package net.wanji.business.service.evaluation.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.common.Constants;
import net.wanji.business.domain.RealWebsocketMessage;
import net.wanji.business.entity.evaluation.single.TjTestSingleSceneScore;
import net.wanji.business.evaluation.EvalContext;
import net.wanji.business.mapper.evaluation.single.TjTestSingleSceneScoreMapper;
import net.wanji.business.service.evaluation.TjTestSingleSceneScoreService;
import net.wanji.business.socket.WebSocketManage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author hcy
 * @version 1.0
 * @className TjTestSingleSceneScoreServiceImpl
 * @description TODO
 * @date 2024/4/22 15:35
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class TjTestSingleSceneScoreServiceImpl
    extends ServiceImpl<TjTestSingleSceneScoreMapper, TjTestSingleSceneScore>
    implements TjTestSingleSceneScoreService {

  private ObjectMapper objectMapper = new ObjectMapper();

  @Resource
  private TjTestSingleSceneScoreMapper tjTestSingleSceneScoreMapper;

  @Override
  @Transactional
  public void data(Object data, EvalContext contextDto) {
    try {
      TjTestSingleSceneScore tjTestSingleSceneScore = objectMapper.readValue(
          String.valueOf(data), TjTestSingleSceneScore.class);
      if (1 == tjTestSingleSceneScore.getSenceStatus()) {
        // 分片结束后发送总评分
        evaluationInfoSave(tjTestSingleSceneScore, contextDto);
      } else {
        // 发送 ws
        wsSend(tjTestSingleSceneScore, contextDto.getWsClientKey());
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("tjSceneScore process data error!", e);
      }
    }
  }

  public boolean saveByUnique(TjTestSingleSceneScore entity) {
    QueryWrapper<TjTestSingleSceneScore> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("task_id", entity.getTaskId());
    queryWrapper.eq("case_id", entity.getCaseId());
    queryWrapper.eq("test_type", entity.getTestType());
    queryWrapper.eq("record_id", entity.getRecordId());
    TjTestSingleSceneScore sceneScore = this.getOne(queryWrapper);
    if (null == sceneScore) {
      return tjTestSingleSceneScoreMapper.insert(entity) > 0;
    } else {
      if (log.isErrorEnabled()) {
        log.error("Repeat data TjTestSingleSceneScore[{}]!", entity);
      }
      return false;
    }
  }

  @Override
  public void data(String channel, Object data, EvalContext contextDto) {

  }

  @Override
  public String dataId(Object data) {
    return null;
  }

  private void evaluationInfoSave(TjTestSingleSceneScore tjTestSingleSceneScore,
      EvalContext contextDto) {
    tjTestSingleSceneScore.setRecordId(contextDto.getRecordId());
    tjTestSingleSceneScore.setTestType(contextDto.getTestType());
    saveByUnique(tjTestSingleSceneScore);
  }

  private void wsSend(TjTestSingleSceneScore tjTestSingleSceneScore,
      String wsClientKey) throws JsonProcessingException {
    RealWebsocketMessage msg = new RealWebsocketMessage(
        Constants.RedisMessageType.SCORE, Maps.newHashMap(),
        tjTestSingleSceneScore, "");
    WebSocketManage.sendInfo(wsClientKey,
        new ObjectMapper().writeValueAsString(msg));
  }
}
