package com.section.common.content.repository;

import com.section.common.base.entity.type.YN;
import com.section.common.content.entity.Document;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long>, CustomDocumentRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select document from Document document where document.id = :documentId")
    Optional<Document> findByIdForUpdate(@Param("documentId") long documentId);

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
