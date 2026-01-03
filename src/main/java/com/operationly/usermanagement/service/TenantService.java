package com.operationly.usermanagement.service;

import com.operationly.usermanagement.entity.Tenant;
import com.operationly.usermanagement.entity.UserAccount;
import com.operationly.usermanagement.exception.BusinessException;
import com.operationly.usermanagement.repository.TenantRepository;
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
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;

    /**
     * Creates a tenant and attaches it to a user account.
     * If the user already has a tenant, this operation will fail.
     *
     * @param workosUserId The WorkOS user ID
     * @param tenantName   The name of the tenant
     * @throws RuntimeException if user not found or user already has a tenant
     */
    @Transactional
    public void createTenantAndAttachToUser(String workosUserId, String tenantName) {

        // Find user account
        Optional<UserAccount> userOpt = userAccountRepository.findByWorkosUserId(workosUserId);
        if (userOpt.isEmpty()) {
            throw new BusinessException("User account not found for WorkOS user ID: " + workosUserId);
        }

        UserAccount userAccount = userOpt.get();
        
        // Check if user already has a tenant
        if (userAccount.getTenantId() != null) {
            throw new BusinessException("User already has a tenant attached. Tenant ID: " + userAccount.getTenantId());
        }

        // Create tenant
        Tenant tenant = Tenant.builder()
                .name(tenantName)
                .plan(Tenant.Plan.FREE)
                .status(Tenant.Status.ACTIVE)
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Created tenant with ID: {} for user: {}", tenant.getTenantId(), workosUserId);

        // Attach tenant to user
        userAccount.setTenantId(tenant.getTenantId());
        userAccount.setOnboardingCompleted(true);
        userAccountRepository.save(userAccount);
        log.info("Attached tenant {} to user account {}", tenant.getTenantId(), workosUserId);

    }

    /**
     * Gets tenant by tenant ID
     *
     * @param tenantId The tenant ID
     * @return Optional Tenant
     */
    public Optional<Tenant> getTenantById(UUID tenantId) {
        return tenantRepository.findByTenantId(tenantId);
    }
}

