package com.pms.appointment.controller.mapper;

import com.pms.appointment.model.Appointment;
import com.pms.models.dto.appointment.AppointmentRequest;
import com.pms.models.dto.appointment.AppointmentResponse;
import com.pms.models.dto.appointment.DoctorAppointment;
import com.pms.models.dto.appointment.PatientAppointment;
import com.pms.models.dto.doctor.DoctorResponse;
import com.pms.models.dto.patient.PatientResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

  public Appointment toAppointment(
      @NotNull Long patientId, @Valid @NotNull AppointmentRequest request) {
    return Appointment.builder()
        .patientId(patientId)
        .doctorId(request.getDoctorId())
        .startTime(request.getStartTime())
        .type(request.getType())
        .title(request.getTitle())
        .description(request.getDescription())
        .build();
  }

  public AppointmentResponse toAppointmentResponse(Appointment appointment) {
    return AppointmentResponse.builder()
        .id(appointment.getId())
        .patientId(appointment.getPatientId())
        .doctorId(appointment.getDoctorId())
        .startTime(appointment.getStartTime())
        .endTime(appointment.getEndTime())
        .duration(appointment.getDuration())
        .description(appointment.getDescription())
        .title(appointment.getTitle())
        .type(appointment.getType())
        .status(appointment.getStatus())
        .cancellationReason(appointment.getCancellationReason())
        .cancellationTime(appointment.getCancellationTime())
        .build();
  }

  /**
   * Enriched mapping used by findByIdEnriched.
   *
   * <p>When a downstream service is degraded, the fallback supplies a partial response (id-only).
   * In that case the rich nested object (doctor / patient) is omitted and only the raw id field is
   * populated, keeping the response schema stable.
   */
  public AppointmentResponse toAppointmentResponse(
      Appointment appointment, DoctorResponse doctor, PatientResponse patient) {

    DoctorAppointment doctorAppointment = toDoctorAppointment(doctor, appointment.getDoctorId());
    PatientAppointment patientAppointment =
        toPatientAppointment(patient, appointment.getPatientId());

    return AppointmentResponse.builder()
        .id(appointment.getId())
        // Populate the rich object only when the full data is available
        .doctor(doctorAppointment.isEnriched() ? doctorAppointment : null)
        .patient(patientAppointment.isEnriched() ? patientAppointment : null)
        // Always populate raw ids as fallback reference
        .doctorId(doctorAppointment.isEnriched() ? null : doctor.getId())
        .patientId(patientAppointment.isEnriched() ? null : patient.getId())
        .startTime(appointment.getStartTime())
        .endTime(appointment.getEndTime())
        .duration(appointment.getDuration())
        .description(appointment.getDescription())
        .title(appointment.getTitle())
        .type(appointment.getType())
        .status(appointment.getStatus())
        .cancellationReason(appointment.getCancellationReason())
        .cancellationTime(appointment.getCancellationTime())
        .build();
  }

  private PatientAppointment toPatientAppointment(PatientResponse patient, Long fallbackId) {
    if (patient == null) {
      return PatientAppointment.idOnly(fallbackId);
    }
    // Fallback response has only id set — treat it as degraded
    if (patient.getFirstName() == null) {
      return PatientAppointment.idOnly(patient.getId());
    }
    return PatientAppointment.builder()
        .id(patient.getId())
        .firstName(patient.getFirstName())
        .lastName(patient.getLastName())
        .email(patient.getEmail())
        .build();
  }

  private DoctorAppointment toDoctorAppointment(DoctorResponse doctor, Long fallbackId) {
    if (doctor == null) {
      return DoctorAppointment.idOnly(fallbackId);
    }
    // Fallback response has only id set — treat it as degraded
    if (doctor.getFirstName() == null) {
      return DoctorAppointment.idOnly(doctor.getId());
    }
    return DoctorAppointment.builder()
        .id(doctor.getId())
        .firstName(doctor.getFirstName())
        .lastName(doctor.getLastName())
        .title(doctor.getTitle())
        .speciality(doctor.getSpeciality())
        .build();
  }
}
