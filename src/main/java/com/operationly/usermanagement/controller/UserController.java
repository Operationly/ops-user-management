package com.operationly.usermanagement.controller;

import com.operationly.usermanagement.dto.BaseResponse;
import com.operationly.usermanagement.dto.UserAccountDto;
import com.operationly.usermanagement.dto.UserContextDto;
import com.operationly.usermanagement.exception.BusinessException;
import com.operationly.usermanagement.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.operationly.usermanagement.constants.UserConstants.SUCCESS;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserAccountService userAccountService;

    /**
     * Sync API endpoint.
     * Handles signup and login flows by syncing WorkOS user data with local
     * database.
     */
    @GetMapping("/sync")
    public ResponseEntity<BaseResponse<UserAccountDto>> syncUser(
            @RequestHeader(value = "x-workos-user-id") String workosUserId) {

        if (workosUserId == null || workosUserId.isEmpty()) {
            throw new BusinessException("Missing required header", "x-workos-user-id header is required");
        }

        BaseResponse<UserAccountDto> response = new BaseResponse<>();
        response.setStatus(SUCCESS);
        response.setResponse(userAccountService.syncUserAccount(workosUserId, null));

        return ResponseEntity.ok(response);
    }

    /**
     * Gets user details including organization information.
     */
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<UserAccountDto>> getUserDetails(
            @RequestHeader(value = "x-workos-user-id") String workosUserId) {

        if (workosUserId == null || workosUserId.isEmpty()) {
            throw new BusinessException("Missing required header", "x-workos-user-id header is required");
        }

        BaseResponse<UserAccountDto> response = new BaseResponse<>();
        var userAccountDto = userAccountService.getUserInfo(workosUserId);

        response.setStatus(SUCCESS);
        response.setResponse(userAccountDto);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponse<UserAccountDto>> getUserById(@PathVariable Long userId) {
        BaseResponse<UserAccountDto> response = new BaseResponse<>();
        var userAccountDto = userAccountService.getUserById(userId);

        response.setStatus(SUCCESS);
        response.setResponse(userAccountDto);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/org/{orgId}")
    public ResponseEntity<BaseResponse<List<UserAccountDto>>> getUsersByOrgId(@PathVariable String orgId) {
        BaseResponse<List<UserAccountDto>> response = new BaseResponse<>();
        var userAccounts = userAccountService.getUsersByOrgId(orgId);

        response.setStatus(SUCCESS);
        response.setResponse(userAccounts);

        return ResponseEntity.ok(response);
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
}
