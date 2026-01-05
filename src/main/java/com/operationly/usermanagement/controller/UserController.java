package com.operationly.usermanagement.controller;

import com.operationly.usermanagement.dto.BaseResponse;
import com.operationly.usermanagement.dto.ErrorDetails;
import com.operationly.usermanagement.dto.UserAccountDto;
import com.operationly.usermanagement.service.OrganizationService;
import com.operationly.usermanagement.service.WorkOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.UUID;

import static com.operationly.usermanagement.constants.UserConstants.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final WorkOSService workOSService;
    private final OrganizationService organizationService;

    /**
     * Sync API endpoint that validates the Bearer token, extracts userId from WorkOS,
     * and syncs the user with the local user_account table.
     * 
     * Flow:
     * 1. Validates the Bearer token and extracts WorkOS userId
     * 2. Fetches user details from WorkOS
     * 3. Checks if user exists in user_account table
     * 4. If exists, updates the user details (doesn't change organization_id if already set)
     * 5. If not exists, creates a new entry (organizationId is optional - can be null during signup)
     *
     * Expected header:
     *   Authorization: Bearer &lt;sessionToken&gt;
     *
     * Optional query parameter:
     *   organizationId: UUID of the organization (if not present in JWT token). Can be omitted during initial signup.
     *
     * (Optional) For backward compatibility you can still send it via cookie `wos_session`,
     * but the Bearer token takes precedence.
     */
    @GetMapping("/sync")
    public ResponseEntity<BaseResponse<UserAccountDto>> syncUser(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @CookieValue(value = "wos_session", required = false) String sessionTokenCookie,
            @RequestParam(value = "organizationId", required = false) String organizationIdParam) {

        BaseResponse<UserAccountDto> response = new BaseResponse<>();
        ErrorDetails errorDetails = new ErrorDetails();
        
        try {
            // Prefer Bearer token from Authorization header
            String sessionToken = null;
            if (authorizationHeader != null && authorizationHeader.startsWith(BEARER)) {
                sessionToken = authorizationHeader.substring(BEARER.length()).trim();
            } else if (sessionTokenCookie != null && !sessionTokenCookie.isEmpty()) {
                // Fallback to cookie if header is not present
                sessionToken = sessionTokenCookie;
            }
            
            if (sessionToken == null || sessionToken.isEmpty()) {
                response.setStatus(FAILURE);
                errorDetails.setError("Session token is required");
                errorDetails.setMessage("Please provide sessionToken as a Bearer token in the Authorization header");
                response.setErrors(Collections.singletonList(errorDetails));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Parse organizationId from parameter if provided
            UUID organizationId = null;
            if (organizationIdParam != null && !organizationIdParam.isEmpty()) {
                try {
                    organizationId = UUID.fromString(organizationIdParam);
                } catch (IllegalArgumentException e) {
                    response.setStatus(FAILURE);
                    errorDetails.setError("Invalid organizationId format");
                    errorDetails.setMessage("organizationId must be a valid UUID");
                    response.setErrors(Collections.singletonList(errorDetails));
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            }
            
            // Sync user with local database
            response.setStatus(SUCCESS);
            response.setResponse(workOSService.syncUserFromSessionToken(sessionToken, organizationId));

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error syncing user from session token: {}", e.getMessage(), e);
            response.setStatus(FAILURE);
            errorDetails.setError("Failed to sync user");
            errorDetails.setMessage(e.getMessage());
            response.setErrors(Collections.singletonList(errorDetails));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * Creates a organization and attaches it to a user account.
     * This endpoint automatically fetches organization details from WorkOS if organization_id is in the token,
     * otherwise uses the provided request body details.
     *
     * Expected header:
     *   Authorization: Bearer &lt;sessionToken&gt;
     *
     * Request body (optional if organization_id is in token):
     *   {
     *     "name": "Organization Name",  // Optional if fetching from WorkOS
     *     "plan": "FREE" | "PRO" | "ENTERPRISE",  // Optional, defaults to FREE
     *     "status": "ACTIVE" | "SUSPENDED"  // Optional, defaults to ACTIVE
     *   }
     */
    @PostMapping("/organization/create")
    public ResponseEntity<BaseResponse<?>> createorganizationAndAttachToUser(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @CookieValue(value = "wos_session", required = false) String sessionTokenCookie,
            @RequestParam(name = "organizationName") String organizationName) {

        BaseResponse<?> response = new BaseResponse<>();
        ErrorDetails errorDetails = new ErrorDetails();

        try {
            // Extract session token
            String sessionToken = null;
            if (authorizationHeader != null && authorizationHeader.startsWith(BEARER)) {
                sessionToken = authorizationHeader.substring(BEARER.length()).trim();
            } else if (sessionTokenCookie != null && !sessionTokenCookie.isEmpty()) {
                sessionToken = sessionTokenCookie;
            }
            
            if (sessionToken == null || sessionToken.isEmpty()) {
                response.setStatus(FAILURE);
                errorDetails.setError("Session token is required");
                errorDetails.setMessage("Please provide sessionToken as a Bearer token in the Authorization header");
                response.setErrors(Collections.singletonList(errorDetails));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Extract WorkOS user ID from token
            String workosUserId = workOSService.getUserIdFromSessionToken(sessionToken);
            
            // Create organization and attach to user (organization will be inserted into DB)
            organizationService.createOrganizationAndAttachToUser(workosUserId, organizationName);
            
            log.info("Successfully created and inserted organization {} into database for user {}", 
                    organizationName, workosUserId);

            response.setStatus(SUCCESS);
            return ResponseEntity.status(HttpStatus.OK).body(response);
            
        } catch (Exception e) {
            response.setStatus(FAILURE);
            errorDetails.setError("Failed to create organization");
            errorDetails.setMessage(e.getMessage());
            response.setErrors(Collections.singletonList(errorDetails));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Gets user details including organization information.
     * Returns all available user and organization details.
     *
     * Expected header:
     *   Authorization: Bearer &lt;sessionToken&gt;
     */
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<UserAccountDto>> getUserDetails(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @CookieValue(value = "wos_session", required = false) String sessionTokenCookie) {

        BaseResponse<UserAccountDto> response = new BaseResponse<>();
        ErrorDetails errorDetails = new ErrorDetails();

        try {
            // Extract session token
            String sessionToken = null;
            if (authorizationHeader != null && authorizationHeader.startsWith(BEARER)) {
                sessionToken = authorizationHeader.substring(BEARER.length()).trim();
            } else if (sessionTokenCookie != null && !sessionTokenCookie.isEmpty()) {
                sessionToken = sessionTokenCookie;
            }
            
            if (sessionToken == null || sessionToken.isEmpty()) {
                response.setStatus(FAILURE);
                errorDetails.setError("Session token is required");
                errorDetails.setMessage("Please provide sessionToken as a Bearer token in the Authorization header");
                response.setErrors(Collections.singletonList(errorDetails));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Extract WorkOS user ID from token
            String workosUserId = workOSService.getUserIdFromSessionToken(sessionToken);
            
            // Get user account
            var userAccount = workOSService.getUserInfo(workosUserId);
            response.setStatus(SUCCESS);
            response.setResponse(userAccount);
            
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
}

