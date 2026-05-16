package com.section.common.commerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderHistoryListResDto {

    private Long historyNo;
    private Long orderNo;
    private String actionType;
    private String beforeStatus;
    private String afterStatus;
    private String reason;
    private String adminMemoSnapshot;
    private String deliveryCompany;
    private String trackingNum;
    private Long actorNo;
    private String actorName;
    private LocalDateTime actionDtm;
}
