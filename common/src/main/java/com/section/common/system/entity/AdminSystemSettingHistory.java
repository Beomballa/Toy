package com.section.common.system.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_system_setting_history")
@Access(AccessType.FIELD)
public class AdminSystemSettingHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_no")
    private Long historyNo;

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_name", nullable = false, length = 100)
    private String settingName;

    @Column(name = "before_value", length = 500)
    private String beforeValue;

    @Column(name = "after_value", nullable = false, length = 500)
    private String afterValue;

    @Column(name = "change_summary", nullable = false, length = 500)
    private String changeSummary;

    @Column(name = "changed_ip_address", nullable = false, length = 50)
    private String changedIpAddress;
}
