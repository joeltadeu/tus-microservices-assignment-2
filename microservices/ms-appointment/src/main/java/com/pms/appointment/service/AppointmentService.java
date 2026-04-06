package com.pms.appointment.service;

import com.pms.appointment.client.ResilienceClient;
import com.pms.appointment.controller.mapper.AppointmentMapper;
import com.pms.appointment.model.Appointment;
import com.pms.appointment.repository.AppointmentRepository;
import com.pms.exception.ConflictException;
import com.pms.exception.NotFoundException;
import com.pms.models.dto.appointment.AppointmentFilter;
import com.pms.models.dto.appointment.AppointmentRequest;
import com.pms.models.dto.appointment.AppointmentResponse;
import com.pms.models.dto.appointment.AppointmentStatus;
import com.pms.models.dto.appointment.CancelAppointmentRequest;
import com.pms.models.dto.doctor.DoctorResponse;
import com.pms.models.dto.patient.PatientResponse;
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

  private final AppointmentRepository repository;
  private final AppointmentMapper mapper;
  private final ResilienceClient resilienceClient; // replaces direct DoctorClient/PatientClient

  // ── Enriched reads (soft fallback — degrade gracefully) ───────────────────

  public AppointmentResponse findByIdEnriched(Long patientId, Long id) {
    var appointment = findById(id, patientId);
    DoctorResponse doctor = resilienceClient.fetchDoctor(appointment.getDoctorId());
    PatientResponse patient = resilienceClient.fetchPatient(appointment.getPatientId());
    return mapper.toAppointmentResponse(appointment, doctor, patient);
  }

  public Page<AppointmentResponse> findAll(AppointmentFilter filter) {
    Page<Appointment> page = repository.findAllWithFilters(filter);

    Set<Long> doctorIds = page.stream().map(Appointment::getDoctorId).collect(Collectors.toSet());

    Map<Long, DoctorResponse> doctorCache =
        doctorIds.stream()
            .collect(Collectors.toMap(Function.identity(), resilienceClient::fetchDoctor));

    Set<Long> patientIds = page.stream().map(Appointment::getPatientId).collect(Collectors.toSet());

    Map<Long, PatientResponse> patientCache =
        patientIds.stream()
            .collect(Collectors.toMap(Function.identity(), resilienceClient::fetchPatient));

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
        doctorIds.stream()
            .collect(Collectors.toMap(Function.identity(), resilienceClient::fetchDoctor));

    PatientResponse patientResponse = resilienceClient.fetchPatient(patientId);

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
    var doctor = resilienceClient.validateDoctor(appointment.getDoctorId());
    var patient = resilienceClient.validatePatient(appointment.getPatientId());

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
    var doctor = resilienceClient.validateDoctor(request.getDoctorId());
    var patient = resilienceClient.validatePatient(patientId);

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
