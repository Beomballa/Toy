package com.section.common.system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminSystemSettingHistoryListResDto {

    private Long historyNo;
    private String settingKey;
    private String settingName;
    private String beforeValue;
    private String afterValue;
    private String changeSummary;
    private String changedIpAddress;
    private Long crtNo;
    private LocalDateTime crtDtm;
}
