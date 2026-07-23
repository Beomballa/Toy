package com.section.common.content.repository;

import com.section.common.content.entity.FrontContentViewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface FrontContentViewEventRepository extends
        JpaRepository<FrontContentViewEvent, Long>,
        CustomFrontContentViewEventRepository {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO front_content_view_event
                (document_no, visitor_key, viewed_date, viewed_dtm)
            VALUES (:documentNo, :visitorKey, :viewedDate, :viewedDtm)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("documentNo") long documentNo,
            @Param("visitorKey") String visitorKey,
            @Param("viewedDate") LocalDate viewedDate,
            @Param("viewedDtm") LocalDateTime viewedDtm
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from FrontContentViewEvent event where event.viewedDate < :retentionStartDate")
    int deleteBefore(@Param("retentionStartDate") LocalDate retentionStartDate);
}
