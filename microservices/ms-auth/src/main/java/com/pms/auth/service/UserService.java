package com.pms.auth.service;

import com.pms.auth.model.User;
import com.pms.auth.repository.RoleRepository;
import com.pms.auth.repository.UserRepository;
import com.pms.exception.BadRequestException;
import com.pms.models.dto.auth.CreateUserRequest;
import com.pms.models.dto.auth.CreateUserResponse;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Creates a new auth user account linked to a domain entity (doctor or patient). Called by
   * ms-doctor and ms-patient after persisting their own domain record. Only HEALTHCARE_ADMIN may
   * trigger this — enforced at the controller layer.
   */
  @Transactional
  public CreateUserResponse create(CreateUserRequest request) {
    log.info(
        "Creating auth user for email={}, role={}, domainId={}",
        request.getEmail(),
        request.getRole(),
        request.getDomainId());

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new BadRequestException(
          "An account with email '%s' already exists".formatted(request.getEmail()));
    }

    var role =
        roleRepository
            .findByName(request.getRole())
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Unknown role '%s'. Accepted: ROLE_DOCTOR, ROLE_PATIENT, ROLE_HEALTHCARE_ADMIN"
                            .formatted(request.getRole())));

    var user =
        User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .domainId(request.getDomainId())
            .enabled(true)
            .roles(Set.of(role))
            .build();

    userRepository.save(user);
    log.info("Auth user created with id={} for email={}", user.getId(), user.getEmail());

    return CreateUserResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .role(role.getName())
        .domainId(user.getDomainId())
        .build();
  }

  /**
   * Disables an auth account — called when a doctor or patient is deleted. Uses soft-disable rather
   * than hard delete to preserve audit history.
   */
  @Transactional
  public void disableByDomainId(Long domainId, String role) {
    log.info("Disabling auth user for domainId={}, role={}", domainId, role);
    userRepository
        .findByDomainIdAndRoleName(domainId, role)
        .ifPresent(
            user -> {
              user.setEnabled(false);
              userRepository.save(user);
              log.info("Auth user id={} disabled", user.getId());
            });
  }
}
