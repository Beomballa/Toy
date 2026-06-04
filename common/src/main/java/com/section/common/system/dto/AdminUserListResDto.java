package com.section.common.system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminUserListResDto {
    private Long adminNo;
    private String loginId;
    private String name;
    private String role;
    private String status;
    private LocalDateTime lastLoginDtm;
    private LocalDateTime crtDtm;
}
