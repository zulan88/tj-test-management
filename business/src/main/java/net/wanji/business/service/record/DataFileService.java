package net.wanji.business.service.record;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.dto.ToLocalDto;
import net.wanji.business.entity.DataFile;
import net.wanji.business.service.record.impl.FileWriteRunnable;

/**
 * @author hcy
 * @version 1.0
 * @className DataFileService
 * @description TODO
 * @date 2024/4/1 16:45
 **/
public interface DataFileService extends IService<DataFile> {

  FileWriteRunnable createToLocalThread(ToLocalDto toLocalDto);

  boolean writeStop(ToLocalDto toLocalDto) throws Exception;

  /**
   * 回放
   *
   * @param playbackId     回放ID，记录回放线程
   * @param fileId         文件记录ID
   * @param startTimestamp
   * @param endTimestamp
   */
  void playback(String playbackId, Integer fileId, Long startTimestamp,
      Long endTimestamp) throws Exception;

  /**
   * 回放
   *
   * @param playbackId       回放ID，记录回放线程，可为null，为null时不会发送数据至ws
   * @param fileId           文件记录ID
   * @param startTimestamp   开始回放时间戳
   * @param endTimestamp     结束回放时间戳
   * @param playbackInterval 回放时间间隔（ms），转存可以设置为0
   * @param dataCopyService  数据拷贝，可为null
   * @throws Exception
   */
  void playback(String playbackId, Integer fileId, Long startTimestamp,
      Long endTimestamp, Integer playbackInterval,
      DataCopyService dataCopyService) throws Exception;

  boolean playbackStop(String playbackId);

  /**
   * 暂停
   *
   * @param state      true：暂停，false：开始
   * @param playbackId
   * @return
   */
  boolean playbackPause(Boolean state, String playbackId);
}
