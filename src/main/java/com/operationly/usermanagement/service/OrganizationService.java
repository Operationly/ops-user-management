package com.operationly.usermanagement.service;

import com.operationly.usermanagement.dto.OrganizationDto;

import java.util.List;

public interface OrganizationService {
    void createOrganizationAndAttachToUser(String workosUserId, String organizationName);
    OrganizationDto getOrganizationById(String organizationId);
    List<OrganizationDto> getAllOrganizations();
}
