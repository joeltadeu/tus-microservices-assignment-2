package com.pms.patient.controller;

import static com.pms.patient.controller.constants.PatientConstants.*;

import com.pms.controller.PmsController;
import com.pms.models.dto.patient.PatientFilter;
import com.pms.models.dto.patient.PatientRequest;
import com.pms.models.dto.patient.PatientResponse;
import com.pms.models.validation.OnCreate;
import com.pms.models.validation.OnUpdate;
import com.pms.patient.controller.mapper.PatientMapper;
import com.pms.patient.model.Patient;
import com.pms.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/patients")
@AllArgsConstructor
@Slf4j
public class PatientController implements PmsController {

  private final PatientService service;
  private final PatientMapper patientMapper;

  private static final String OWNS_PATIENT =
      "hasRole('HEALTHCARE_ADMIN') or "
          + "(hasRole('PATIENT') and authentication.credentials == #id)";

  @Operation(
      summary = "Register a Patient",
      description = "This endpoint is responsible to register a new patient",
      security = @SecurityRequirement(name = AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_CREATED,
            description = "Patient created",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = PatientResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_BAD_REQUEST,
            description = "Patient request is invalid",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_BAD_REQUEST_NAME,
                        value = PATIENT_EXAMPLE_ERROR_400_BAD_REQUEST)
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
  @PostMapping
  public ResponseEntity<PatientResponse> insert(
      @RequestBody @Validated(OnCreate.class) @NotNull PatientRequest request) {
    log.info("Request for create a patient. patient:{}", request);
    var patient = patientMapper.toPatient(request);
    service.insert(patient, request.getInitialPassword());
    var response = patientMapper.toPatientResponse(patient);
    return ResponseEntity.created(getURI(response.getId())).body(response);
  }

  @Operation(
      summary = "Update a patient",
      description =
          "This endpoint is responsible to update the patient data by id. "
              + "A patient can only update their own record.",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "id",
            description = "Id of the patient to be updated",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Patient updated",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE)}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_BAD_REQUEST,
            description = "Patient request is invalid",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_BAD_REQUEST_NAME,
                        value = PATIENT_EXAMPLE_ERROR_400_BAD_REQUEST)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Patient not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        value = PATIENT_EXAMPLE_ERROR_404_NOT_FOUND)
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
  @PutMapping("/{id}")
  @PreAuthorize(OWNS_PATIENT)
  public ResponseEntity<Void> update(
      @PathVariable Long id,
      @RequestBody @Validated(OnUpdate.class) @NotNull PatientRequest request) {
    log.info("Request for update a patient. id:{}", id);
    var patient = patientMapper.toPatient(request);
    service.update(id, patient);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "Retrieve a patient by id",
      description =
          "This endpoint is responsible to retrieve the patient data by id. "
              + "A patient can only retrieve their own record. A doctor cannot access this endpoint.",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "id",
            description = "Id of the patient to be searched",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Return patient",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = PatientResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Patient not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        value = PATIENT_EXAMPLE_ERROR_404_NOT_FOUND)
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
  @GetMapping("/{id}")
  @PreAuthorize(OWNS_PATIENT)
  public ResponseEntity<PatientResponse> findById(@PathVariable Long id) {
    log.info("Request for find patient by id [{}]", id);
    final var patient = service.findById(id);
    return ResponseEntity.ok(patientMapper.toPatientResponse(patient));
  }

  @Operation(
      summary = "Delete the patient by id",
      description = "This endpoint is responsible to delete the patient by id",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "id",
            description = "Id of the patient to be deleted",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Patient deleted",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE)}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Patient not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        value = PATIENT_EXAMPLE_ERROR_404_NOT_FOUND)
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
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    log.info("Request for delete patient by id [{}]", id);
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Retrieve a list of patients",
      security = @SecurityRequirement(name = AUTHORIZATION),
      description = "This endpoint is responsible to retrieve a list of patients. Admin only.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = HTTP_STATUS_CODE_OK, description = "Return patients list"),
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
  @GetMapping
  public Page<PatientResponse> listAll(PatientFilter filter) {
    log.info("Request for list all patients");
    Page<Patient> patients = service.findAll(filter);
    log.info("Found [{}] results", patients.getTotalElements());
    var fetchedList =
        patients.stream().map(patientMapper::toPatientResponse).collect(Collectors.toList());
    return new PageImpl<>(fetchedList, patients.getPageable(), patients.getTotalElements());
  }
}
