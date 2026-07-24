package com.section.common.system.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "admin_operation_task",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_admin_operation_task_source",
                columnNames = {"source_type", "source_id"}
        )
)
public class AdminOperationTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_no")
    private Long taskNo;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    @Column(name = "assignee_admin_no")
    private Long assigneeAdminNo;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "is_pinned", nullable = false, length = 1)
    private String isPinned;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    public void update(
            String title,
            String description,
            String status,
            String priority,
            Long assigneeAdminNo,
            LocalDate dueDate,
            String isPinned
    ) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assigneeAdminNo = assigneeAdminNo;
        this.dueDate = dueDate;
        this.isPinned = isPinned;
    }

    public void updateStatus(String status) {
        this.status = status;
    }
}
