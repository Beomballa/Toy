package com.section.common.system.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "admin_system_setting")
public class AdminSystemSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_no")
    private Long settingNo;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 500)
    private String settingValue;

    @Column(name = "description", length = 500)
    private String description;

    public void updateValue(String settingValue) {
        this.settingValue = settingValue;
    }
}
