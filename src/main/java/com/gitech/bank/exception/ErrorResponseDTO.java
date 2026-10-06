package com.gitech.bank.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        Map<String, String> validationErrors
) {
    public static ErrorResponseDTO of(int status, String error, String message, String path) {
        return new ErrorResponseDTO(status, error, message, path, LocalDateTime.now(), null);
    }

    public static ErrorResponseDTO ofValidation(int status, String error, String message, String path, Map<String, String> validationErrors) {
        return new ErrorResponseDTO(status, error, message, path, LocalDateTime.now(), validationErrors);
    }
}
