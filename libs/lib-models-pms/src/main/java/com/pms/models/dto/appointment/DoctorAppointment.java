package com.pms.models.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DoctorAppointment {

  @Schema(description = "Doctor id", name = "id", example = "12")
  private Long id;

  @Schema(description = "Doctor first name", name = "firstName", example = "John")
  private String firstName;

  @Schema(description = "Doctor last name", name = "lastName", example = "Foreman")
  private String lastName;

  @Schema(description = "Doctor title", name = "title", example = "Dr.")
  private String title;

  @Schema(description = "Doctor speciality", name = "speciality", example = "Primary Care")
  private String speciality;

  /**
   * Returns {@code true} when this object was populated from a live response (i.e. it carries more
   * than just the id). Used by the mapper to decide whether to embed the full object or fall back
   * to the raw id field.
   */
  @Schema(hidden = true)
  public boolean isEnriched() {
    return firstName != null;
  }

  /** Factory for a degraded/fallback instance that carries only the id. */
  public static DoctorAppointment idOnly(Long id) {
    return DoctorAppointment.builder().id(id).build();
  }
}
