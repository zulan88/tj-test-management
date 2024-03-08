package net.wanji.business.util;

import net.wanji.business.common.Constants;
import net.wanji.business.domain.bo.CaseConfigBo;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author hcy
 * @version 1.0
 * @className DeviceUtils
 * @description TODO
 * @date 2024/3/8 10:18
 **/
public class DeviceUtils {

  public static String getVisualDeviceId(Integer caseId, Integer deviceId,
      String supportRoles) {
    return getVisualDeviceId(caseId, deviceId, supportRoles, null);
  }

  /**
   * 获取设备虚拟id（用于区分tessng）
   *
   * @param caseId
   * @param deviceId
   * @param supportRoles
   * @return
   */
  public static String getVisualDeviceId(Integer caseId, Integer deviceId,
      String supportRoles, String username) {
    if (Constants.PartRole.MV_SIMULATION.equals(supportRoles)) {
      return TessngUtils.virtualDeviceId(0, caseId, username);
    }
    return deviceId.toString();
  }

  /**
   * 根据设备ID去重
   *
   * @param caseConfigs
   * @return
   */
  public static List<CaseConfigBo> deWeightConfigsByDeviceId(
      List<CaseConfigBo> caseConfigs) {
    return caseConfigs.stream()
        .filter(info -> !ObjectUtils.isEmpty(info.getDeviceId())).collect(
            Collectors.collectingAndThen(Collectors.toCollection(
                    () -> new TreeSet<>(
                        Comparator.comparing(CaseConfigBo::getDeviceId))),
                ArrayList::new));
  }
}
