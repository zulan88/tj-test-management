package net.wanji.approve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.approve.entity.AppointmentRecord;
import net.wanji.approve.entity.dto.AppointmentRecordDto;
import net.wanji.approve.mapper.AppointmentRecordMapper;
import net.wanji.approve.service.AppointmentRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 预约记录表 服务实现类
 * </p>
 *
 * @author wj
 * @since 2023-12-05
 */
@Service
public class AppointmentRecordServiceImpl extends ServiceImpl<AppointmentRecordMapper, AppointmentRecord> implements AppointmentRecordService {

    @Override
    public List<AppointmentRecord> listByEntity(AppointmentRecordDto appointmentRecord) {
        if (appointmentRecord == null) {
            return this.list();
        }else {
            QueryWrapper<AppointmentRecord> queryWrapper = new QueryWrapper<>();
            if (appointmentRecord.getUnitName()!= null&&!appointmentRecord.getUnitName().isEmpty()) {
                queryWrapper.eq("unit_name", appointmentRecord.getUnitName());
            }
            if (appointmentRecord.getContactPerson()!= null&&!appointmentRecord.getContactPerson().isEmpty()) {
                queryWrapper.eq("contact_person", appointmentRecord.getContactPerson());
            }
            if(appointmentRecord.getMeasurandType()!=null&&!appointmentRecord.getMeasurandType().isEmpty()){
                queryWrapper.eq("measurand_type", appointmentRecord.getMeasurandType());
            }
            if(appointmentRecord.dateExists()){
                queryWrapper.between("commit_date", appointmentRecord.getStartDate(), appointmentRecord.getEndDate());
            }
            return this.list(queryWrapper);
        }
    }


}
