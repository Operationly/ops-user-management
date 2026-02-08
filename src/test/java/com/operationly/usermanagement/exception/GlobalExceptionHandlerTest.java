package com.operationly.usermanagement.exception;

import com.operationly.usermanagement.dto.BaseResponse;
import com.operationly.usermanagement.dto.ErrorDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;

import static com.operationly.usermanagement.constants.UserConstants.FAILURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException_ShouldReturnBadRequest_WithCorrectErrorDetails() {
        // Arrange
        String param = "paramName";
        String errorCode = "errorCode";
        String errorMessage = "Something went wrong";
        BusinessException ex = new BusinessException(errorCode, errorMessage);

        // Act
        ResponseEntity<BaseResponse<Void>> responseEntity = exceptionHandler.handleBusinessException(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        BaseResponse<?> body = responseEntity.getBody();
        assertEquals(FAILURE, body.getStatus());
        assertEquals(1, body.getErrors().size());
        ErrorDetails errorDetails = body.getErrors().get(0);
        assertEquals(errorCode, errorDetails.getError());
        assertEquals(errorMessage, errorDetails.getMessage());
    }

    @Test
    void handleMissingRequestHeaderException_ShouldReturnBadRequest_WithReadableMessage() {
        // Arrange
        String headerName = "x-workos-user-id";
        MissingRequestHeaderException ex = mock(MissingRequestHeaderException.class);
        // Stubbing the methods we use
        org.mockito.Mockito.when(ex.getMessage())
                .thenReturn("Missing request header 'x-workos-user-id' for method parameter of type String");
        org.mockito.Mockito.when(ex.getHeaderName()).thenReturn(headerName);

        // Act
        ResponseEntity<BaseResponse<Void>> responseEntity = exceptionHandler.handleMissingRequestHeaderException(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        BaseResponse<?> body = responseEntity.getBody();
        assertEquals(FAILURE, body.getStatus());
        assertEquals(1, body.getErrors().size());
        ErrorDetails errorDetails = body.getErrors().get(0);
        assertEquals("Missing required header", errorDetails.getError());
        assertEquals(headerName + " header is required", errorDetails.getMessage());
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Arrange
        Exception ex = new RuntimeException("Unexpected error");

        // Act
        ResponseEntity<BaseResponse<Void>> responseEntity = exceptionHandler.handleGenericException(ex);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        BaseResponse<?> body = responseEntity.getBody();
        assertEquals(FAILURE, body.getStatus());
        assertEquals(1, body.getErrors().size());
        ErrorDetails errorDetails = body.getErrors().get(0);
        assertEquals("Internal server error", errorDetails.getError());
        assertEquals("Unexpected error", errorDetails.getMessage());
    }
}
