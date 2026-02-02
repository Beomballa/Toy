package com.section.common.content.repository;

import com.section.common.content.custom.CustomDocumentRepository;
import com.section.common.content.dto.DocumentDateListItemDto;
import com.section.common.content.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document,Long>, CustomDocumentRepository {

    @Query("SELECT d FROM Document d where d.id =:no")
    Optional<Document> findByNo(Long no);

    @Modifying(clearAutomatically = true) // 벌크 연산 후 영속성 컨텍스트 초기화 (필수)
    @Query("UPDATE Document d SET d.viewCnt = d.viewCnt + :quantity, d.reserveDtm = CURRENT_TIMESTAMP WHERE d.id = :id")
    void addViewCnt(@Param("id") Long id, @Param("quantity") int quantity);

    @Query("SELECT d FROM Document d WHERE d.reserveDtm BETWEEN :startDate AND :today ORDER BY d.viewCnt DESC limit 5")
    List<Document> getRecent7DaysDocumentList(LocalDateTime startDate, LocalDateTime today);

    @Query("SELECT new com.section.common.content.dto.DocumentDateListItemDto(d.id AS no, d.reserveDtm AS stdDate) " +
            "FROM Document d WHERE d.reserveDtm BETWEEN :startDate AND :endDt")
    List<DocumentDateListItemDto> findDocumentDateInfo(LocalDate startDate, LocalDate endDt);

}
