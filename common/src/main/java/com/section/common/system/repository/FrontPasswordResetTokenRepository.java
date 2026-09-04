package com.section.common.system.repository;

import com.section.common.system.entity.FrontPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FrontPasswordResetTokenRepository extends JpaRepository<FrontPasswordResetToken, Long> {
    Optional<FrontPasswordResetToken> findByTokenHash(String tokenHash);
}
