package net.wanji.approve.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScheduleDto {

    List<String> dates;

    Integer recordId;

    String deviceIds;

    String personIds;

}
