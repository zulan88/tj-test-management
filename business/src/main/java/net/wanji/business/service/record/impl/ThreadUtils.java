package net.wanji.business.service.record.impl;

import com.google.common.util.concurrent.RateLimiter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author glace
 * @version 1.0
 * @className ThreadUtils
 * @description TODO
 * @date 2023/8/21 16:11
 **/
public class ThreadUtils {

  public static void execute(String fileId,
      Map<String, List<FileReadThread>> taskThreadMap,
      FileReadThread readThread) {
    List<FileReadThread> threads = taskThreadMap.computeIfAbsent(fileId,
        k -> new ArrayList<>());
    threads.add(execute(readThread));
  }

  public static FileReadThread execute(FileReadThread readThread) {
    readThread.start();
    return readThread;
  }

  public static void stop(List<FileReadThread> threads) {
    if (null == threads) {
      return;
    }
    for (FileReadThread thread : threads) {
      thread.sendStop();
    }
  }

  public static void sentPauseAndResume(boolean state,
      List<FileReadThread> threads) {
    if (null != threads) {
      for (FileReadThread thread : threads) {
        if (state) {
          thread.setPauseState(true);
        } else {
          RateLimiter rateLimiter = thread.getRateLimiter();
          synchronized (rateLimiter) {
            thread.setPauseState(false);
            rateLimiter.notify();
          }
        }
      }
    }
  }
}
