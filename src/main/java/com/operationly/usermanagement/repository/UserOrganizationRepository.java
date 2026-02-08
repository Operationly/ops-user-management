package com.operationly.usermanagement.repository;

import com.operationly.usermanagement.entity.Organization;
import com.operationly.usermanagement.entity.UserAccount;
import com.operationly.usermanagement.entity.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, Long> {

    /**
     * Finds all organizations for a given user.
     * 
     * @param user The user account
     * @return List of user organizations
     */
    List<UserOrganization> findByUser(UserAccount user);

    /**
     * Finds a user organization by user and organization ID.
     * 
     * @param user           The user account
     * @param organizationId The organization ID
     * @return Optional user organization
     */
    Optional<UserOrganization> findByUserAndOrganizationOrganizationId(UserAccount user, UUID organizationId);

    /**
     * Deletes all user organizations for a given user.
     * 
     * @param user The user account
     */
    void deleteByUser(UserAccount user);

    /**
     * Finds all user organizations for a given organization.
     *
     * @param organization The organization
     * @return List of user organizations
     */
    List<UserOrganization> findByOrganization(Organization organization);
}
