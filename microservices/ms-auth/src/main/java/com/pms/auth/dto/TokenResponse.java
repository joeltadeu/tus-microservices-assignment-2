package com.pms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponse {

    @Schema(description = "Short-lived JWT access token (15 min by default)")
    private String accessToken;

    @Schema(description = "Long-lived opaque refresh token (7 days). Rotated on every use.")
    private String refreshToken;

    @Schema(example = "Bearer")
    private String tokenType;

    @Schema(description = "Access token lifetime in seconds", example = "900")
    private long expiresIn;
}
