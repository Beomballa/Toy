package com.section.common.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "admin_user")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_no")
    private Long adminNo;

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "role", length = 20)
    @Builder.Default
    private String role = "ROLE_ADMIN";

    @Column(name = "status", length = 10)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "last_login_dtm")
    private LocalDateTime lastLoginDtm;

    @Column(name = "crt_dtm", updatable = false)
    private LocalDateTime crtDtm;

    @PrePersist
    public void prePersist() {
        this.crtDtm = LocalDateTime.now();
    }

    public void updateInfo(String name, String role, String status) {
        this.name = name;
        this.role = role;
        this.status = status;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void recordLogin(LocalDateTime loginDtm) {
        this.lastLoginDtm = loginDtm;
    }
}
