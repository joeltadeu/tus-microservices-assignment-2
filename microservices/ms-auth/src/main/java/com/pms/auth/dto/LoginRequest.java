package com.pms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    @Email
    @Schema(example = "dr.smith@pms.ie")
    private String email;

    @NotBlank
    @Schema(example = "Doctor@1234!")
    private String password;
}
