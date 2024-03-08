package net.wanji.business.util;

import net.wanji.business.common.Constants;
import net.wanji.common.utils.SecurityUtils;

/**
 * @author hcy
 * @version 1.0
 * @className TessngUtils
 * @description TODO
 * @date 2024/3/8 10:01
 **/
public class TessngUtils {

  public static String virtualDeviceId(Integer taskId, Integer caseId) {
    return virtualDeviceId(taskId, caseId, null);
  }
  /**
   * tessng交互通信虚拟设备ID（控制通道）
   * @param taskId
   * @param caseId
   * @return
   */
  public static String virtualDeviceId(Integer taskId, Integer caseId, String username) {
    if(null == username){
      username = SecurityUtils.getUsername();
    }
    if (null == taskId || 0 == taskId) {
      return Constants.ChannelBuilder.buildTestingControlChannel(username,
          caseId);
    }
    return Constants.ChannelBuilder.buildTaskControlChannel(username, taskId);
  }
}
