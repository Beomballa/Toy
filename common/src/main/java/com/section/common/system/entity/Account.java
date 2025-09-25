package com.section.common.system.entity;

import com.section.common.base.entity.type.BaseEntity;
import com.section.common.base.entity.type.YN;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "SY_ACCOUNT")
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "NAME")
    private String name;

    @Column(name = "NICKNAME")
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "MASTER_YN")
    private YN masterYn;

    @Enumerated(EnumType.STRING)
    @Column(name = "INIT_YN")
    private YN initYn;

    @Column(name = "PROFILE_IMG_PATH")
    private String profileImgPath;

    @Column(name = "PROFILE_IMG_NAME")
    private String profileImgName;

    @Enumerated(EnumType.STRING)
    @Column(name = "DEL_YN")
    private YN delYn;

    @Column(name = "TMP_PW_ISSUE_DT")
    private LocalDateTime tmpPwIssueDt;
}
