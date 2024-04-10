package net.wanji.business.service.record.impl;

import lombok.extern.slf4j.Slf4j;
import net.wanji.common.file.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author hcy
 * @version 1.0
 * @className FileToLocalThread
 * @description TODO
 * @date 2024/4/2 9:14
 **/
@Slf4j
public class FileWriteRunnable implements Runnable {
  private final String name;
  private final String path;

  public FileWriteRunnable(String name, String path) {
    this.name = name;
    this.path = path;
  }

  private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(
      5000);
  private final AtomicBoolean stop = new AtomicBoolean(false);
  private final AtomicReference<CountDownLatch> countDownLatchAtom = new AtomicReference<>();
  private boolean init = true;
  private boolean started = false;

  @Override
  public void run() {
    started = true;
    FileWriter writer = null;
    try {
      writer = new FileWriter(new File(path, name));
      while (!stop.get()) {
        String data = queue.poll(5, TimeUnit.SECONDS);
        if (!init) {
          writer.write("\n");
        } else {
          init = false;
        }
        writer.write(data);
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("file [{}] [{}] write error!", path, name, e);
      }
      try {
        FileUtils.deleteFile(new File(path, name));
      } catch (Exception de) {
        if (log.isErrorEnabled()) {
          log.error("File [{}] [{}] delete error!", path, name, de);
        }
      }
    } finally {
      try {
        if (null != writer) {
          writer.flush();
          writer.close();
        }
      } catch (Exception e) {
        log.error("File [{}] [{}] writer close error!", path, name, e);
      }
      countDownLatchAtom.get().countDown();
    }
  }

  public boolean write(String data) {
    try {
      queue.add(data);
      return true;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("file [{}] [{}] write error!", path, name, e);
      }
      return false;
    }
  }

  public boolean stop(CountDownLatch countDownLatch) {
    // 防止进程未启动一直等待
    if (!started) {
      countDownLatch.countDown();
    }
    countDownLatchAtom.set(countDownLatch);
    stop.set(true);
    return true;
  }
}
