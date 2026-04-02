package com.pms.patient.service;

import com.pms.exception.BadRequestException;
import com.pms.exception.NotFoundException;
import com.pms.exception.ServiceUnavailableException;
import com.pms.models.dto.auth.CreateUserRequest;
import com.pms.models.dto.patient.PatientFilter;
import com.pms.patient.client.AuthClient;
import com.pms.patient.model.Patient;
import com.pms.patient.repository.PatientRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PatientService {

  private static final String ROLE_PATIENT = "ROLE_PATIENT";

  private final PatientRepository repository;
  private final AuthClient authClient;

  public Patient findById(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Patient with Id %s was not found".formatted(id)));
  }

  public Page<Patient> findAll(PatientFilter filter) {
    return repository.findAllWithFilters(filter);
  }

  /**
   * Saves the patient domain record then creates an auth account in ms-auth. Both operations must
   * succeed — if ms-auth is unreachable the whole request fails and the DB insert is rolled back.
   */
  @Transactional
  public void insert(Patient patient, String initialPassword) {
    log.info("Checking for duplicate email [{}]", patient.getEmail());
    if (repository.existsByEmail(patient.getEmail())) {
      throw new BadRequestException(
          "There is another patient using the same email '%s' informed"
              .formatted(patient.getEmail()));
    }

    patient.setCreatedAt(LocalDateTime.now());
    repository.save(patient);

    log.info("Patient saved with id={}. Creating auth account...", patient.getId());
    try {
      authClient.createUser(
          CreateUserRequest.builder()
              .email(patient.getEmail())
              .password(initialPassword)
              .role(ROLE_PATIENT)
              .domainId(patient.getId())
              .build());
      log.info("Auth account created for patientId={}", patient.getId());
    } catch (Exception e) {
      log.error(
          "Failed to create auth account for patientId={}: {}", patient.getId(), e.getMessage());
      throw new ServiceUnavailableException(
          "Patient record was saved but the auth account could not be created. "
              + "Please try again or contact support.");
    }
  }

  public void update(Long id, Patient patient) {
    log.info("Before update, checking if the patient exists...");
    var savedPatient = findById(id);
    savedPatient.setEmail(patient.getEmail());
    savedPatient.setFirstName(patient.getFirstName());
    savedPatient.setLastName(patient.getLastName());
    savedPatient.setAddress(patient.getAddress());
    savedPatient.setDateOfBirth(patient.getDateOfBirth());
    repository.save(savedPatient);
  }

  /** Deletes the patient domain record and soft-disables their auth account. */
  @Transactional
  public void delete(Long id) {
    log.info("Before delete, checking if the patient exists...");
    final var patient = findById(id);
    repository.delete(patient);

    try {
      authClient.disableUser(patient.getId(), ROLE_PATIENT);
      log.info("Auth account disabled for patientId={}", patient.getId());
    } catch (Exception e) {
      log.warn(
          "Could not disable auth account for patientId={}: {}", patient.getId(), e.getMessage());
    }
  }
}
