package com.operationly.usermanagement.dto;

import com.operationly.usermanagement.entity.Organization;
import com.operationly.usermanagement.entity.Plan;
import com.operationly.usermanagement.entity.Status;
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

    public static OrganizationDto fromEntity(Organization organization) {
        return OrganizationDto.builder()
                .organizationId(organization.getOrganizationId().toString())
                .name(organization.getName())
                .plan(organization.getPlan().getValue())
                .status(organization.getStatus().getValue())
                .createdAt(organization.getCreatedAt().toString())
                .updatedAt(organization.getUpdatedAt().toString())
                .build();
    }

    public static Organization toEntity(OrganizationDto organizationDto) {
        return Organization.builder()
                .name(organizationDto.getName())
                .plan(Plan.valueOf(organizationDto.getPlan()))
                .status(Status.valueOf(organizationDto.getStatus()))
                .build();
    }
}
