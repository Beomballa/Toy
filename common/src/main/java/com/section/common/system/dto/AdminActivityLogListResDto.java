package com.section.common.system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminActivityLogListResDto {

    private Long logNo;
    private Long adminNo;
    private String actionType;
    private Long targetId;
    private String ipAddress;
    private LocalDateTime actionDtm;
}
