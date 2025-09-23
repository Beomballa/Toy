package com.section.common.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.section.common.base.entity.type.BaseEntity;
import com.section.common.base.entity.type.YN;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "SY_APPROVAL_DOCUMENT")
public class ApprovalDocument extends BaseEntity {

    @Id
    @Column(name = "DOC_NO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long docNo;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "CONTENT_TYPE_CODE")
    private String contentTypeCode;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "DEPTH")
    private String depth;

    @Column(name = "RENEW_YN")
    @Enumerated(EnumType.STRING)
    private YN renewYn;

    @Column(name = "DEL_YN")
    @Enumerated(EnumType.STRING)
    private YN delYn;

    @Column(name = "MANAGER_NO")
    private String managerNo;

    @Column(name = "ALT_MANAGER_NO")
    private String altManagerNo;

    @Column(name = "APPROVAL_DTM")
    private LocalDateTime approvalDtm;
}