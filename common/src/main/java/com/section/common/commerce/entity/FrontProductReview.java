package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "front_product_review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_front_product_review_member_order_product",
                columnNames = {"member_no", "order_no", "product_no"}
        )
)
public class FrontProductReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_no")
    private Long id;

    @Column(name = "member_no", nullable = false)
    private Long memberNo;

    @Column(name = "product_no", nullable = false)
    private Long productNo;

    @Column(name = "order_no", nullable = false)
    private Long orderNo;

    @Column(name = "reviewer_name", nullable = false, length = 40)
    private String reviewerName;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    private FrontProductReview(
            Long memberNo,
            Long productNo,
            Long orderNo,
            String reviewerName,
            int rating,
            String content
    ) {
        this.memberNo = memberNo;
        this.productNo = productNo;
        this.orderNo = orderNo;
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.content = content;
    }

    public static FrontProductReview create(
            long memberNo,
            long productNo,
            long orderNo,
            String reviewerName,
            int rating,
            String content
    ) {
        return new FrontProductReview(memberNo, productNo, orderNo, reviewerName, rating, content);
    }
}
