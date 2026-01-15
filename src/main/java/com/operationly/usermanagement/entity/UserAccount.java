package com.operationly.usermanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_account", indexes = {
    @Index(name = "idx_workos_user_id", columnList = "workos_user_id", unique = true),
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_organization_id", columnList = "organization_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workos_user_id", nullable = false, unique = true, length = 255)
    private String workosUserId;

    @Column(name = "organization_id", nullable = true, updatable = true)
    private UUID organizationId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Column(name = "role", length = 100)
    private Role role;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean onboardingCompleted = false;

    @Column(name = "profile_picture_url", length = 512)
    private String profilePictureUrl;

    @Column(name = "last_sign_in_at")
    private LocalDateTime lastSignInAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Getter
    @RequiredArgsConstructor
    public enum Role {
        USER("USER"),
        ADMIN("ADMIN"),
        SUPER_ADMIN("SUPER_ADMIN");

        private final String value;
    }
}

