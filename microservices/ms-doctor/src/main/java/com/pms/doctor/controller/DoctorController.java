package com.pms.doctor.controller;

import static com.pms.doctor.controller.constants.DoctorConstants.*;

import com.pms.controller.PmsController;
import com.pms.doctor.controller.mapper.DoctorMapper;
import com.pms.doctor.model.Doctor;
import com.pms.doctor.service.DoctorService;
import com.pms.models.dto.doctor.DoctorFilter;
import com.pms.models.dto.doctor.DoctorRequest;
import com.pms.models.dto.doctor.DoctorResponse;
import com.pms.models.validation.OnCreate;
import com.pms.models.validation.OnUpdate;
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
@RequestMapping("/v1/doctors")
@AllArgsConstructor
@Slf4j
public class DoctorController implements PmsController {

  private final DoctorService service;
  private final DoctorMapper doctorMapper;

  private static final String OWNS_DOCTOR =
      "hasRole('HEALTHCARE_ADMIN') or "
          + "(hasRole('DOCTOR') and authentication.credentials == #id)";

  private static final String CAN_READ_DOCTOR =
      "hasRole('HEALTHCARE_ADMIN') or "
          + "(hasRole('DOCTOR') and authentication.credentials == #id) or "
          + "hasRole('PATIENT')";

  @Operation(
      summary = "Register a Doctor",
      description = "This endpoint is responsible to register a new doctor",
      security = @SecurityRequirement(name = AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_CREATED,
            description = "Doctor created",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DoctorResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_BAD_REQUEST,
            description = "Doctor request is invalid",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_BAD_REQUEST_NAME,
                        value = DOCTOR_EXAMPLE_ERROR_400_BAD_REQUEST)
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
  public ResponseEntity<DoctorResponse> insert(
      @RequestBody @Validated(OnCreate.class) @NotNull DoctorRequest request) {
    log.info("Request for create a doctor. doctor:{}", request);
    var doctor = doctorMapper.toDoctor(request);
    service.insert(doctor, request.getInitialPassword());
    var response = doctorMapper.toDoctorResponse(doctor);
    return ResponseEntity.created(getURI(response.getId())).body(response);
  }

  @Operation(
      summary = "Update a doctor",
      description =
          "This endpoint is responsible to update the doctor data by id. "
              + "A doctor can only update their own record.",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "id",
            description = "Id of the doctor to be updated",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Doctor updated",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE)}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_BAD_REQUEST,
            description = "Doctor request is invalid",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_BAD_REQUEST_NAME,
                        value = DOCTOR_EXAMPLE_ERROR_400_BAD_REQUEST)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Doctor not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        value = DOCTOR_EXAMPLE_ERROR_404_NOT_FOUND)
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
  @PreAuthorize(OWNS_DOCTOR)
  public ResponseEntity<Void> update(
      @PathVariable Long id,
      @RequestBody @Validated(OnUpdate.class) @NotNull DoctorRequest request) {
    log.info("Request for update a doctor. id:{}", id);
    var doctor = doctorMapper.toDoctor(request);
    service.update(id, doctor);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "Retrieve a doctor by id",
      description =
          "This endpoint is responsible to retrieve the doctor data by id. "
              + "A doctor can only retrieve their own record. A patient cannot access this endpoint.",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "id",
            description = "Id of the doctor to be searched",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Return doctor",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DoctorResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Doctor not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        value = DOCTOR_EXAMPLE_ERROR_404_NOT_FOUND)
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
  // Admin sees any; doctor sees only their own record; patients cannot access
  @PreAuthorize(CAN_READ_DOCTOR)
  public ResponseEntity<DoctorResponse> findById(@PathVariable Long id) {
    log.info("Request for find doctor by id [{}]", id);
    final var doctor = service.findById(id);
    return ResponseEntity.ok(doctorMapper.toDoctorResponse(doctor));
  }

  @Operation(
      summary = "Delete the doctor by id",
      description = "This endpoint is responsible to delete the doctor by id",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "id",
            description = "Id of the doctor to be deleted",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Doctor deleted",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE)}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Doctor not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        value = DOCTOR_EXAMPLE_ERROR_404_NOT_FOUND)
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
    log.info("Request for delete doctor by id [{}]", id);
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Retrieve a list of doctors",
      security = @SecurityRequirement(name = AUTHORIZATION),
      description = "This endpoint is responsible to retrieve a list of doctors. Admin only.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = HTTP_STATUS_CODE_OK, description = "Return doctors list"),
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
  public Page<DoctorResponse> listAll(DoctorFilter filter) {
    log.info("Request for list all doctors");
    Page<Doctor> doctors = service.findAll(filter);
    log.info("Found [{}] results", doctors.getTotalElements());
    var fetchedList =
        doctors.stream().map(doctorMapper::toDoctorResponse).collect(Collectors.toList());
    return new PageImpl<>(fetchedList, doctors.getPageable(), doctors.getTotalElements());
  }
}
