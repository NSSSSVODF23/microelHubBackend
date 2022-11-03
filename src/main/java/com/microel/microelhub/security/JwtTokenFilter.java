package com.microel.microelhub.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class JwtTokenFilter extends GenericFilterBean {

    private final AuthenticationManager authenticationManager;

    public JwtTokenFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            DecodedJWT decodedJWT = authenticationManager.validateUserToken(getTokenFromRequest((HttpServletRequest) servletRequest));
            SecurityContextHolder.getContext().setAuthentication(authenticationManager.getAuthentication(decodedJWT));
        } catch (Exception ignored) {

        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String getTokenFromRequest(HttpServletRequest request)  {
        final String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token)) {
            return token.replace("Bearer ", "");
        }
        return null;
    }
}
