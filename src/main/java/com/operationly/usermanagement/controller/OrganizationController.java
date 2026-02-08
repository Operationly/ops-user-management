package com.operationly.usermanagement.controller;

import com.operationly.usermanagement.dto.BaseResponse;
import com.operationly.usermanagement.dto.OrganizationDto;
import com.operationly.usermanagement.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.operationly.usermanagement.constants.UserConstants.SUCCESS;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * Creates an organization and attaches it to a user account.
     */
    @PostMapping
    public ResponseEntity<BaseResponse<?>> createOrganizationAndAttachToUser(
            @RequestParam(name = "organizationName") String organizationName,
            @RequestHeader(value = "x-workos-user-id", required = false) String workosUserId) {

        organizationService.createOrganizationAndAttachToUser(workosUserId, organizationName);

        log.info("Successfully created and inserted organization {} into database for user {}",
                organizationName, workosUserId);

        BaseResponse<?> response = new BaseResponse<>();
        response.setStatus(SUCCESS);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Gets an organization by ID.
     */
    @GetMapping("/{orgId}")
    public ResponseEntity<BaseResponse<OrganizationDto>> getOrganizationById(@PathVariable String orgId) {
        BaseResponse<OrganizationDto> response = new BaseResponse<>();
        response.setStatus(SUCCESS);
        response.setResponse(organizationService.getOrganizationById(orgId));
        return ResponseEntity.ok(response);
    }

    /**
     * Gets all organizations.
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<OrganizationDto>>> getAllOrganizations() {
        BaseResponse<List<OrganizationDto>> response = new BaseResponse<>();
        response.setStatus(SUCCESS);
        response.setResponse(organizationService.getAllOrganizations());
        return ResponseEntity.ok(response);
    }
}
