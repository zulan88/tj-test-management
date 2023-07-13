package net.wanji.dataset.util;

import net.wanji.dataset.entity.VehicleRsuInfo;
import net.wanji.dataset.entity.VehicleRsuInfoDTO;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : yangliang
 * @create 2022/12/27 14:44
 */
public class DevTypeChangeUtil {
    public static void devTypeChange(String devType, VehicleRsuInfo vehicleRsuInfoDTO) {
        if (devType.contains(",")) {
            String[] split = devType.split(",");
            for (int i = 0; i < split.length; i++) {
                switch (split[i]) {
                    case "0":
                        split[i] = "其他";
                        break;
                    case "4":
                        split[i] = "卡口";
                        break;
                    case "5":
                        split[i] = "激光雷达";
                        break;
                    case "6":
                        split[i] = "毫米波雷达";
                        break;
                    default:
                        split[i] = "default";
                }
            }
            String substring = Arrays.deepToString(split).substring(1, Arrays.deepToString(split).length() - 1);
            vehicleRsuInfoDTO.setDevType(substring);
        } else if (devType.contains(" ")) {
            String[] split = devType.split(" ");
            for (int i = 0; i < split.length; i++) {
                switch (split[i]) {
                    case "0":
                        split[i] = "其他";
                        break;
                    case "4":
                        split[i] = "卡口";
                        break;
                    case "5":
                        split[i] = "激光雷达";
                        break;
                    case "6":
                        split[i] = "毫米波雷达";
                        break;
                    default:
                        split[i] = "default";
                }
            }
            String substring = Arrays.deepToString(split).substring(1, Arrays.deepToString(split).length() - 1);
            vehicleRsuInfoDTO.setDevType(substring);
        }
    }


}
