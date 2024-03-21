package net.wanji.business.entity.infity;

import lombok.Data;

/**
 * @author hcy
 * @version 1.0
 * @className TjShardingResult
 * @description TODO
 * @date 2024/3/21 17:36
 **/
@Data
public class TjShardingResult {
  private Integer shardingId;
  private String recordId;
  private Integer time;
  private Integer evaluationScore;
}
