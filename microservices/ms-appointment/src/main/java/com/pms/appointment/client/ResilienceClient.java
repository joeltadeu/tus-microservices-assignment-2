package com.pms.appointment.client;

import com.pms.exception.ServiceUnavailableException;
import com.pms.models.dto.doctor.DoctorResponse;
import com.pms.models.dto.patient.PatientResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Wraps all downstream Feign calls with Resilience4j @Retry and @CircuitBreaker.
 *
 * <p>These annotations rely on Spring AOP proxies. Placing them inside AppointmentService and
 * calling them from within the same bean bypasses the proxy (self-invocation), meaning the
 * annotations are silently ignored. By extracting the resilient calls into a separate @Component,
 * every call goes through the proxy and the decorators are applied correctly.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ResilienceClient {

  private static final String DOCTOR_CB = "doctorClient";
  private static final String PATIENT_CB = "patientClient";

  private final DoctorClient doctorClient;
  private final PatientClient patientClient;

  // ── Soft fetches — used by reads, fallback returns an id-only shell ───────

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

  // ── Hard validates — used by writes, fallback throws 503 ─────────────────

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
}
