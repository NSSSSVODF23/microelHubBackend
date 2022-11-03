package com.microel.microelhub.common;

import org.springframework.security.core.GrantedAuthority;

public enum OperatorGroup implements GrantedAuthority {
    ADMIN("ADMIN"),USER("USER");

    private final String role;

    OperatorGroup(String role){
        this.role = role;
    }

    @Override
    public String getAuthority() {
        return "ROLE_"+role;
    }
}
