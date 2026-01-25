package com.section.common.content.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "CT_DOCUMENT_STATS",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_doc_std_dt",
                        columnNames = {"NO", "STD_DT"} // 문서 PK(NO)와 통계날짜의 조합을 유니크하게 설정(같은 날짜 중복 방지)
                )
        }
)
public class DocumentStats extends BaseEntity {

    @Id
    @Column(name = "STATS_NO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NO")
    private Document document;

    @Column(name = "STD_DT")
    private LocalDate stdDt;

    @Column(name = "VIEW_CNT", nullable = false)
    private int viewCnt;

    public void addViewCount(int count) {
        this.viewCnt += count;
    }
}