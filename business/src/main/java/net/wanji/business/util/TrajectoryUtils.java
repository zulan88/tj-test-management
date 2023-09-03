package net.wanji.business.util;

import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.GeoUtil;

import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/9/2 9:16
 * @Descriptoin:
 */

public class TrajectoryUtils {

    /**
     * 计算里程
     * @param data
     * @return
     */
    public static double getMileage(List<TrajectoryValueDto> data) {
        double mileage = 0;
        for (int i = 0; i < data.size() - 1; i++) {
            mileage += GeoUtil.calculateDistance(data.get(i).getLatitude(), data.get(i).getLongitude(),
                    data.get(i + 1).getLatitude(), data.get(i + 1).getLongitude());
        }
        return mileage;
    }

}
