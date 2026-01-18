package com.section.common.content.repository;

import com.section.common.content.custom.CustomDocumentRepository;
import com.section.common.content.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document,Long>, CustomDocumentRepository {

    @Query("SELECT d FROM Document d where d.approvalDocument.docNo =:docNo")
    Optional<Document> findByDocNo(Long docNo);

    @Modifying(clearAutomatically = true) // 벌크 연산 후 영속성 컨텍스트 초기화 (필수)
    @Query("UPDATE Document d SET d.viewCnt = d.viewCnt + :quantity WHERE d.id = :id")
    void addViewCnt(@Param("id") Long id, @Param("quantity") int quantity);

}
