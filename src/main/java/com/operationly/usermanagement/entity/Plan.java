package com.operationly.usermanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Plan {
    FREE("FREE"),
    PRO("PRO"),
    ENTERPRISE("ENTERPRISE");

    private final String value;
}
