package com.pms.doctor.service;

import com.pms.doctor.client.AuthClient;
import com.pms.doctor.model.Doctor;
import com.pms.doctor.repository.DoctorRepository;
import com.pms.doctor.repository.SpecialityRepository;
import com.pms.exception.BadRequestException;
import com.pms.exception.NotFoundException;
import com.pms.exception.ServiceUnavailableException;
import com.pms.models.dto.auth.CreateUserRequest;
import com.pms.models.dto.doctor.DoctorFilter;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DoctorService {

  private static final String ROLE_DOCTOR = "ROLE_DOCTOR";

  private final DoctorRepository doctorRepository;
  private final SpecialityRepository specialityRepository;
  private final AuthClient authClient;

  public Doctor findById(Long id) {
    return doctorRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Doctor with Id %s was not found".formatted(id)));
  }

  public Page<Doctor> findAll(DoctorFilter filter) {
    return doctorRepository.findAllWithFilters(filter);
  }

  /**
   * Saves the doctor domain record then creates an auth account in ms-auth. Both operations must
   * succeed — if ms-auth is unreachable the whole request fails and the DB insert is rolled back,
   * keeping domain data and auth data in sync.
   */
  @Transactional
  public void insert(Doctor doctor, String initialPassword) {
    log.info("Checking for duplicate email [{}]", doctor.getEmail());
    if (doctorRepository.existsByEmail(doctor.getEmail())) {
      throw new BadRequestException(
          "There is another doctor using the same email '%s' informed"
              .formatted(doctor.getEmail()));
    }

    var foundSpeciality =
        specialityRepository
            .findById(doctor.getSpeciality().getId())
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Speciality not found with id: " + doctor.getSpeciality().getId()));
    doctor.setSpeciality(foundSpeciality);
    doctor.setCreatedAt(LocalDateTime.now());
    doctorRepository.save(doctor);

    log.info("Doctor saved with id={}. Creating auth account...", doctor.getId());
    try {
      authClient.createUser(
          CreateUserRequest.builder()
              .email(doctor.getEmail())
              .password(initialPassword)
              .role(ROLE_DOCTOR)
              .domainId(doctor.getId())
              .build());
      log.info("Auth account created for doctorId={}", doctor.getId());
    } catch (Exception e) {
      log.error(
          "Failed to create auth account for doctorId={}: {}", doctor.getId(), e.getMessage());
      throw new ServiceUnavailableException(
          "Doctor record was saved but the auth account could not be created. "
              + "Please try again or contact support.");
    }
  }

  public void update(Long id, Doctor doctor) {
    log.info("Before update, checking if the doctor exists...");
    var savedDoctor = findById(id);
    savedDoctor.setEmail(doctor.getEmail());
    savedDoctor.setFirstName(doctor.getFirstName());
    savedDoctor.setLastName(doctor.getLastName());
    savedDoctor.setPhone(doctor.getPhone());
    savedDoctor.setDepartment(doctor.getDepartment());
    savedDoctor.setSpeciality(doctor.getSpeciality());
    savedDoctor.setTitle(doctor.getTitle());
    doctorRepository.save(savedDoctor);
  }

  /** Deletes the doctor domain record and soft-disables their auth account. */
  @Transactional
  public void delete(Long id) {
    log.info("Before delete, checking if the doctor exists...");
    final var doctor = findById(id);
    doctorRepository.delete(doctor);

    try {
      authClient.disableUser(doctor.getId(), ROLE_DOCTOR);
      log.info("Auth account disabled for doctorId={}", doctor.getId());
    } catch (Exception e) {
      // Auth disable failure is non-fatal for deletion — log and continue.
      // The account can be manually disabled if needed.
      log.warn(
          "Could not disable auth account for doctorId={}: {}", doctor.getId(), e.getMessage());
    }
  }
}
