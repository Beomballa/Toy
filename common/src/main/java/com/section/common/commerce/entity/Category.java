package com.section.common.commerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "CATEGORY")
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_no")
    private Long id;

    @Column(name = "parent_no")
    private Long parentNo;

    @Column(name = "name", nullable = false)
    private String name;

    private int depth;

    @Column(name = "is_active")
    @Builder.Default
    private String isActive = "Y";
}
