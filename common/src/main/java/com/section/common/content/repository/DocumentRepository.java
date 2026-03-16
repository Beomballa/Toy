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

}
