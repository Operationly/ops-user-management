package com.operationly.usermanagement.filter;

import com.operationly.usermanagement.dto.UserContextDto;
import com.operationly.usermanagement.filter.UserContextFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserContextFilterTest {

    @InjectMocks
    private UserContextFilter userContextFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithValidHeaders_ShouldSetSecurityContext() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("x-user-id")).thenReturn("123");
        when(request.getHeader("x-workos-user-id")).thenReturn("workos_123");
        when(request.getHeader("x-user-email")).thenReturn("test@example.com");
        when(request.getHeader("x-user-role")).thenReturn("ADMIN");
        when(request.getHeader("x-org-id")).thenReturn("org_123");

        // Act
        userContextFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());

        UserContextDto userContext = (UserContextDto) authentication.getPrincipal();
        assertEquals("123", userContext.getUserId());
        assertEquals("workos_123", userContext.getWorkosUserId());
        assertEquals("test@example.com", userContext.getEmail());
        assertEquals("ADMIN", userContext.getRole());
        assertEquals("org_123", userContext.getOrganizationId());

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithMissingHeaders_ShouldNotSetSecurityContext() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("x-user-id")).thenReturn(null);

        // Act
        userContextFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithPartialHeaders_ShouldNotSetSecurityContext() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("x-user-id")).thenReturn("123");
        // Missing workos_user_id
        when(request.getHeader("x-workos-user-id")).thenReturn(null);

        // Act
        userContextFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
