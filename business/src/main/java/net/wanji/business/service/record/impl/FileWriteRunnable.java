package net.wanji.business.service.record.impl;

import lombok.extern.slf4j.Slf4j;
import net.wanji.common.file.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
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

  @Override
  public void run() {
    try (FileWriter writer = new FileWriter(new File(path, name))) {
      while (!stop.get()) {
        String poll = queue.take();
        if (!init) {
          writer.write("\n");
        } else {
          init = false;
        }
        writer.write(poll);
      }
      writer.flush();
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
    }finally {
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
    countDownLatchAtom.set(countDownLatch);
    stop.set(true);
    return true;
  }
}