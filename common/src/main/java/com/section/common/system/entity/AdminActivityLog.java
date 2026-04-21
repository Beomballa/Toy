package com.section.common.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "admin_activity_log")
public class AdminActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_no")
    private Long logNo;

    @Column(name = "admin_no", nullable = false)
    private Long adminNo;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "ip_address", nullable = false, length = 50)
    private String ipAddress;

    @Column(name = "action_dtm")
    private LocalDateTime actionDtm;

    @PrePersist
    public void prePersist() {
        this.actionDtm = LocalDateTime.now();
    }
}
