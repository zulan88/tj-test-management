package net.wanji.approve.service;

import net.wanji.approve.entity.AppointmentRecord;
import net.wanji.approve.entity.RecordRe;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author wj
 * @since 2023-12-07
 */
public interface RecordReService extends IService<RecordRe> {

    List<AppointmentRecord> getrecordBydevice(Integer deviceId);

    List<AppointmentRecord> getrecordByperson(Integer deviceId);

}
