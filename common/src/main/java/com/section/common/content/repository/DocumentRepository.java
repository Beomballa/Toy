package com.section.common.content.repository;

import com.section.common.content.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document,Long> {

    @Query("SELECT d FROM Document d where d.approvalDocument.docNo =:docNo")
    Optional<Document> findByDocNo(Long docNo);

    @Query("SELECT d FROM Document d where d.crtNo =:adminNo")
    List<Document> findByDocumentInfo(String adminNo);
}
