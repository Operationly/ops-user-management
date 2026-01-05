package com.operationly.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrganizationDto {
    private String organizationId;
    private String name;
    private String plan;
    private String status;
    private String createdAt;
    private String updatedAt;
}
