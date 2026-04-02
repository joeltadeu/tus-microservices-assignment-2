package com.pms.auth.repository;

import com.pms.auth.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  /**
   * Finds a user by their linked domain entity id and role name. Used when disabling an account
   * after a doctor or patient is deleted.
   */
  @Query("SELECT u FROM User u JOIN u.roles r WHERE u.domainId = :domainId AND r.name = :roleName")
  Optional<User> findByDomainIdAndRoleName(Long domainId, String roleName);
}
