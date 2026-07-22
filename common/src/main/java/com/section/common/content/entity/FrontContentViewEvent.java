package com.section.common.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "front_content_view_event",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_front_content_view_daily",
                columnNames = {"document_no", "visitor_key", "viewed_date"}
        )
)
public class FrontContentViewEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_no")
    private Long id;

    @Column(name = "document_no", nullable = false)
    private Long documentNo;

    @Column(name = "visitor_key", nullable = false, length = 64)
    private String visitorKey;

    @Column(name = "viewed_date", nullable = false)
    private LocalDate viewedDate;

    @Column(name = "viewed_dtm", nullable = false)
    private LocalDateTime viewedDtm;
}
