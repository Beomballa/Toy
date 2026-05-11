package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import com.section.common.base.entity.type.ProductHistoryActionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "product_change_history")
public class ProductChangeHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_no")
    private Long historyNo;

    @Column(name = "product_no", nullable = false)
    private Long productNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ProductHistoryActionType actionType;

    @Column(name = "summary", nullable = false, length = 1000)
    private String summary;

    @Column(name = "status_snapshot", length = 20)
    private String statusSnapshot;

    @Column(name = "option_count", nullable = false)
    private Integer optionCount;

    @Column(name = "total_stock", nullable = false)
    private Long totalStock;

    public static ProductChangeHistory of(
            Long productNo,
            ProductHistoryActionType actionType,
            String summary,
            String statusSnapshot,
            int optionCount,
            long totalStock
    ) {
        return ProductChangeHistory.builder()
                .productNo(productNo)
                .actionType(actionType)
                .summary(summary)
                .statusSnapshot(statusSnapshot)
                .optionCount(optionCount)
                .totalStock(totalStock)
                .build();
    }
}
