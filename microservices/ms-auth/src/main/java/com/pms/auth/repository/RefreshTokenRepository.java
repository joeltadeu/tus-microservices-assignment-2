package com.pms.auth.repository;

import com.pms.auth.model.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /** Revokes all active sessions for a user (used on login and logout). */
  @Modifying
  @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId")
  void revokeAllByUserId(Long userId);
}
