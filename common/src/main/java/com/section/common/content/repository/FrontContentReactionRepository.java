package com.section.common.content.repository;

import com.section.common.content.entity.FrontContentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface FrontContentReactionRepository extends
        JpaRepository<FrontContentReaction, Long>,
        CustomFrontContentReactionRepository {

    Optional<FrontContentReaction> findByDocumentNoAndVisitorKey(long documentNo, String visitorKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO front_content_reaction
                (document_no, visitor_key, reaction_type, created_dtm, updated_dtm)
            VALUES (:documentNo, :visitorKey, :reactionType, :reactedDtm, :reactedDtm)
            ON DUPLICATE KEY UPDATE
                reaction_type = VALUES(reaction_type),
                updated_dtm = VALUES(updated_dtm)
            """, nativeQuery = true)
    int upsert(
            @Param("documentNo") long documentNo,
            @Param("visitorKey") String visitorKey,
            @Param("reactionType") String reactionType,
            @Param("reactedDtm") LocalDateTime reactedDtm
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from FrontContentReaction reaction where reaction.documentNo = :documentNo")
    int deleteByDocumentNo(@Param("documentNo") long documentNo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from FrontContentReaction reaction where reaction.documentNo in :documentNos")
    int deleteByDocumentNoIn(@Param("documentNos") Collection<Long> documentNos);
}
