package com.nazir.ecommerce.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {

    private final String accessToken;
    private final String refreshToken;

    @Builder.Default
    private final String tokenType = "Bearer";

    /**
     * Access token expiry in seconds (default 900 = 15 min).
     */
    private final long expiresIn;
}
