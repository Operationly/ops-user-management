package com.operationly.usermanagement.service;

import com.operationly.usermanagement.entity.Organization;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationService {
    void createOrganizationAndAttachToUser(String workosUserId, String organizationName);
    Optional<Organization> getOrganizationById(UUID organizationId);
}
