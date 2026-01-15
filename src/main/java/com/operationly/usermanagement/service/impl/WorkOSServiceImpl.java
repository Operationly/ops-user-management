package com.operationly.usermanagement.service.impl;

import com.operationly.usermanagement.service.WorkOSService;
import com.workos.WorkOS;
import com.workos.usermanagement.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOSServiceImpl implements WorkOSService {

    private final WorkOS workOS;

    public User getWorkOsUserById(String workosUserId) {
        return workOS.userManagement.getUser(workosUserId);
    }
}
