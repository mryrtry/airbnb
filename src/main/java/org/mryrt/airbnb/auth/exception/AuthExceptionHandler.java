package org.mryrt.airbnb.auth.exception;

import org.mryrt.airbnb.auth.controller.AuthController;
import org.mryrt.airbnb.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;

@ControllerAdvice(basePackageClasses = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(AuthCredNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(AuthCredNotValidException ex) {
        List<ErrorResponse.ErrorDetail> errorDetailList = ex.getDetails().entrySet().stream()
                .map(entry -> ErrorResponse.ErrorDetail.builder()
                        .field(entry.getKey())
                        .message(entry.getValue())
                        .rejectedValue(null)
                        .errorType("VALIDATION_ERROR")
                        .build())
                .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message(ex.getMessage())
                .details(errorDetailList)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

}
