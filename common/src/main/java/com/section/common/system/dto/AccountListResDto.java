package com.section.common.system.dto;

import com.section.common.base.entity.type.YN;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AccountListResDto {

    private Long id;
    private String email;
    private String name;
    private String nickname;
    private YN masterYn;
    private YN initYn;
    private YN delYn;
    private LocalDateTime crtDtm;
}
