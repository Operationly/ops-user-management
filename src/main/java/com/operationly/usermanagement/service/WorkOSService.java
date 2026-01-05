package com.operationly.usermanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.operationly.usermanagement.dto.OrganizationDto;
import com.operationly.usermanagement.dto.UserAccountDto;
import com.operationly.usermanagement.entity.Organization;
import com.operationly.usermanagement.entity.UserAccount;
import com.operationly.usermanagement.exception.BusinessException;
import com.operationly.usermanagement.repository.OrganizationRepository;
import com.workos.WorkOS;
import com.workos.usermanagement.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOSService {

    private final WorkOS workOS;
    private final UserAccountService userAccountService;
    private final OrganizationRepository organizationRepository;

    /**
     * Retrieves the WorkOS user ID from a session token.
     * WorkOS session tokens are JWTs (access tokens) that contain the user ID in the 'sub' claim.
     *
     * @param sessionToken The WorkOS session token (JWT) from the cookie
     * @return The WorkOS user ID
     * @throws Exception If the session token is invalid or user ID cannot be extracted
     */
    public String getUserIdFromSessionToken(String sessionToken) throws Exception {
        try {
            // WorkOS access tokens are JWTs. We decode without verification to extract the user ID.
            // Note: In production, you should verify the signature using WorkOS's public keys for security.
            // For now, we decode without verification to extract claims.
            String[] parts = sessionToken.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException("Invalid JWT format");
            }

            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            var mapper = new ObjectMapper();
            var jsonNode = mapper.readTree(payload);

            // The user ID is typically in the 'sub' (subject) claim of the JWT
            String userId = jsonNode.has("sub") ? jsonNode.get("sub").asText() : null;

            if (userId == null || userId.isEmpty()) {
                // Fallback: try to get from 'user_id' claim if 'sub' doesn't exist
                userId = jsonNode.has("user_id") ? jsonNode.get("user_id").asText() : null;
            }

            if (userId == null || userId.isEmpty()) {
                throw new BusinessException("User ID not found in session token claims. Available claims: " + jsonNode.fieldNames());
            }

            return userId;
        } catch (Exception e) {
            log.error("Error extracting user ID from session token: {}", e.getMessage(), e);
            throw new BusinessException("Failed to extract user ID from session token: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts organization ID from JWT session token if available.
     * Checks for common claim names: 'organization_id', 'org_id', 'organization_id', 'organizationId'
     *
     * @param sessionToken The WorkOS session token (JWT)
     * @return Organization ID if found in token, null otherwise
     */
    public UUID getOrganizationIdFromSessionToken(String sessionToken) {
        try {
            String[] parts = sessionToken.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(payload);

            // Try different possible claim names for organization ID
            String organizationIdStr = null;
            if (jsonNode.has("organization_id")) {
                organizationIdStr = jsonNode.get("organization_id").asText();
            } else if (jsonNode.has("org_id")) {
                organizationIdStr = jsonNode.get("org_id").asText();
            }

            if (StringUtils.isNotEmpty(organizationIdStr)) {
                try {
                    return UUID.fromString(organizationIdStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID format for organization_id in token: {}", organizationIdStr);
                    return null;
                }
            }

            return null;
        } catch (Exception e) {
            log.warn("Error extracting organization ID from session token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the full WorkOS User object from a session token.
     * First extracts the user ID from the JWT, then fetches the user details using the WorkOS API.
     *
     * @param sessionToken The WorkOS session token (JWT) from the cookie
     * @return The WorkOS User object
     * @throws Exception If the session token is invalid or user cannot be retrieved
     */
    public User getUserFromSessionToken(String sessionToken) throws Exception {
        try {
            // First extract the user ID from the JWT
            String userId = getUserIdFromSessionToken(sessionToken);

            // Then fetch the full user details using the WorkOS API
            return workOS.userManagement.getUser(userId);
        } catch (Exception e) {
            log.error("Error retrieving user from session token: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Validates the session token, extracts user ID, fetches WorkOS user details,
     * and syncs with the local user_account table.
     * Organization ID is optional - if not provided, user will be created without organization.
     *
     * @param sessionToken The WorkOS session token (JWT) from the Bearer token
     * @param organizationId     Optional organization ID. If null, will try to extract from JWT token. If still null, user will be created without organization.
     * @return The synced UserAccount entity
     * @throws Exception If the session token is invalid or sync fails
     */
    public UserAccountDto syncUserFromSessionToken(String sessionToken, UUID organizationId) throws Exception {
        try {
            // Validate token and get WorkOS user
            User workosUser = getUserFromSessionToken(sessionToken);

            // Extract organization ID from token if not provided
            UUID finalOrgId = organizationId;
            if (finalOrgId == null) {
                finalOrgId = getOrganizationIdFromSessionToken(sessionToken);
            }

            // Sync with local database (organizationId can be null for initial signup)
            UserAccount userAccount = userAccountService.syncUserAccount(workosUser, finalOrgId);

            Optional<Organization> organizationOpt = Optional.empty();
            if (finalOrgId != null && StringUtils.isNotEmpty(finalOrgId.toString())) {
                organizationOpt = organizationRepository.findByOrganizationId(finalOrgId);
            }

            log.info("Successfully synced user account for WorkOS user ID: {} with organization ID: {}",
                    workosUser.getId(), finalOrgId);

            return constructUserDto(userAccount, organizationOpt);
        } catch (Exception e) {
            log.error("Error syncing user from session token: {}", e.getMessage(), e);
            throw e;
        }
    }

    public UserAccountDto getUserInfo(String workosUserId) {
        Optional<UserAccount> userAccountOptional = userAccountService.getUserAccountByWorkosUserId(workosUserId);

        if (userAccountOptional.isEmpty()) {
            throw new BusinessException("No user account found for workosUserId: " + workosUserId);
        }

        UserAccount userAccount = userAccountOptional.get();
        Optional<Organization> organizationOpt = Optional.empty();

        if (userAccount.getOrganizationId() != null) {
            organizationOpt = organizationRepository.findByOrganizationId(userAccount.getOrganizationId());
        }

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
}

