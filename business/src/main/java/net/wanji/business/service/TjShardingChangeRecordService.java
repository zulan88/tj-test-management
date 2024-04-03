package net.wanji.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.dto.TjTessngShardingChangeDto;
import net.wanji.business.domain.vo.task.infinity.ShardingResultVo;
import net.wanji.business.entity.infity.TjShardingChangeRecord;

import java.util.List;

/**
 * @author hcy
 * @version 1.0
 * @interfaceName TjShardingChangeRecordService
 * @description TODO
 * @date 2024/3/12 18:22
 **/
public interface TjShardingChangeRecordService
    extends IService<TjShardingChangeRecord> {

  /**
   * 开始，创建记录ID
   *
   * @param taskId
   * @param caseId
   */
  void start(Integer taskId, Integer caseId);

  /**
   * 进出分片信息保存时自动获取记录id
   *
   * @param record
   * @return
   */
  boolean saveShardingInOut(TjShardingChangeRecord record);

  /**
   * tessng分片信息交互
   *
   * @param tjTessngShardingChangeDto
   * @return
   */
  boolean tessngShardingInOutSend(
      TjTessngShardingChangeDto tjTessngShardingChangeDto) throws Exception;

  /**
   * 结束，删除记录ID
   *
   * @param taskId
   * @param caseId
   */
  void stop(Integer taskId, Integer caseId);

  /**
   * 开始结束
   *
   * @param taskId
   * @param caseId
   * @param state
   */
  void stateControl(Integer taskId, Integer caseId, Integer state);

  /**
   * 分片进出统计结果
   *
   * @param taskId
   * @param caseId
   * @return
   */
  List<ShardingResultVo> shardingResult(Integer taskId, Integer caseId);
}
