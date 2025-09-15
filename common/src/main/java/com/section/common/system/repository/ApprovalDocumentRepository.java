package com.section.common.system.repository;

import com.section.common.system.entity.ApprovalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalDocumentRepository extends JpaRepository<ApprovalDocument, Long> {
}
