package com.section.common.content.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ContentDateListItemDto {
    private LocalDate startDt;
    private LocalDate endDt;
}
