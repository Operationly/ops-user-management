package com.operationly.usermanagement.repository;

import com.operationly.usermanagement.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    
    /**
     * Find user account by WorkOS user ID
     */
    Optional<UserAccount> findByWorkosUserId(String workosUserId);
    
    /**
     * Check if user account exists by WorkOS user ID
     */
    boolean existsByWorkosUserId(String workosUserId);
    
    /**
     * Find user account by email
     */
    Optional<UserAccount> findByEmail(String email);
}

