package com.operationly.usermanagement.service.impl;

import com.operationly.usermanagement.service.UserAccountService;
import com.operationly.usermanagement.service.WorkOSService;
import com.operationly.usermanagement.dto.OrganizationDto;
import com.operationly.usermanagement.dto.UserAccountDto;
import com.operationly.usermanagement.dto.UserContextDto;
import com.operationly.usermanagement.entity.Organization;
import com.operationly.usermanagement.entity.UserAccount;
import com.operationly.usermanagement.exception.BusinessException;
import com.operationly.usermanagement.entity.UserOrganization;
import com.operationly.usermanagement.entity.Role;
import com.operationly.usermanagement.repository.OrganizationRepository;
import com.operationly.usermanagement.repository.UserAccountRepository;
import com.operationly.usermanagement.repository.UserOrganizationRepository;
import com.workos.usermanagement.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final WorkOSService workOSService;

    /**
     * Syncs a WorkOS user with the local user_account table.
     * If the user exists, updates their information (but doesn't change
     * organization_id if already set).
     * If not, creates a new entry. Organization ID is optional - can be null during
     * initial signup.
     *
     * @param workosUserId   The WorkOS User object
     * @param organizationId Optional organization ID. Can be null during initial
     *                       signup.
     * @return The synced UserAccount entity
     */
    @Transactional
    @Override
    public UserAccountDto syncUserAccount(String workosUserId, UUID organizationId) {
        User workosUser = workOSService.getWorkOsUserById(workosUserId);
        Optional<UserAccount> existingUserOpt = userAccountRepository.findByWorkosUserId(workosUserId);

        UserAccount userAccount;
        userAccount = existingUserOpt.map(account -> updateExistingUser(account, workosUser, organizationId))
                .orElseGet(() -> createNewUser(workosUserId, workosUser, organizationId));

        updateLastSignIn(userAccount, workosUser.getLastSignInAt());

        userAccount = userAccountRepository.save(userAccount);
        Optional<Organization> organizationOpt = resolveOrganization(userAccount);

        return constructUserDto(userAccount, organizationOpt);
    }

    private UserAccount updateExistingUser(UserAccount existingUser, User workosUser, UUID organizationId) {
        log.info("Updating existing user account for WorkOS user ID: {}", existingUser.getWorkosUserId());

        // Only update organization if user has no organizations and a new
        // organizationId is provided
        List<UserOrganization> existingOrgs = userOrganizationRepository.findByUser(existingUser);
        if (existingOrgs.isEmpty() && organizationId != null) {
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isPresent()) {
                UserOrganization userOrg = UserOrganization.builder()
                        .user(existingUser)
                        .organization(orgOpt.get())
                        .role(Role.MEMBER)
                        .build();
                userOrganizationRepository.save(userOrg);
                log.info("Attaching organization ID {} to existing user", organizationId);
            } else {
                log.warn("Organization ID {} not found, cannot attach to user", organizationId);
            }
        }

        existingUser.setEmail(workosUser.getEmail());
        existingUser.setFirstName(workosUser.getFirstName());
        existingUser.setLastName(workosUser.getLastName());
        existingUser.setEmailVerified(workosUser.getEmailVerified());
        existingUser.setProfilePictureUrl(workosUser.getProfilePictureUrl());

        return existingUser;
    }

    private UserAccount createNewUser(String workosUserId, User workosUser, UUID organizationId) {
        log.warn("User verified by WorkOS but not found in local DB: {}", workosUserId);
        log.info("Creating new user account for WorkOS user ID: {}", workosUserId);

        UserAccount newUser = UserAccount.builder()
                .workosUserId(workosUserId)
                .email(workosUser.getEmail())
                .firstName(workosUser.getFirstName())
                .lastName(workosUser.getLastName())
                .emailVerified(workosUser.getEmailVerified())
                .profilePictureUrl(workosUser.getProfilePictureUrl())
                .build();

        newUser = userAccountRepository.save(newUser);

        if (organizationId != null) {
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isPresent()) {
                UserOrganization userOrg = UserOrganization.builder()
                        .user(newUser)
                        .organization(orgOpt.get())
                        .role(Role.MEMBER)
                        .build();
                userOrganizationRepository.save(userOrg);
                log.info("Attached organization ID {} to new user", organizationId);
            } else {
                log.warn("Organization ID {} not found, cannot attach to new user", organizationId);
            }
        }

        return newUser;
    }

    private void updateLastSignIn(UserAccount userAccount, String lastSignInAtStr) {
        if (StringUtils.isNotEmpty(lastSignInAtStr)) {
            try {
                LocalDateTime lastSignIn = parseDateTime(lastSignInAtStr);
                userAccount.setLastSignInAt(lastSignIn);
            } catch (Exception e) {
                log.warn("Failed to parse lastSignInAt: {}", lastSignInAtStr, e);
            }
        }
    }

    private Optional<Organization> resolveOrganization(UserAccount userAccount) {
        List<UserOrganization> userOrgs = userOrganizationRepository.findByUser(userAccount);
        if (!userOrgs.isEmpty()) {
            return Optional.of(userOrgs.get(0).getOrganization());
        }
        return Optional.empty();
    }

    @Override
    public UserAccountDto getUserInfo(String workosUserId) {
        Optional<UserAccount> userAccountOptional = userAccountRepository.findByWorkosUserId(workosUserId);

        if (userAccountOptional.isEmpty()) {
            throw new BusinessException("No user account found for workosUserId: " + workosUserId);
        }

        UserAccount userAccount = userAccountOptional.get();
        Optional<Organization> organizationOpt = resolveOrganization(userAccount);

        return constructUserDto(userAccount, organizationOpt);
    }

    private UserAccountDto constructUserDto(UserAccount userAccount, Optional<Organization> organizationOpt) {
        String roleValue = null;
        if (organizationOpt.isPresent()) {
            Optional<UserOrganization> userOrgOpt = userOrganizationRepository.findByUserAndOrganizationOrganizationId(
                    userAccount,
                    organizationOpt.get().getOrganizationId());
            if (userOrgOpt.isPresent()) {
                roleValue = userOrgOpt.get().getRole().getValue();
            }
        }

        return UserAccountDto.builder()
                .id(userAccount.getId())
                .workosUserId(userAccount.getWorkosUserId())
                .organization(organizationOpt.map(this::constructOrganizationDto).orElse(null))
                .email(userAccount.getEmail())
                .firstName(userAccount.getFirstName())
                .lastName(userAccount.getLastName())
                .emailVerified(userAccount.getEmailVerified())
                .role(roleValue)
                .onboardingCompleted(userAccount.getOnboardingCompleted())
                .profilePictureUrl(userAccount.getProfilePictureUrl())
                .lastSignInAt(userAccount.getLastSignInAt() != null ? userAccount.getLastSignInAt().toString() : null)
                .createdAt(userAccount.getCreatedAt() != null ? userAccount.getCreatedAt().toString() : null)
                .updatedAt(userAccount.getUpdatedAt() != null ? userAccount.getUpdatedAt().toString() : null)
                .build();
    }

    private OrganizationDto constructOrganizationDto(Organization organization) {
        return OrganizationDto.builder()
                .organizationId(organization.getOrganizationId().toString())
                .name(organization.getName())
                .plan(organization.getPlan() != null ? organization.getPlan().name() : null)
                .status(organization.getStatus() != null ? organization.getStatus().name() : null)
                .createdAt(organization.getCreatedAt() != null ? organization.getCreatedAt().toString() : null)
                .updatedAt(organization.getUpdatedAt() != null ? organization.getUpdatedAt().toString() : null)
                .build();
    }

    /**
     * Gets user account by WorkOS user ID
     *
     * @param workosUserId The WorkOS user ID
     * @return Optional UserAccount
     */
    @Override
    public UserContextDto getUserAccountByWorkosUserId(String workosUserId) {
        Optional<UserAccount> userAccountOpt = userAccountRepository.findByWorkosUserId(workosUserId);
        UserContextDto context = null;
        if (userAccountOpt.isPresent()) {
            UserAccount userAccount = userAccountOpt.get();

            // Resolve primary organization (first one found)
            List<UserOrganization> userOrgs = userOrganizationRepository.findByUser(userAccount);
            UserOrganization primaryOrg = userOrgs.isEmpty() ? null : userOrgs.get(0);

            context = UserContextDto.builder()
                    .userId(String.valueOf(userAccount.getId()))
                    .workosUserId(userAccount.getWorkosUserId())
                    .email(userAccount.getEmail())
                    .role(primaryOrg != null ? primaryOrg.getRole().getValue() : null)
                    .organizationId(
                            primaryOrg != null ? primaryOrg.getOrganization().getOrganizationId().toString() : null)
                    .build();
        }
        return context;
    }

    @Override
    public UserAccountDto getUserById(Long userId) {
        Optional<UserAccount> userAccountOpt = userAccountRepository.findById(userId);

        if (userAccountOpt.isEmpty()) {
            throw new BusinessException("No user account found for userId: " + userId);
        }

        UserAccount userAccount = userAccountOpt.get();
        Optional<Organization> organizationOpt = resolveOrganization(userAccount);

        return constructUserDto(userAccount, organizationOpt);
    }

    @Override
    public List<UserAccountDto> getUsersByOrgId(String orgId) {
        Optional<Organization> organizationOpt = organizationRepository.findByOrganizationId(UUID.fromString(orgId));
        if (organizationOpt.isEmpty()) {
            throw new BusinessException("No organization found for orgId: " + orgId);
        }

        List<UserOrganization> userOrgs = userOrganizationRepository.findByOrganization(organizationOpt.get());
        return userOrgs.stream()
                .map(UserOrganization::getUser)
                .map(userAccount -> constructUserDto(userAccount, organizationOpt))
                .toList();
    }

    /**
     * Parses ISO 8601 datetime string to LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return null;
        }

        try {
            // Try ISO 8601 format first
            return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                // Try ISO 8601 with offset
                return LocalDateTime.parse(dateTimeString.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2"),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX"));
            } catch (DateTimeParseException e2) {
                log.warn("Could not parse datetime string: {}", dateTimeString);
                return null;
            }
        }
    }
}
