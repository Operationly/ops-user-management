package com.operationly.usermanagement.repository;

import com.operationly.usermanagement.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    
    /**
     * Find tenant by tenant ID
     */
    Optional<Tenant> findByTenantId(UUID tenantId);
    
    /**
     * Check if tenant exists by tenant ID
     */
    boolean existsByTenantId(UUID tenantId);
}

