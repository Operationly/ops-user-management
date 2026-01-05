package com.operationly.usermanagement.repository;

import com.operationly.usermanagement.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    
    /**
     * Find organization by organization ID
     */
    Optional<Organization> findByOrganizationId(UUID organizationId);
    
    /**
     * Check if organization exists by organization ID
     */
    boolean existsByOrganizationId(UUID organizationId);
}

