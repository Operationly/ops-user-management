package com.operationly.usermanagement.service;

import com.operationly.usermanagement.entity.Organization;
import com.operationly.usermanagement.entity.UserAccount;
import com.operationly.usermanagement.exception.BusinessException;
import com.operationly.usermanagement.repository.OrganizationRepository;
import com.operationly.usermanagement.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserAccountRepository userAccountRepository;

    /**
     * Creates a organization and attaches it to a user account.
     * If the user already has a organization, this operation will fail.
     *
     * @param workosUserId The WorkOS user ID
     * @param organizationName   The name of the organization
     * @throws RuntimeException if user not found or user already has a organization
     */
    @Transactional
    public void createOrganizationAndAttachToUser(String workosUserId, String organizationName) {

        // Find user account
        Optional<UserAccount> userOpt = userAccountRepository.findByWorkosUserId(workosUserId);
        if (userOpt.isEmpty()) {
            throw new BusinessException("User account not found for WorkOS user ID: " + workosUserId);
        }

        UserAccount userAccount = userOpt.get();
        
        // Check if user already has an organization
        if (userAccount.getOrganizationId() != null) {
            throw new BusinessException("User already has a organization attached. Organization ID: " + userAccount.getOrganizationId());
        }

        // Create organization
        Organization organization = Organization.builder()
                .name(organizationName)
                .plan(Organization.Plan.FREE)
                .status(Organization.Status.ACTIVE)
                .build();

        organization = organizationRepository.save(organization);
        log.info("Created organization with ID: {} for user: {}", organization.getOrganizationId(), workosUserId);

        // Attach organization to user
        userAccount.setOrganizationId(organization.getOrganizationId());
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
    public Optional<Organization> getOrganizationById(UUID organizationId) {
        return organizationRepository.findByOrganizationId(organizationId);
    }
}

