package com.microel.microelhub.security;

import com.microel.microelhub.common.OperatorGroup;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

@AllArgsConstructor
public class JwtAuthentication implements Authentication {
    private boolean isAuth;
    private String login;
    private String name;
    private OperatorGroup role;

    @Override
    public String getName() {
        return login;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Set.of(role);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuth;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        isAuth = isAuthenticated;
    }

    @Override
    public String toString() {
        return "JwtAuthentication{" +
                "isAuth=" + isAuth +
                ", login='" + login + '\'' +
                ", name='" + name + '\'' +
                ", role=" + role +
                '}';
    }
}
