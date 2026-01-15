package com.operationly.usermanagement.service;

import com.operationly.usermanagement.dto.UserAccountDto;
import com.operationly.usermanagement.entity.Organization;
import com.operationly.usermanagement.entity.UserAccount;
import com.operationly.usermanagement.repository.OrganizationRepository;
import com.operationly.usermanagement.repository.UserAccountRepository;
import com.operationly.usermanagement.service.impl.UserAccountServiceImpl;
import com.workos.usermanagement.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private WorkOSService workOSService;

    @InjectMocks
    private UserAccountServiceImpl userAccountService;

    private User workosUser;
    private final String WORKOS_USER_ID = "succ_123";
    private final String EMAIL = "test@example.com";
    private final String FIRST_NAME = "Test";
    private final String LAST_NAME = "User";

    @BeforeEach
    void setUp() {
        workosUser = mock(User.class);
        when(workosUser.getEmail()).thenReturn(EMAIL);
        when(workosUser.getFirstName()).thenReturn(FIRST_NAME);
        when(workosUser.getLastName()).thenReturn(LAST_NAME);
        when(workosUser.getEmailVerified()).thenReturn(true);
        when(workosUser.getProfilePictureUrl()).thenReturn("http://pic.url");
    }

    @Test
    void syncUserAccount_NewUser_ShouldCreateUser() {
        when(workOSService.getWorkOsUserById(WORKOS_USER_ID)).thenReturn(workosUser);
        when(userAccountRepository.findByWorkosUserId(WORKOS_USER_ID)).thenReturn(Optional.empty());

        UserAccount savedUser = UserAccount.builder()
                .id(1L)
                .workosUserId(WORKOS_USER_ID)
                .email(EMAIL)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .emailVerified(true)
                .profilePictureUrl("http://pic.url")
                .role(UserAccount.Role.USER)
                .build();

        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedUser);

        UserAccountDto result = userAccountService.syncUserAccount(WORKOS_USER_ID, null);

        assertNotNull(result);
        assertEquals(EMAIL, result.getEmail());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    void syncUserAccount_ExistingUser_ShouldUpdateUser() {
        when(workOSService.getWorkOsUserById(WORKOS_USER_ID)).thenReturn(workosUser);

        UserAccount existingUser = UserAccount.builder()
                .id(1L)
                .workosUserId(WORKOS_USER_ID)
                .email("old@example.com")
                .role(UserAccount.Role.USER)
                .build();

        when(userAccountRepository.findByWorkosUserId(WORKOS_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(existingUser);

        UserAccountDto result = userAccountService.syncUserAccount(WORKOS_USER_ID, null);

        assertNotNull(result);
        assertEquals(EMAIL, result.getEmail()); // Should be updated
        verify(userAccountRepository).save(existingUser);
    }

    @Test
    void syncUserAccount_ExistingUser_ShouldUpdateOrganizationIfNull() {
        when(workOSService.getWorkOsUserById(WORKOS_USER_ID)).thenReturn(workosUser);

        UserAccount existingUser = UserAccount.builder()
                .id(1L)
                .workosUserId(WORKOS_USER_ID)
                .email(EMAIL)
                .organizationId(null)
                .role(UserAccount.Role.USER)
                .build();

        UUID orgId = UUID.randomUUID();

        when(userAccountRepository.findByWorkosUserId(WORKOS_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(existingUser);

        userAccountService.syncUserAccount(WORKOS_USER_ID, orgId);

        assertEquals(orgId, existingUser.getOrganizationId());
    }

    @Test
    void syncUserAccount_ExistingUser_ShouldNotUpdateOrganizationIfNotNull() {
        when(workOSService.getWorkOsUserById(WORKOS_USER_ID)).thenReturn(workosUser);

        UUID oldOrgId = UUID.randomUUID();
        UserAccount existingUser = UserAccount.builder()
                .id(1L)
                .workosUserId(WORKOS_USER_ID)
                .email(EMAIL)
                .organizationId(oldOrgId)
                .role(UserAccount.Role.USER)
                .build();

        UUID newOrgId = UUID.randomUUID();

        Organization mockOrg = mock(Organization.class);
        when(mockOrg.getOrganizationId()).thenReturn(oldOrgId);

        when(userAccountRepository.findByWorkosUserId(WORKOS_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(existingUser);
        when(organizationRepository.findByOrganizationId(oldOrgId)).thenReturn(Optional.of(mockOrg));

        userAccountService.syncUserAccount(WORKOS_USER_ID, newOrgId);

        assertEquals(oldOrgId, existingUser.getOrganizationId());
    }
}
