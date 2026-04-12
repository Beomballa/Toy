package com.section.common.content.repository;

import com.section.common.content.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findAllByBoardTypeOrderByCrtDtmDesc(Document.BoardType boardType, Pageable pageable);
}
