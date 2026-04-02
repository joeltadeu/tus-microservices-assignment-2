package com.pms.appointment.controller;

import static com.pms.appointment.controller.constants.AppointmentConstants.*;

import com.pms.appointment.controller.mapper.AppointmentMapper;
import com.pms.appointment.service.AppointmentService;
import com.pms.controller.PmsController;
import com.pms.models.dto.appointment.AppointmentFilter;
import com.pms.models.dto.appointment.AppointmentRequest;
import com.pms.models.dto.appointment.AppointmentResponse;
import com.pms.models.dto.appointment.CancelAppointmentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Slf4j
public class AppointmentController implements PmsController {

  private final AppointmentService service;
  private final AppointmentMapper mapper;

  private static final String OWNS_PATIENT =
      "hasRole('HEALTHCARE_ADMIN') or "
          + "(hasRole('PATIENT') and authentication.credentials == #patientId)";

  @Operation(
      summary = "Register an Appointment",
      description =
          "Creates a new appointment for the given patient. "
              + "A patient can only create appointments for themselves.",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_CREATED,
            description = "Appointment created",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = AppointmentResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_BAD_REQUEST,
            description = "Appointment request is invalid",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_BAD_REQUEST_NAME,
                        value = APPOINTMENT_EXAMPLE_ERROR_400_BAD_REQUEST)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @PostMapping("/v1/patients/{patientId}/appointments")
  @PreAuthorize(OWNS_PATIENT)
  public ResponseEntity<AppointmentResponse> create(
      @PathVariable Long patientId, @RequestBody @Valid @NotNull AppointmentRequest request) {

    log.info("Creating appointment for patientId={}, payload={}", patientId, request);
    var appointment = mapper.toAppointment(patientId, request);
    var savedAppointment = service.insert(appointment);
    var response = mapper.toAppointmentResponse(savedAppointment);
    return ResponseEntity.created(getURI(savedAppointment.getId())).body(response);
  }

  @Operation(
      summary = "Update the appointment by id",
      description = "Updates an appointment. A patient can only update their own appointments.",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH),
        @Parameter(
            name = "id",
            description = "Id of the appointment",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Appointment updated",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = AppointmentResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Appointment not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        value = APPOINTMENT_EXAMPLE_ERROR_404_NOT_FOUND)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @PutMapping("/v1/patients/{patientId}/appointments/{id}")
  @PreAuthorize(OWNS_PATIENT)
  public ResponseEntity<AppointmentResponse> update(
      @PathVariable Long patientId,
      @PathVariable Long id,
      @RequestBody @Valid @NotNull AppointmentRequest request) {

    log.info("Updating appointment id={} for patientId={}, payload={}", id, patientId, request);
    var appointment = service.update(id, patientId, request);
    var response = mapper.toAppointmentResponse(appointment);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Cancel the appointment by id",
      description = "Cancels an appointment. A patient can only cancel their own appointments.",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH),
        @Parameter(
            name = "id",
            description = "Id of the appointment",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Appointment cancelled",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = AppointmentResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Appointment not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        value = APPOINTMENT_EXAMPLE_ERROR_404_NOT_FOUND)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @PostMapping("/v1/patients/{patientId}/appointments/{id}/cancel")
  @PreAuthorize(OWNS_PATIENT)
  public ResponseEntity<AppointmentResponse> cancel(
      @PathVariable Long patientId,
      @PathVariable Long id,
      @RequestBody CancelAppointmentRequest request) {
    log.info(
        "Cancelling appointment id={} for patientId={}, reason={}",
        id,
        patientId,
        request.getReason());
    var cancelledAppointment = service.cancel(id, patientId, request);
    return ResponseEntity.ok(mapper.toAppointmentResponse(cancelledAppointment));
  }

  @Operation(
      summary = "Retrieve an appointment by id",
      description =
          "Returns a single enriched appointment. "
              + "A patient can only retrieve their own appointments.",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH),
        @Parameter(
            name = "id",
            description = "Id of the appointment",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Return appointment",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = AppointmentResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Appointment not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        value = APPOINTMENT_EXAMPLE_ERROR_404_NOT_FOUND)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @GetMapping("/v1/patients/{patientId}/appointments/{id}")
  @PreAuthorize(OWNS_PATIENT)
  public ResponseEntity<AppointmentResponse> findById(
      @PathVariable Long patientId, @PathVariable Long id) {

    log.info("Fetching appointment id={} for patientId={}", id, patientId);
    var response = service.findByIdEnriched(patientId, id);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Delete the appointment by id",
      description = "Hard deletes an appointment. Admin only.",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH),
        @Parameter(
            name = "id",
            description = "Id of the appointment",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Appointment deleted",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE)}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Appointment not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        value = APPOINTMENT_EXAMPLE_ERROR_404_NOT_FOUND)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @DeleteMapping("/v1/patients/{patientId}/appointments/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long patientId, @PathVariable Long id) {
    log.info("Deleting appointment id={} for patientId={}", id, patientId);
    service.delete(patientId, id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Retrieve a list of appointments by patient",
      security = @SecurityRequirement(name = AUTHORIZATION),
      description =
          "Returns a paged list of enriched appointments. "
              + "A patient can only list their own appointments.",
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = HTTP_STATUS_CODE_OK, description = "Return appointments list"),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @GetMapping("/v1/patients/{patientId}/appointments")
  @PreAuthorize(OWNS_PATIENT)
  public Page<AppointmentResponse> list(@PathVariable Long patientId, AppointmentFilter filter) {

    log.info("Listing appointments for patientId={}, filter={}", patientId, filter);
    var page = service.findAllByPatientId(patientId, filter);
    log.info("Found {} appointments for patientId={}", page.getTotalElements(), patientId);
    return page;
  }

  @GetMapping("/v1/appointments")
  @PreAuthorize("hasRole('HEALTHCARE_ADMIN')")
  public Page<AppointmentResponse> list(AppointmentFilter filter) {

    log.info("Listing appointments for filter={}", filter);
    var page = service.findAll(filter);
    log.info("Found {} appointments ", page.getTotalElements());
    return page;
  }
}
