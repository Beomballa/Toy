package com.section.common.content.dto;

import com.section.common.base.entity.type.YN;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DocumentDateListItemDto {
    private Long no;
    private LocalDateTime stdDate;
}
