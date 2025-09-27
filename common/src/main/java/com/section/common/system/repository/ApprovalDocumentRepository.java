package com.section.common.system.repository;

import com.section.common.system.entity.ApprovalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApprovalDocumentRepository extends JpaRepository<ApprovalDocument, Long> {
    @Query("SELECT ad FROM ApprovalDocument ad WHERE ad.docNo in :ids")
    List<ApprovalDocument> findAllDocumentInfo(List<Long> ids);
}
