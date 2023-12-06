package net.wanji.approve.entity.dto;

import lombok.Data;
import net.wanji.approve.entity.AppointmentRecord;

import java.time.LocalDateTime;

@Data
public class AppointmentRecordDto extends AppointmentRecord {

    LocalDateTime startDate;

    LocalDateTime endDate;

    public boolean dateExists() {
        return startDate!= null || endDate!= null;
    }

}
