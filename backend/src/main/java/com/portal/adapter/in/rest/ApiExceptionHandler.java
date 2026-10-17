package com.portal.adapter.in.rest;

import com.portal.adapter.out.google.GoogleDriveAdapter.ImageNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> fileTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", "La imagen supera el límite de 10 MB."));
    }

    @ExceptionHandler(ImageNotFoundException.class)
    ResponseEntity<Map<String, String>> imageNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<Map<String, String>> driveUnavailable() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Google Drive no pudo completar la solicitud."));
    }
}
