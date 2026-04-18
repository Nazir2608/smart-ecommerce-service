package com.nazir.ecommerce.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success;
    private final String code;
    private final String message;
    private final Map<String, String> fieldErrors;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }

    public static ErrorResponse validation(Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
                .success(false)
                .code("VALIDATION_ERROR")
                .message("Request validation failed")
                .fieldErrors(fieldErrors)
                .build();
    }
}
