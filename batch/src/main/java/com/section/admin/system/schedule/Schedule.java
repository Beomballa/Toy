package com.section.admin.system.schedule;

import com.section.common.content.dto.DocumentDateListItemDto;
import com.section.common.content.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class Schedule {
    private Log log = LogFactory.getLog(Schedule.class);

    private final DocumentService documentService;

    @Scheduled(cron = "*/5 * * * * *")
    public void DocumentStatsSchedule() {
        log.info("=========== DocumentStatsSchedule Start!!! ===========");
        try{
            // ************* 여기에서 검색의 기준은 AdminUser의 계정 정보 *************
            // 1. Document 테이블에서 조회한 기간 별(필터 조건)에 따른 데이터가 있는지 확인]]
            List<DocumentDateListItemDto> resDto = documentService.findDocumentDateInfo(null, null);

            // 2. 해당 기간에 데이터가 존재한다면 Document 테이블의 해당 날짜들에 total_cnt를 계산

            // 3. 해당 날짜에 total_cnt가 하나씩만 구현Document_Stats 테이블에 들어가도록 구현

        }catch (Exception e){

        }
    }
}
