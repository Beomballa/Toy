package com.section.common.base.entity.type;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
@Setter
public class BaseEntity {

    @CreatedDate
    @Column(name = "CRT_DTM", updatable = false)
    protected LocalDateTime crtDtm;

    @CreatedBy
    @Column(name = "CRT_ADMIN_NO")
    protected String crtNo;

    @LastModifiedDate
    @Column(name = "UPT_DTM")
    protected LocalDateTime uptDtm;

    @LastModifiedBy
    @Column(name = "UPT_ADMIN_NO")
    protected String uptNo;
}
