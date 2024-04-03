package net.wanji.business.service.record.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.wanji.business.entity.DataFile;
import net.wanji.common.common.ClientSimulationTrajectoryDto;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hcy
 * @version 1.0
 * @className FileAnalysis
 * @description TODO
 * @date 2024/4/2 13:16
 **/
public class FileAnalysis {
  public static DataFile lineOffset(String filePath, DataFile dataFile,
      AnalyseProgressHandler.AnalyseProgress analyseProgress)
      throws IOException {
    File file = new File(filePath, dataFile.getFileName());
    long fileLength = file.length();
    if (fileLength <= 0) {
      return null;
    }
    AnalyseProgressHandler analyseProgressHandler = new AnalyseProgressHandler();
    // 记录每一行开头的指针位置
    List<Long> offsets = new ArrayList<>();
    long startTimestamp = 0;
    long endTimestamp = 0;
    try (FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis,
            StandardCharsets.ISO_8859_1);
        BufferedReader br = new BufferedReader(isr)) {
      // 记录每一行字节大小
      long lineSize;
      // 记录已经读取到的字节总大小
      long totalSize = 0;
      String readLine;
      String judgeFL = null;
      while ((readLine = br.readLine()) != null) {
        lineSize = readLine.getBytes(StandardCharsets.ISO_8859_1).length;
        if (judgeFL == null) {
          startTimestamp = getTimestamp(readLine);
        }
        offsets.add(totalSize);
        //每行有换行符 需要+2
        totalSize += lineSize + 2;

        analyseProgressHandler.record(dataFile.getId(), analyseProgress,
            fileLength, totalSize);
        judgeFL = readLine;
      }
      endTimestamp = getTimestamp(judgeFL);
    }
    analyseProgress.progress(dataFile.getId(), "100");
    dataFile.setDataStartTime(
        LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimestamp),
            ZoneId.systemDefault()));
    dataFile.setDataStopTime(
        LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp),
            ZoneId.systemDefault()));
    dataFile.setLineOffset(new ObjectMapper().writeValueAsString(offsets)
        .getBytes(StandardCharsets.UTF_8));
    return dataFile;
  }

  private static Long getTimestamp(String readLine)
      throws JsonProcessingException {
    List<ClientSimulationTrajectoryDto> clientSimulationTrajectoryDtos = new ObjectMapper().readValue(
        readLine, new TypeReference<List<ClientSimulationTrajectoryDto>>() {
        });
    return Long.parseLong(clientSimulationTrajectoryDtos.get(0).getTimestamp());
  }
}
