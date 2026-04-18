package com.nazir.ecommerce.user.dto;

import com.nazir.ecommerce.user.model.Address;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AddressResponse {

    private final UUID id;
    private final String label;
    private final String fullName;
    private final String phoneNumber;
    private final String line1;
    private final String line2;
    private final String city;
    private final String state;
    private final String postalCode;
    private final String country;
    private final boolean isDefault;
    private final Instant createdAt;

    public static AddressResponse from(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .line1(address.getLine1())
                .line2(address.getLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
