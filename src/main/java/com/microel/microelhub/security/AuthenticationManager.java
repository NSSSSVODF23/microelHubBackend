package com.microel.microelhub.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.microel.microelhub.api.transport.LoginRequest;
import com.microel.microelhub.api.transport.TokensResponse;
import com.microel.microelhub.common.OperatorGroup;
import com.microel.microelhub.configuration.ApplicationProperties;
import com.microel.microelhub.storage.OperatorDispatcher;
import com.microel.microelhub.storage.entity.Operator;
import lombok.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthenticationManager {

    private final OperatorDispatcher operatorDispatcher;
    private final ApplicationProperties applicationProperties;

    public AuthenticationManager(OperatorDispatcher operatorDispatcher, ApplicationProperties applicationProperties) {
        this.operatorDispatcher = operatorDispatcher;
        this.applicationProperties = applicationProperties;
    }

    public TokensResponse doLogin(LoginRequest request) throws Exception {
        Operator operator = operatorDispatcher.getByLogin(request.getLogin());
        if(operator == null || !operator.getPassword().equals(request.getPassword())) throw new Exception("Не верный логин или пароль");
        operatorDispatcher.updateLastLoginTime(operator);
        return new TokensResponse(generateUserToken(operator), generateRefreshToken(operator));
    }

    public TokensResponse doRefresh(@Nullable DecodedJWT refreshJwt) throws Exception {
        if(refreshJwt == null) throw new Exception("Токен не действителен");
        Operator operator = operatorDispatcher.getByLogin(refreshJwt.getSubject());
        if(operator == null) throw new Exception("Пользователь не найден");
        return new TokensResponse(generateUserToken(operator), generateRefreshToken(operator));
    }

    public String generateUserToken(Operator operator) {
        return JWT.create().withSubject(operator.getLogin())
                .withClaim("role", operator.getRole().name())
                .withClaim("name", operator.getName())
                .withExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .sign(Algorithm.HMAC512(applicationProperties.getAuthUserTokenSecretKey()));
    }

    public String generateRefreshToken(Operator operator){
        return JWT.create().withSubject(operator.getLogin())
                .withExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .sign(Algorithm.HMAC512(applicationProperties.getAuthRefreshTokenSecretKey()));
    }

    public DecodedJWT validateUserToken(@Nullable String token) throws Exception {
        if(token == null) throw new Exception("Пустой токен");
        try {
            return JWT.require(Algorithm.HMAC512(applicationProperties.getAuthUserTokenSecretKey()))
                    .build().verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public DecodedJWT validateRefreshToken(String token) {
        try {
            return JWT.require(Algorithm.HMAC512(applicationProperties.getAuthRefreshTokenSecretKey()))
                    .build().verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public JwtAuthentication getAuthentication(DecodedJWT jwt) throws Exception {
        final Operator operator = operatorDispatcher.getByLogin(jwt.getSubject());
        if (operator == null)
            throw new Exception("Пользователь не найден");
        return new JwtAuthentication(true, operator.getLogin(), operator.getName(), operator.getRole());
    }

}
