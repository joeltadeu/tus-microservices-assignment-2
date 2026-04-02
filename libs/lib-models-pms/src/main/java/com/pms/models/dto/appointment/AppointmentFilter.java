package com.pms.models.dto.appointment;

import com.pms.models.dto.PmsFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppointmentFilter extends PmsFilter {
  @Schema(description = "Patient Id to be searched", name = "patientId", example = "14")
  private Long patientId;

  @Schema(description = "Doctor Id to be searched", name = "doctorId", example = "14")
  private Long doctorId;

  @Schema(description = "Appointment status be searched", name = "status", example = "14")
  private AppointmentStatus status;

  @Schema(description = "Appointment start date be searched", name = "startDate", example = "2026-01-20")
  private LocalDate startDate;

  @Schema(description = "Appointment end date be searched", name = "status", example = "2026-01-22")
  private LocalDate endDate;
}
