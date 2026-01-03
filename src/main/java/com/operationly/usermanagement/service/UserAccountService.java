package com.operationly.usermanagement.service;

import com.operationly.usermanagement.entity.UserAccount;
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
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    /**
     * Syncs a WorkOS user with the local user_account table.
     * If the user exists, updates their information (but doesn't change tenant_id if already set).
     * If not, creates a new entry. Tenant ID is optional - can be null during initial signup.
     *
     * @param workosUser The WorkOS User object
     * @param tenantId   Optional tenant ID. Can be null during initial signup.
     * @return The synced UserAccount entity
     */
    @Transactional
    public UserAccount syncUserAccount(User workosUser, UUID tenantId) {
        String workosUserId = workosUser.getId();

        // Check if user already exists
        Optional<UserAccount> existingUserOpt = userAccountRepository.findByWorkosUserId(workosUserId);

        boolean isLastSignInPresent = StringUtils.isNotEmpty(workosUser.getLastSignInAt());
        if (existingUserOpt.isPresent()) {
            // Update existing user - don't modify tenant_id if it's already set
            UserAccount existingUser = existingUserOpt.get();
            log.info("Updating existing user account for WorkOS user ID: {}", workosUserId);

            // Only update tenant_id if it's currently null and a new tenantId is provided
            if (existingUser.getTenantId() == null && tenantId != null) {
                existingUser.setTenantId(tenantId);
                log.info("Attaching tenant ID {} to existing user", tenantId);
            }

            existingUser.setEmail(workosUser.getEmail());
            existingUser.setFirstName(workosUser.getFirstName());
            existingUser.setLastName(workosUser.getLastName());
            existingUser.setEmailVerified(workosUser.getEmailVerified());
            existingUser.setProfilePictureUrl(workosUser.getProfilePictureUrl());

            // Update last sign in time if available
            if (isLastSignInPresent) {
                try {
                    LocalDateTime lastSignIn = parseDateTime(workosUser.getLastSignInAt());
                    existingUser.setLastSignInAt(lastSignIn);
                } catch (Exception e) {
                    log.warn("Failed to parse lastSignInAt: {}", workosUser.getLastSignInAt(), e);
                }
            }

            return userAccountRepository.save(existingUser);
        } else {
            // Create new user (tenantId can be null)
            log.info("Creating new user account for WorkOS user ID: {} with tenant ID: {}",
                    workosUserId, tenantId);

            UserAccount newUser = UserAccount.builder()
                    .workosUserId(workosUserId)
                    .tenantId(tenantId)
                    .email(workosUser.getEmail())
                    .firstName(workosUser.getFirstName())
                    .lastName(workosUser.getLastName())
                    .emailVerified(workosUser.getEmailVerified())
                    .profilePictureUrl(workosUser.getProfilePictureUrl())
                    .build();

            // Set last sign in time if available
            if (isLastSignInPresent) {
                try {
                    LocalDateTime lastSignIn = parseDateTime(workosUser.getLastSignInAt());
                    newUser.setLastSignInAt(lastSignIn);
                } catch (Exception e) {
                    log.warn("Failed to parse lastSignInAt: {}", workosUser.getLastSignInAt(), e);
                }
            }

            return userAccountRepository.save(newUser);
        }
    }

    /**
     * Gets user account by WorkOS user ID
     *
     * @param workosUserId The WorkOS user ID
     * @return Optional UserAccount
     */
    public Optional<UserAccount> getUserAccountByWorkosUserId(String workosUserId) {
        return userAccountRepository.findByWorkosUserId(workosUserId);
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

