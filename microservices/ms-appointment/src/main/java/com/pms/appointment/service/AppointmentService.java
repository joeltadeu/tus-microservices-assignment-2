package com.pms.appointment.service;

import com.pms.appointment.client.DoctorClient;
import com.pms.appointment.client.PatientClient;
import com.pms.appointment.controller.mapper.AppointmentMapper;
import com.pms.appointment.model.Appointment;
import com.pms.appointment.repository.AppointmentRepository;
import com.pms.exception.ConflictException;
import com.pms.exception.NotFoundException;
import com.pms.exception.ServiceUnavailableException;
import com.pms.models.dto.appointment.AppointmentFilter;
import com.pms.models.dto.appointment.AppointmentRequest;
import com.pms.models.dto.appointment.AppointmentResponse;
import com.pms.models.dto.appointment.AppointmentStatus;
import com.pms.models.dto.appointment.CancelAppointmentRequest;
import com.pms.models.dto.doctor.DoctorResponse;
import com.pms.models.dto.patient.PatientResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {

  private static final String DOCTOR_CB = "doctorClient";
  private static final String PATIENT_CB = "patientClient";

  private final AppointmentRepository repository;
  private final AppointmentMapper mapper;
  private final DoctorClient doctorClient;
  private final PatientClient patientClient;

  // ── Enriched reads (soft fallback — degrade gracefully) ───────────────────

  public AppointmentResponse findByIdEnriched(Long patientId, Long id) {
    var appointment = findById(id, patientId);
    DoctorResponse doctor = fetchDoctor(appointment.getDoctorId());
    PatientResponse patient = fetchPatient(appointment.getPatientId());
    return mapper.toAppointmentResponse(appointment, doctor, patient);
  }

  public Page<AppointmentResponse> findAll(AppointmentFilter filter) {
    Page<Appointment> page = repository.findAllWithFilters(filter);

    Set<Long> doctorIds = page.stream().map(Appointment::getDoctorId).collect(Collectors.toSet());

    Map<Long, DoctorResponse> doctorCache =
        doctorIds.stream().collect(Collectors.toMap(Function.identity(), this::fetchDoctor));

    Set<Long> patientIds = page.stream().map(Appointment::getPatientId).collect(Collectors.toSet());

    Map<Long, PatientResponse> patientCache =
        patientIds.stream().collect(Collectors.toMap(Function.identity(), this::fetchPatient));

    var enrichedContent =
        page.stream()
            .map(
                appt ->
                    mapper.toAppointmentResponse(
                        appt,
                        doctorCache.get(appt.getDoctorId()),
                        patientCache.get(appt.getPatientId())))
            .collect(Collectors.toList());

    return new PageImpl<>(enrichedContent, page.getPageable(), page.getTotalElements());
  }

  public Page<AppointmentResponse> findAllByPatientId(Long patientId, AppointmentFilter filter) {
    Page<Appointment> page = repository.findAllWithFilters(patientId, filter);

    Set<Long> doctorIds = page.stream().map(Appointment::getDoctorId).collect(Collectors.toSet());

    Map<Long, DoctorResponse> doctorCache =
        doctorIds.stream().collect(Collectors.toMap(Function.identity(), this::fetchDoctor));

    PatientResponse patientResponse = fetchPatient(patientId);

    var enrichedContent =
        page.stream()
            .map(
                appt ->
                    mapper.toAppointmentResponse(
                        appt, doctorCache.get(appt.getDoctorId()), patientResponse))
            .collect(Collectors.toList());

    return new PageImpl<>(enrichedContent, page.getPageable(), page.getTotalElements());
  }

  // ── Writes (hard fail — no write without a verified doctor and patient) ───

  public Appointment insert(Appointment appointment) {
    // Both must succeed — a write with an unverified reference is data corruption.
    // validateDoctor/validatePatient retry then throw ServiceUnavailableException
    // if the downstream is still unreachable, which maps to HTTP 503.
    var doctor = validateDoctor(appointment.getDoctorId());
    var patient = validatePatient(appointment.getPatientId());

    appointment.setDoctorId(doctor.getId());
    appointment.setPatientId(patient.getId());
    appointment.setCreatedAt(LocalDateTime.now());
    appointment.setEndTime(appointment.getStartTime().plusHours(1));
    appointment.setDuration(60);
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    repository.save(appointment);

    return appointment;
  }

  public Appointment update(Long id, Long patientId, AppointmentRequest request) {
    var doctor = validateDoctor(request.getDoctorId());
    var patient = validatePatient(patientId);

    var appointment = findById(id, patientId);
    validateScheduledStatus(appointment, "updated");

    appointment.setDoctorId(doctor.getId());
    appointment.setPatientId(patient.getId());
    appointment.setTitle(request.getTitle());
    appointment.setDescription(request.getDescription());
    appointment.setStartTime(request.getStartTime());
    appointment.setEndTime(appointment.getStartTime().plusHours(1));
    appointment.setDuration(60);
    repository.save(appointment);

    return appointment;
  }

  // ── Remote calls: soft — used by reads, fallback returns id-only ──────────

  @Retry(name = DOCTOR_CB, fallbackMethod = "fetchDoctorFallback")
  @CircuitBreaker(name = DOCTOR_CB, fallbackMethod = "fetchDoctorFallback")
  public DoctorResponse fetchDoctor(Long doctorId) {
    log.debug("Fetching doctor id={}", doctorId);
    return doctorClient.findById(doctorId);
  }

  @Retry(name = PATIENT_CB, fallbackMethod = "fetchPatientFallback")
  @CircuitBreaker(name = PATIENT_CB, fallbackMethod = "fetchPatientFallback")
  public PatientResponse fetchPatient(Long patientId) {
    log.debug("Fetching patient id={}", patientId);
    return patientClient.findById(patientId);
  }

  // ── Remote calls: hard — used by writes, fallback throws 503 ─────────────

  @Retry(name = DOCTOR_CB, fallbackMethod = "validateDoctorFallback")
  @CircuitBreaker(name = DOCTOR_CB, fallbackMethod = "validateDoctorFallback")
  public DoctorResponse validateDoctor(Long doctorId) {
    log.debug("Validating doctor id={}", doctorId);
    return doctorClient.findById(doctorId);
  }

  @Retry(name = PATIENT_CB, fallbackMethod = "validatePatientFallback")
  @CircuitBreaker(name = PATIENT_CB, fallbackMethod = "validatePatientFallback")
  public PatientResponse validatePatient(Long patientId) {
    log.debug("Validating patient id={}", patientId);
    return patientClient.findById(patientId);
  }

  // ── Soft fallbacks (reads) ────────────────────────────────────────────────

  public DoctorResponse fetchDoctorFallback(Long doctorId, Throwable ex) {
    log.warn(
        "Doctor service unavailable for doctorId={}. Falling back to id-only. Cause: {}",
        doctorId,
        ex.getMessage());
    var fallback = new DoctorResponse();
    fallback.setId(doctorId);
    return fallback;
  }

  public PatientResponse fetchPatientFallback(Long patientId, Throwable ex) {
    log.warn(
        "Patient service unavailable for patientId={}. Falling back to id-only. Cause: {}",
        patientId,
        ex.getMessage());
    var fallback = new PatientResponse();
    fallback.setId(patientId);
    return fallback;
  }

  // ── Hard fallbacks (writes) ───────────────────────────────────────────────

  public DoctorResponse validateDoctorFallback(Long doctorId, Throwable ex) {
    log.error(
        "Doctor service unavailable for doctorId={}. Blocking write. Cause: {}",
        doctorId,
        ex.getMessage());
    throw new ServiceUnavailableException(
        "Doctor service is currently unavailable. Cannot validate doctor id %d. Please try again later."
            .formatted(doctorId));
  }

  public PatientResponse validatePatientFallback(Long patientId, Throwable ex) {
    log.error(
        "Patient service unavailable for patientId={}. Blocking write. Cause: {}",
        patientId,
        ex.getMessage());
    throw new ServiceUnavailableException(
        "Patient service is currently unavailable. Cannot validate patient id %d. Please try again later."
            .formatted(patientId));
  }

  // ── Standard CRUD ─────────────────────────────────────────────────────────

  public Appointment findById(Long id, Long patientId) {
    return repository
        .findByIdAndPatientId(id, patientId)
        .orElseThrow(
            () -> new NotFoundException("Appointment with Id %s was not found".formatted(id)));
  }

  public Appointment cancel(
      Long id, Long patientId, CancelAppointmentRequest cancelAppointmentRequest) {
    log.info("Cancelling appointment {} for patient {}...", id, patientId);

    var appointment = findById(id, patientId);
    validateScheduledStatus(appointment, "cancelled");

    appointment.setCancellationReason(cancelAppointmentRequest.getReason());
    appointment.setCancellationTime(LocalDateTime.now());
    appointment.setLastUpdated(LocalDateTime.now());
    appointment.setStatus(AppointmentStatus.CANCELLED);

    repository.save(appointment);
    return appointment;
  }

  public void delete(Long patientId, Long id) {
    log.info("Deleting appointment {} for patient {}...", id, patientId);

    var appointment = findById(id, patientId);
    validateScheduledStatus(appointment, "deleted");

    log.info("Appointment {} deleted successfully.", id);
    repository.delete(appointment);
  }

  private void validateScheduledStatus(Appointment appointment, String action) {
    if (!AppointmentStatus.SCHEDULED.equals(appointment.getStatus())) {
      throw new ConflictException(
          "Appointment cannot be %s because it is not in the SCHEDULED state. Current status: '%s'."
              .formatted(action, appointment.getStatus()));
    }
  }
}
