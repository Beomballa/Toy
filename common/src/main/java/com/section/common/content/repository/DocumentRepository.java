package com.section.common.content.repository;

import com.section.common.base.entity.type.YN;
import com.section.common.content.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long>, CustomDocumentRepository {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Document document
               set document.viewCnt = document.viewCnt + 1
             where document.id = :documentId
               and document.status = :status
               and document.publicYn = :publicYn
            """)
    int incrementPublicViewCount(
            @Param("documentId") long documentId,
            @Param("status") Document.PublishStatus status,
            @Param("publicYn") YN publicYn
    );
}
