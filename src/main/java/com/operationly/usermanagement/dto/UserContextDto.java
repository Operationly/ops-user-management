package com.operationly.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContextDto {
    private String userId;
    private String workosUserId;
    private String email;
    private String role;
    private String organizationId;
}
