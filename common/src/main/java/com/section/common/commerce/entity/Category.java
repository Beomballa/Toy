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
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_no")
    private Long categoryNo;

    @Column(name = "parent_no")
    private Long parentNo;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    @Column(name = "is_active", length = 1)
    private String isActive;

    public void update(String name, String isActive) {
        this.name = name;
        this.isActive = isActive;
    }

    public void changeStatus(String isActive) {
        this.isActive = isActive;
    }
}
