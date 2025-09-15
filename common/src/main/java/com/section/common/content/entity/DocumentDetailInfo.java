package com.section.common.content.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "CT_DOCUMENT_DETAIL_INFO")
public class DocumentDetailInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no")
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_no")
    private Document docNo;

    @Column(name = "view_cnt")
    private int viewCnt;
}
