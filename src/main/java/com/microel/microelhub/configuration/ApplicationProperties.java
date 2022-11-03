package com.microel.microelhub.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
@Getter
@Setter
public class ApplicationProperties {
    private String authUserTokenSecretKey = "";
    private String authRefreshTokenSecretKey = "";
}
