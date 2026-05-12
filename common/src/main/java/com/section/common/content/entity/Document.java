package com.section.common.content.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.section.common.base.entity.type.BaseEntity;
import com.section.common.base.entity.type.YN;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PublishStatus status = PublishStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "public_yn", nullable = false, length = 1)
    private YN publicYn = YN.Y;

    @Enumerated(EnumType.STRING)
    @Column(name = "pinned_yn", nullable = false, length = 1)
    private YN pinnedYn = YN.N;

    private String title;
    private String content;
    private int viewCnt;

    public enum BoardType { NOTICE, STYLE, DISCUSS, QNA }

    public enum PublishStatus { DRAFT, PUBLISHED }

    public void applyEditorValues(
            BoardType boardType,
            PublishStatus status,
            YN publicYn,
            YN pinnedYn,
            String title,
            String content,
            Long productNo
    ) {
        this.boardType = boardType;
        this.status = status;
        this.publicYn = publicYn;
        this.pinnedYn = pinnedYn;
        this.title = title;
        this.content = content;
        this.productNo = productNo;
    }
}
