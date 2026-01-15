package com.operationly.usermanagement.controller;

import com.operationly.usermanagement.dto.BaseResponse;
import com.operationly.usermanagement.dto.ErrorDetails;
import com.operationly.usermanagement.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

import static com.operationly.usermanagement.constants.UserConstants.FAILURE;
import static com.operationly.usermanagement.constants.UserConstants.SUCCESS;

@RestController
@RequestMapping("/api/v1/organization")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * Creates a organization and attaches it to a user account.
     */
    @PostMapping("/create")
    public ResponseEntity<BaseResponse<?>> createOrganizationAndAttachToUser(
            @RequestParam(name = "organizationName") String organizationName,
            @RequestHeader(value = "x-workos-user-id", required = false) String workosUserId) {

        BaseResponse<?> response = new BaseResponse<>();
        ErrorDetails errorDetails = new ErrorDetails();

        try {
            // Create organization and attach to user
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
}
