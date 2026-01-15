package com.operationly.usermanagement.service.impl;

import com.operationly.usermanagement.service.UserAccountService;
import com.operationly.usermanagement.service.WorkOSService;
import com.operationly.usermanagement.dto.OrganizationDto;
import com.operationly.usermanagement.dto.UserAccountDto;
import com.operationly.usermanagement.dto.UserContextDto;
import com.operationly.usermanagement.entity.Organization;
import com.operationly.usermanagement.entity.UserAccount;
import com.operationly.usermanagement.exception.BusinessException;
import com.operationly.usermanagement.repository.OrganizationRepository;
import com.operationly.usermanagement.repository.UserAccountRepository;
import com.workos.usermanagement.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
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

        // Only update organization_id if it's currently null and a new organizationId
        // is provided
        if (existingUser.getOrganizationId() == null && organizationId != null) {
            existingUser.setOrganizationId(organizationId);
            log.info("Attaching organization ID {} to existing user", organizationId);
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
        log.info("Creating new user account for WorkOS user ID: {} with organization ID: {}",
                workosUserId, organizationId);

        return UserAccount.builder()
                .workosUserId(workosUserId)
                .organizationId(organizationId)
                .email(workosUser.getEmail())
                .firstName(workosUser.getFirstName())
                .lastName(workosUser.getLastName())
                .emailVerified(workosUser.getEmailVerified())
                .profilePictureUrl(workosUser.getProfilePictureUrl())
                .role(UserAccount.Role.USER)
                .build();
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
        if (userAccount.getOrganizationId() != null) {
            return organizationRepository.findByOrganizationId(userAccount.getOrganizationId());
        }
        return Optional.empty();
    }

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
        return UserAccountDto.builder()
                .id(userAccount.getId())
                .workosUserId(userAccount.getWorkosUserId())
                .organization(organizationOpt.map(this::constructOrganizationDto).orElse(null))
                .email(userAccount.getEmail())
                .firstName(userAccount.getFirstName())
                .lastName(userAccount.getLastName())
                .emailVerified(userAccount.getEmailVerified())
                .role(userAccount.getRole() != null ? userAccount.getRole().getValue() : null)
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
    public UserContextDto getUserAccountByWorkosUserId(String workosUserId) {
        Optional<UserAccount> userAccountOpt = userAccountRepository.findByWorkosUserId(workosUserId);
        UserContextDto context = null;
        if (userAccountOpt.isPresent()) {
            UserAccount userAccount = userAccountOpt.get();
            context = UserContextDto.builder()
                    .userId(String.valueOf(userAccount.getId()))
                    .workosUserId(userAccount.getWorkosUserId())
                    .email(userAccount.getEmail())
                    .role(userAccount.getRole() != null ? userAccount.getRole().getValue() : null)
                    .organizationId(
                            userAccount.getOrganizationId() != null ? userAccount.getOrganizationId().toString() : null)
                    .build();
        }
        return context;
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
