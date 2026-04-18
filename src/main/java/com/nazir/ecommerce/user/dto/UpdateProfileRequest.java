package com.nazir.ecommerce.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
    private String fullName;

    @Pattern(
        regexp = "^(https?://.*|)$",
        message = "Avatar URL must be a valid HTTP/HTTPS URL"
    )
    private String avatarUrl;

    @Pattern(
        regexp = "^(\\+?[1-9]\\d{6,14}|)$",
        message = "Phone number must be a valid international format (e.g. +919876543210)"
    )
    private String phoneNumber;
}
