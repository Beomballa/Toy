package com.section.common.system.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "admin_operation_notice")
public class AdminOperationNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_no")
    private Long noticeNo;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_active", nullable = false, length = 1)
    private String isActive;

    @Column(name = "is_pinned", nullable = false, length = 1)
    private String isPinned;

    @Column(name = "start_dtm")
    private LocalDateTime startDtm;

    @Column(name = "end_dtm")
    private LocalDateTime endDtm;

    public void update(
            String title,
            String content,
            String isActive,
            String isPinned,
            LocalDateTime startDtm,
            LocalDateTime endDtm
    ) {
        this.title = title;
        this.content = content;
        this.isActive = isActive;
        this.isPinned = isPinned;
        this.startDtm = startDtm;
        this.endDtm = endDtm;
    }

    public void updateActive(String isActive) {
        this.isActive = isActive;
    }
}
