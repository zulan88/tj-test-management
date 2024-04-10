package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.wanji.business.common.Constants;
import net.wanji.business.domain.InfinteMileScenceExo;
import net.wanji.business.domain.dto.TjTessngShardingChangeDto;
import net.wanji.business.domain.vo.task.infinity.ShardingResultVo;
import net.wanji.business.entity.infity.TjInfinityTask;
import net.wanji.business.entity.infity.TjInfinityTaskRecord;
import net.wanji.business.entity.infity.TjShardingChangeRecord;
import net.wanji.business.entity.infity.TjShardingResult;
import net.wanji.business.mapper.TjInfinityMapper;
import net.wanji.business.mapper.TjShardingChangeRecordMapper;
import net.wanji.business.service.InfinteMileScenceService;
import net.wanji.business.service.TjInfinityTaskRecordService;
import net.wanji.business.service.TjShardingChangeRecordService;
import net.wanji.business.util.RedisCacheUtils;
import net.wanji.business.util.RedisChannelUtils;
import net.wanji.common.core.redis.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
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
@RequiredArgsConstructor
public class TjShardingChangeRecordServiceImpl
    extends ServiceImpl<TjShardingChangeRecordMapper, TjShardingChangeRecord>
    implements TjShardingChangeRecordService {

  private final RedisCache redisCache;
  private final RedisTemplate<String, Object> redisTemplate;
  private final InfinteMileScenceService infinteMileScenceService;
  private final TjInfinityTaskRecordService tjInfinityTaskRecordService;
  @Resource
  private TjShardingChangeRecordMapper shardingChangeRecordMapper;
  @Resource
  private TjInfinityMapper tjInfinityMapper;

  @Override
  public void start(Integer taskId, Integer caseId, String username) {
    String recordCacheId = RedisCacheUtils.createRecordCacheId(taskId, caseId);
    if (redisCache.hasKey(recordCacheId)) {
      redisCache.deleteObject(recordCacheId);
    }

    redisCache.setCacheObject(recordCacheId,
        createRecordId(taskId, caseId, username));
  }

  @Override
  public boolean saveShardingInOut(TjShardingChangeRecord record) {
    record.setRecordId(redisCache.getCacheObject(
        RedisCacheUtils.createRecordCacheId(record.getTaskId(),
            record.getCaseId())));
    return this.save(record);
  }

  @Override
  public boolean tessngShardingInOutSend(
      TjTessngShardingChangeDto tjTessngShardingChangeDto) throws Exception {
    tjTessngShardingChangeDto.setShardingId(1);
    String commandChannel = RedisChannelUtils.getCommandChannelByRole(
        tjTessngShardingChangeDto.getTaskId(),
        tjTessngShardingChangeDto.getCaseId(), Constants.PartRole.MV_SIMULATION,
        null, tjTessngShardingChangeDto.getUsername());
    redisTemplate.convertAndSend(commandChannel,
        new ObjectMapper().writeValueAsString(tjTessngShardingChangeDto));
    return false;
  }

  @Override
  public void stop(Integer taskId, Integer caseId) {
    redisCache.deleteObject(
        RedisCacheUtils.createRecordCacheId(taskId, caseId));
  }

  @Override
  public void stateControl(Integer taskId, Integer caseId, Integer state,
      String username) {
    if (state > 0) {
      this.start(taskId, caseId, username);
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

  private Integer createRecordId(Integer taskId, Integer caseId,
      String username) {
    QueryWrapper<TjInfinityTaskRecord> recordQW = new QueryWrapper<>();
    if (null != taskId) {
      recordQW.eq("task_id", taskId);
    }
    recordQW.eq("case_id", caseId);
    recordQW.orderByDesc("created_date");
    recordQW.eq("created_by", username);
    Page<TjInfinityTaskRecord> recordPage = new Page<>(0, 1);
    Page<TjInfinityTaskRecord> pageRecord = tjInfinityTaskRecordService.page(
        recordPage, recordQW);
    return pageRecord.getRecords().get(0).getId();
  }
}
