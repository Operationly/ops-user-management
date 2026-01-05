package com.operationly.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAccountDto {
    private Long id;
    private String workosUserId;
    private OrganizationDto organization;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean emailVerified;
    private Boolean onboardingCompleted;
    private String profilePictureUrl;
    private String lastSignInAt;
    private String createdAt;
    private String updatedAt;
}
