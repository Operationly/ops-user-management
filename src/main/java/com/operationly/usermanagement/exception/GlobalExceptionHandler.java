package com.operationly.usermanagement.exception;

import com.operationly.usermanagement.dto.BaseResponse;
import com.operationly.usermanagement.dto.ErrorDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;

import static com.operationly.usermanagement.constants.UserConstants.FAILURE;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException ex) {
        log.error("BusinessException: {}", ex.getErrorMessage());
        BaseResponse<Void> response = new BaseResponse<>();
        ErrorDetails errorDetails = new ErrorDetails();

        response.setStatus(FAILURE);
        errorDetails.setError(ex.getErrorCode());
        errorDetails.setMessage(ex.getErrorMessage());
        response.setErrors(Collections.singletonList(errorDetails));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        log.error("MissingRequestHeaderException: {}", ex.getMessage());
        BaseResponse<Void> response = new BaseResponse<>();
        ErrorDetails errorDetails = new ErrorDetails();

        response.setStatus(FAILURE);
        errorDetails.setError("Missing required header");
        errorDetails.setMessage(ex.getHeaderName() + " header is required");
        response.setErrors(Collections.singletonList(errorDetails));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        BaseResponse<Void> response = new BaseResponse<>();
        ErrorDetails errorDetails = new ErrorDetails();

        response.setStatus(FAILURE);
        errorDetails.setError("Internal server error");
        errorDetails.setMessage(ex.getMessage());
        response.setErrors(Collections.singletonList(errorDetails));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
