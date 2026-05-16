package com.section.common.commerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductHistoryListResDto {

    private Long historyNo;
    private Long productNo;
    private String actionType;
    private String summary;
    private String statusSnapshot;
    private Integer optionCount;
    private Long totalStock;
    private Long actorNo;
    private String actorName;
    private LocalDateTime actionDtm;
}
