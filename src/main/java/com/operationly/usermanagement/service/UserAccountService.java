package com.operationly.usermanagement.service;

import com.operationly.usermanagement.dto.UserAccountDto;
import com.operationly.usermanagement.dto.UserContextDto;

import java.util.List;
import java.util.UUID;

public interface UserAccountService {
    UserAccountDto syncUserAccount(String workosUserId, UUID organizationId);
    UserAccountDto getUserInfo(String workosUserId);
    UserContextDto getUserAccountByWorkosUserId(String workosUserId);
    UserAccountDto getUserById(Long userId);
    List<UserAccountDto> getUsersByOrgId(String orgId);
}
