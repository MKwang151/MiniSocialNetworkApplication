package com.social.backend.exception;

import com.social.backend.model.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ApiResponse<Void> handleNotFound(ResourceNotFoundException e) {
        return ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleGeneral(Exception e) {
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }
}
