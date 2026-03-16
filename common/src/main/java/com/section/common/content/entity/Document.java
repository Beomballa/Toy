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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NO")
    private Long id;

    @Column(name = "product_no")
    private Long productNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type")
    private BoardType boardType;

    private String title;
    private String content;
    private int viewCnt;

    public enum BoardType { NOTICE, STYLE, DISCUSS, QNA }
}