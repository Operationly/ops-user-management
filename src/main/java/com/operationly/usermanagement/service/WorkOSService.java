package com.operationly.usermanagement.service;

import com.workos.usermanagement.models.User;

public interface WorkOSService {
    User getWorkOsUserById(String workosUserId);
}
