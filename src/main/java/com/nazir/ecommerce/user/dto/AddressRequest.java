package com.nazir.ecommerce.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddressRequest {

    @Size(max = 20, message = "Label must be at most 20 characters")
    private String label = "HOME";

    @NotBlank(message = "Full name is required")
    @Size(max = 255)
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^\\+?[1-9]\\d{6,14}$",
        message = "Phone number must be a valid international format"
    )
    private String phoneNumber;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 500)
    private String line1;

    @Size(max = 500)
    private String line2;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20)
    private String postalCode;

    @Size(max = 100)
    private String country = "IN";

    private boolean isDefault = false;
}
