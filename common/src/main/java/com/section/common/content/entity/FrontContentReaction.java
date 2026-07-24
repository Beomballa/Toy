package com.section.common.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "front_content_reaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_front_content_reaction_visitor",
                columnNames = {"document_no", "visitor_key"}
        )
)
public class FrontContentReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reaction_no")
    private Long id;

    @Column(name = "document_no", nullable = false)
    private Long documentNo;

    @Column(name = "visitor_key", nullable = false, length = 64)
    private String visitorKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 20)
    private ReactionType reactionType;

    @Column(name = "created_dtm", nullable = false)
    private LocalDateTime createdDtm;

    @Column(name = "updated_dtm", nullable = false)
    private LocalDateTime updatedDtm;

    public enum ReactionType {
        HELPFUL,
        NOT_HELPFUL
    }
}
