package com.operationly.usermanagement.service.impl;

import com.operationly.usermanagement.dto.OrganizationDto;
import com.operationly.usermanagement.entity.*;
import com.operationly.usermanagement.exception.BusinessException;
import com.operationly.usermanagement.repository.OrganizationRepository;
import com.operationly.usermanagement.repository.UserAccountRepository;
import com.operationly.usermanagement.repository.UserOrganizationRepository;
import com.operationly.usermanagement.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserOrganizationRepository userOrganizationRepository;

    /**
     * Creates a organization and attaches it to a user account.
     * If the user already has a organization, this operation will fail.
     *
     * @param workosUserId     The WorkOS user ID
     * @param organizationName The name of the organization
     * @throws RuntimeException if user not found or user already has a organization
     */
    @Transactional
    @Override
    public void createOrganizationAndAttachToUser(String workosUserId, String organizationName) {

        // Find user account
        Optional<UserAccount> userOpt = userAccountRepository.findByWorkosUserId(workosUserId);
        if (userOpt.isEmpty()) {
            throw new BusinessException("User account not found for WorkOS user ID: " + workosUserId);
        }

        UserAccount userAccount = userOpt.get();

        // Check if user already has an organization
        if (!userOrganizationRepository.findByUser(userAccount).isEmpty()) {
            throw new BusinessException(
                    "User already has a organization attached. User ID: " + userAccount.getId());
        }

        // Create organization
        Organization organization = Organization.builder()
                .name(organizationName)
                .plan(Plan.FREE)
                .status(Status.ACTIVE)
                .build();

        organization = organizationRepository.save(organization);
        log.info("Created organization with ID: {} for user: {}", organization.getOrganizationId(), workosUserId);

        // Attach organization to user
        UserOrganization userOrganization = UserOrganization.builder()
                .user(userAccount)
                .organization(organization)
                .role(Role.ADMIN)
                .build();

        userOrganizationRepository.save(userOrganization);

        userAccount.setOnboardingCompleted(true);
        userAccountRepository.save(userAccount);
        log.info("Attached organization {} to user account {}", organization.getOrganizationId(), workosUserId);

    }

    /**
     * Gets organization by organization ID
     *
     * @param organizationId The organization ID
     * @return Optional Organization
     */
    @Override
    public OrganizationDto getOrganizationById(String organizationId) {
        Optional<Organization> organizationOpt = organizationRepository.findByOrganizationId(UUID.fromString(organizationId));
        if (organizationOpt.isEmpty()) {
            throw new BusinessException("Organization not found for ID: " + organizationId);
        }
        return OrganizationDto.fromEntity(organizationOpt.get());
    }

    /**
     * Gets all organizations
     *
     * @return List of OrganizationDto
     */
    @Override
    public List<OrganizationDto> getAllOrganizations() {
        List<Organization> organizations = organizationRepository.findAll();
        return organizations.stream()
                .map(OrganizationDto::fromEntity)
                .toList();
    }
}
