package net.wanji.business.service.record.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.common.Constants;
import net.wanji.business.domain.RealWebsocketMessage;
import net.wanji.business.entity.DataFile;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.common.common.ClientSimulationTrajectoryDto;
import net.wanji.common.utils.DateUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author glace
 * @version 1.0
 * @className HistoryFileReadRunnable
 * @description TODO
 * @date 2023/8/17 15:47
 **/
@Slf4j
@RequiredArgsConstructor
public class FileReadThread extends Thread {

  @Getter
  private final RateLimiter rateLimiter;
  private final DataFile dataFile;
  private final String filePath;
  private final Long startOffset;
  private final Long endOffset;
  private final List<Long> offsets;
  private final String wsClientKey;

  private final AtomicBoolean pause = new AtomicBoolean(false);
  private final AtomicBoolean stop = new AtomicBoolean(false);

  @Override
  public void run() {

    long count = startOffset;
    if (startOffset < 0 || endOffset > offsets.size()) {
      throw new IllegalArgumentException(
          String.format("lineNum must between %s and %s", 0, offsets.size()));
    }
    try (RandomAccessFile raf = new RandomAccessFile(
        new File(filePath, dataFile.getFileName()), "r")) {
      String encode = dataFile.getEncode();
      if (null == encode) {
        encode = "GB2312";
      }
      raf.seek(offsets.get(startOffset.intValue()));
      while (count <= endOffset && !stop.get()) {
        if (pause.get()) {
          synchronized (rateLimiter) {
            rateLimiter.wait();
          }
        }
        rateLimiter.acquire();
        byte[] bytes = raf.readLine().getBytes(StandardCharsets.ISO_8859_1);
        ClientSimulationTrajectoryDto trajectoryDto;
        try {
          trajectoryDto = new ObjectMapper().readValue(
              new String(bytes, encode), ClientSimulationTrajectoryDto.class);
        } catch (Exception e) {
          if (log.isErrorEnabled()) {
            log.error("data parse error!", e);
          }
          continue;
        }
        if (dataSend(trajectoryDto, count))
          continue;
        count++;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("History file read error!", e);
      }
    } finally {
      dataSendEnd(count);
    }
  }

  private boolean dataSend(
      ClientSimulationTrajectoryDto participantTrajectories, long count)
      throws JsonProcessingException {
    String duration = DateUtils.secondsToDuration(
        (int) Math.floor((double) count / 10));
    RealWebsocketMessage msg = new RealWebsocketMessage(
        Constants.RedisMessageType.TRAJECTORY, Maps.newHashMap(),
        participantTrajectories, duration);
    WebSocketManage.sendInfo(wsClientKey,
        new ObjectMapper().writeValueAsString(msg));
    return false;
  }

  public void setPauseState(boolean state) {
    pause.set(state);
  }

  public void sendStop() {
    stop.set(true);
  }

  private void dataSendEnd(long count) {
    String duration = DateUtils.secondsToDuration(
        (int) Math.floor((double) count / 10));
    RealWebsocketMessage endMsg = new RealWebsocketMessage(
        Constants.RedisMessageType.END, null, null, duration);
    try {
      WebSocketManage.sendInfo(wsClientKey,
          new ObjectMapper().writeValueAsString(endMsg));
    } catch (JsonProcessingException e) {
      if (log.isErrorEnabled()) {
        log.error("History file read end send error!", e);
      }
    }
  }
}
