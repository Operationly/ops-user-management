package com.operationly.usermanagement.filter;

import com.operationly.usermanagement.dto.UserContextDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.operationly.usermanagement.constants.UserConstants.SecurityConstants.*;

@Component
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String workosUserId = request.getHeader(HEADER_WORKOS_USER_ID);
        String email = request.getHeader(HEADER_USER_EMAIL);
        String role = request.getHeader(HEADER_USER_ROLE);
        String orgId = request.getHeader(HEADER_ORG_ID);

        if (userId != null && workosUserId != null && email != null && role != null) {
            log.debug("Populating SecurityContext for user: {}", userId);

            UserContextDto userContext = UserContextDto.builder()
                    .userId(userId)
                    .workosUserId(workosUserId)
                    .email(email)
                    .role(role)
                    .organizationId(orgId)
                    .build();

            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + role));

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userContext, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            log.debug("Missing user context headers. Skipping SecurityContext population.");
        }

        filterChain.doFilter(request, response);
    }
}
