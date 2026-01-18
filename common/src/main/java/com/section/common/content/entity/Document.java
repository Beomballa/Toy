package com.section.common.content.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.section.common.base.entity.type.BaseEntity;
import com.section.common.base.entity.type.YN;
import com.section.common.system.entity.ApprovalDocument;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "CT_DOCUMENT")
public class Document extends BaseEntity {

    @Id
    @Column(name = "NO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DOC_NO")
    private ApprovalDocument approvalDocument;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "CONTENT")
    private String content;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "RESERVE_YN")
    @Enumerated(EnumType.STRING)
    private YN reserveYn;

    @Column(name = "RESERVE_DTM")
    private LocalDateTime reserveDtm;

    @Column(name = "VIEW_CNT")
    private int viewCnt;

    @Column(name = "VIEW_YN")
    @Enumerated(EnumType.STRING)
    private YN viewYn;

}