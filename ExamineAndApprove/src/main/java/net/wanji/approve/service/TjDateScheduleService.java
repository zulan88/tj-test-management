package net.wanji.approve.service;

import net.wanji.approve.entity.TjDateSchedule;
import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.approve.entity.vo.DateScheduleVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author wj
 * @since 2023-12-07
 */
public interface TjDateScheduleService extends IService<TjDateSchedule> {

    List<DateScheduleVo> takeDateScheduleByDate(String year, Integer quarter);



}
