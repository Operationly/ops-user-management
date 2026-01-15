package com.operationly.usermanagement.controller;

import com.operationly.usermanagement.dto.BaseResponse;
import com.operationly.usermanagement.dto.ErrorDetails;
import com.operationly.usermanagement.dto.UserAccountDto;
import com.operationly.usermanagement.dto.UserContextDto;
import com.operationly.usermanagement.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

import static com.operationly.usermanagement.constants.UserConstants.FAILURE;
import static com.operationly.usermanagement.constants.UserConstants.SUCCESS;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserAccountService userAccountService;

    /**
     * Sync API endpoint.
     * Handles signup and login flows by syncing WorkOS user data with local database.
     */
    @GetMapping("/sync")
    public ResponseEntity<BaseResponse<UserAccountDto>> syncUser(
            @RequestHeader(value = "x-workos-user-id") String workosUserId) {

        BaseResponse<UserAccountDto> response = new BaseResponse<>();
        ErrorDetails errorDetails = new ErrorDetails();

        try {
            if (workosUserId == null || workosUserId.isEmpty()) {
                log.error("x-workos-user-id header is missing or empty");
                response.setStatus(FAILURE);
                errorDetails.setError("Missing required header");
                errorDetails.setMessage("x-workos-user-id header is required");
                response.setErrors(Collections.singletonList(errorDetails));
                return ResponseEntity.badRequest().body(response);
            }

            response.setStatus(SUCCESS);
            response.setResponse(userAccountService.syncUserAccount(workosUserId, null));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing user: {}", e.getMessage(), e);
            response.setStatus(FAILURE);
            errorDetails.setError("Failed to sync user");
            errorDetails.setMessage(e.getMessage());
            response.setErrors(Collections.singletonList(errorDetails));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Gets user details including organization information.
     */
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<UserAccountDto>> getUserDetails(
            @RequestHeader(value = "x-workos-user-id") String workosUserId) {

        BaseResponse<UserAccountDto> response = new BaseResponse<>();
        ErrorDetails errorDetails = new ErrorDetails();

        try {
            if (workosUserId == null || workosUserId.isEmpty()) {
                log.error("x-workos-user-id header is missing or empty");
                response.setStatus(FAILURE);
                errorDetails.setError("Missing required header");
                errorDetails.setMessage("x-workos-user-id header is required");
                response.setErrors(Collections.singletonList(errorDetails));
                return ResponseEntity.badRequest().body(response);
            }

            var userAccountDto = userAccountService.getUserInfo(workosUserId);

            response.setStatus(SUCCESS);
            response.setResponse(userAccountDto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting user details: {}", e.getMessage(), e);
            response.setStatus(FAILURE);
            errorDetails.setError("Failed to retrieve user details");
            errorDetails.setMessage(e.getMessage());
            response.setErrors(Collections.singletonList(errorDetails));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/context")
    public ResponseEntity<UserContextDto> getUserContext(@RequestParam("workosUserId") String workosUserId) {
        log.debug("Fetching user context for WorkOS ID: {}", workosUserId);
        UserContextDto userContextDto = userAccountService.getUserAccountByWorkosUserId(workosUserId);
        if (userContextDto == null) {
            log.warn("No user context found for WorkOS ID: {}", workosUserId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userContextDto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleException(Exception e) {
        BaseResponse<?> response = new BaseResponse<>();
        ErrorDetails errorDetails = new ErrorDetails();
        log.error("Unhandled exception: {}", e.getMessage(), e);
        response.setStatus(FAILURE);
        errorDetails.setError("Internal server error");
        errorDetails.setMessage(e.getMessage());
        response.setErrors(Collections.singletonList(errorDetails));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
