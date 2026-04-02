package com.pms.patient.controller.mapper;

import com.pms.models.dto.patient.PatientRequest;
import com.pms.models.dto.patient.PatientResponse;
import com.pms.patient.model.Patient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {
  public Patient toPatient(@Valid @NotNull PatientRequest request) {
    return Patient.builder()
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .email(request.getEmail())
        .address(request.getAddress())
        .dateOfBirth(request.getDateOfBirth())
        .createdAt(LocalDateTime.now())
        .build();
  }

  public PatientResponse toPatientResponse(@NotNull Patient patient) {
    return new PatientResponse(
        patient.getId(),
        patient.getFirstName(),
        patient.getLastName(),
        patient.getEmail(),
        patient.getAddress(),
        patient.getDateOfBirth());
  }
}
