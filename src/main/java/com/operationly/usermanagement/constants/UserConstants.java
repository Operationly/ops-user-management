package com.operationly.usermanagement.constants;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class UserConstants {
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    public static final String BEARER = "Bearer ";
    public static final String BUSINESS_EXCEPTION = "BUSINESS_EXCEPTION";

    @NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    public static final class SecurityConstants {
        public static final String HEADER_USER_ID = "x-user-id";
        public static final String HEADER_WORKOS_USER_ID = "x-workos-user-id";
        public static final String HEADER_USER_EMAIL = "x-user-email";
        public static final String HEADER_USER_ROLE = "x-user-role";
        public static final String HEADER_ORG_ID = "x-org-id";
    }
}
