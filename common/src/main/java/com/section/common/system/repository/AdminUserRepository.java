package com.section.common.system.repository;

import com.section.common.system.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long>, CustomAdminUserRepository {
    Optional<AdminUser> findByLoginId(String loginId);

    boolean existsByLoginIdIgnoreCase(String loginId);

    List<AdminUser> findAllByStatusOrderByNameAsc(String status);

    long countByRoleAndStatus(String role, String status);
}
