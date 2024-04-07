package net.wanji.business.service.record.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.domain.dto.RecordSimulationTrajectoryDto;
import net.wanji.business.domain.dto.ToLocalDto;
import net.wanji.business.entity.DataFile;
import net.wanji.business.mapper.DataFileMapper;
import net.wanji.business.service.record.DataFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author hcy
 * @version 1.0
 * @className DataFileServiceImpl
 * @description TODO
 * @date 2024/4/1 16:47
 **/
@Service
@Slf4j
public class DataFileServiceImpl extends ServiceImpl<DataFileMapper, DataFile>
    implements DataFileService {
  private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(10,
      20, 5, TimeUnit.MINUTES, new ArrayBlockingQueue<>(20),
      r -> new Thread(r, "tj-test-m" + r.hashCode()));
  private static final Map<String, List<FileReadThread>> taskThreadMap = new ConcurrentHashMap<>();

  @Value("${ruoyi.profile}/record")
  private String path;

  @Override
  public FileWriteRunnable createToLocalThread(ToLocalDto toLocalDto) {
    pathCheckCreate(path, toLocalDto.getFileName());
    FileWriteRunnable fileWriteRunnable = new FileWriteRunnable(
        toLocalDto.getFileName(), path);
    executor.execute(fileWriteRunnable);
    return fileWriteRunnable;
  }

  @Override
  public boolean writeStop(ToLocalDto toLocalDto) throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    toLocalDto.getToLocalThread().stop(countDownLatch);
    // 更新记录
    DataFile dataFile = this.getById(toLocalDto.getFileId());
    countDownLatch.await();
    dataFile.setEncode("utf-8");
    // ?????????????????? 要改
    dataFile.setDataStopTime(LocalDateTime.now());
    FileAnalysis.lineOffset(path, dataFile, (id, progress) -> {
      DataFile dataFileQ = new DataFile();
      dataFileQ.setId(toLocalDto.getFileId());
      dataFileQ.setProgress(progress);
      updateById(dataFileQ);
    });
    return updateById(dataFile);
  }

  @Override
  public void playback(String playbackId, Integer fileId, Long startTimestamp,
      Long endTimestamp) throws Exception {
    DataFile dataFile = this.getById(fileId);
    File localFile = new File(path, dataFile.getFileName());
    List<Long> offsets = new ObjectMapper().readValue(
        new String(dataFile.getLineOffset(), StandardCharsets.UTF_8),
        new TypeReference<List<Long>>() {
        });
    long startOffset = getStartOffset(fileOffset(
            dataFile.getDataStartTime().atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli(), startTimestamp), offsets, localFile.getPath(),
        dataFile.getEncode(), startTimestamp);
    long endOffset = getEndOffset(startOffset, startTimestamp, endTimestamp,
        offsets);

    ThreadUtils.execute(playbackId, taskThreadMap,
        new FileReadThread(RateLimiter.create(10), dataFile, localFile,
            startOffset, endOffset, offsets, playbackId));
  }

  @Override
  public boolean playbackStop(String playbackId) {
    ThreadUtils.stop(taskThreadMap.get(playbackId));
    return true;
  }

  @Override
  public boolean playbackPause(Boolean state, String playbackId) {
    ThreadUtils.sentPauseAndResume(state, taskThreadMap.get(playbackId));
    return true;
  }

  private long getStartOffset(Long tentativeStartOffset, List<Long> offsets,
      String filePath, String encode, Long timestamp) {
    long startOffset = tentativeStartOffset;
    if (tentativeStartOffset > offsets.size()) {
      startOffset = offsets.size() - 1;
    }
    if (tentativeStartOffset == 0) {
      return 0;
    }
    return correctStartOffset(offsets, startOffset, filePath, encode,
        timestamp);
  }

  private long fileOffset(Long fileStartTimestamp, Long timestamp) {
    long dv = timestamp - fileStartTimestamp;
    if (dv <= 0) {
      return 0;
    } else {
      return dv / 100;
    }
  }

  private long getEndOffset(Long startOffset, Long startTimestamp,
      Long endTimestamp, List<Long> offsets) {
    if (endTimestamp <= 0) {
      return offsets.size() - 1;
    }
    long endOffset = startOffset + (endTimestamp - startTimestamp) / 100;
    if (endOffset > offsets.size()) {
      return offsets.size() - 1;
    }
    return endOffset;
  }

  /**
   * 由于数据缺帧导致无法根据帧数计算偏移量，用时间戳做矫正
   *
   * @param offsets
   * @param startOffset
   * @param filePath
   * @param encode
   * @param startTimestamp
   * @return
   */
  private long correctStartOffset(List<Long> offsets, long startOffset,
      String filePath, String encode, long startTimestamp) {
    ObjectMapper objectMapper = new ObjectMapper();
    long fileTimestamp = getFileTimestamp(offsets, startOffset, filePath,
        encode, objectMapper);
    if (fileTimestamp <= startTimestamp) {
      return startOffset;
    }
    // 计算开始偏移量后推 真实开始时间与计算偏移量时间只差的帧数
    long offsetStartOffset =
        startOffset - (fileTimestamp - startTimestamp) / 100;
    // 折半查找
    Long binSearch = binSearch(offsets, startTimestamp, offsetStartOffset,
        startOffset, filePath, encode, objectMapper);
    if (null == binSearch) {
      return startOffset;
    }
    return binSearch;
  }

  private Long binSearch(List<Long> offsets, long targetTimestamp, long start,
      long end, String filePath, String encode, ObjectMapper objectMapper) {
    long mid = 0;
    long sFileTimestamp = getFileTimestamp(offsets, start, filePath, encode,
        objectMapper);
    long eFileTimestamp = getFileTimestamp(offsets, end, filePath, encode,
        objectMapper);
    while (sFileTimestamp <= eFileTimestamp) {
      mid = (start + end) / 2;
      long mFileTimestamp = getFileTimestamp(offsets, mid, filePath, encode,
          objectMapper);
      if (targetTimestamp < mFileTimestamp) {
        end = mid - 1;
        eFileTimestamp = getFileTimestamp(offsets, end, filePath, encode,
            objectMapper);
      } else if (targetTimestamp > mFileTimestamp) {
        start = mid + 1;
        sFileTimestamp = getFileTimestamp(offsets, start, filePath, encode,
            objectMapper);
      } else {
        return mid;
      }
    }
    return mid;
  }

  private long getFileTimestamp(List<Long> offsets, long startOffset,
      String filePath, String encode, ObjectMapper objectMapper) {
    long fileTimestamp = 0;
    int count = 0;
    while (fileTimestamp == 0 && count < 10) {
      try {
        String line = randomReadFileLine(offsets, startOffset, filePath,
            encode);
        List<RecordSimulationTrajectoryDto> recordss = objectMapper.readValue(
            line, new TypeReference<List<RecordSimulationTrajectoryDto>>() {
            });
        fileTimestamp = recordss.get(0).getTimestamp();
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("getFileTimestamp error!", e);
        }
      }
      startOffset++;
      count++;
    }
    return fileTimestamp;
  }

  private String randomReadFileLine(List<Long> points, long linenumber,
      String filePath, String encode) throws IOException {
    if (0 == linenumber) {
      linenumber = 1;
    }
    try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
      raf.seek(points.get((int) (linenumber - 1)));
      byte[] bytes = raf.readLine().getBytes(StandardCharsets.ISO_8859_1);
      return new String(bytes, encode);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("RandomAccessFile read number[{}] points size[{}] error!",
            linenumber, points.size(), e);
      }
      return null;
    }
  }

  private void pathCheckCreate(String path, String name) {
    File file = new File(path, name);
    File parentFile = file.getParentFile();
    if (!parentFile.exists()) {
      parentFile.mkdirs();
    }
  }
}
