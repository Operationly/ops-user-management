package com.operationly.usermanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Status {
    ACTIVE("ACTIVE"),
    SUSPENDED("SUSPENDED");

    private final String value;
}
