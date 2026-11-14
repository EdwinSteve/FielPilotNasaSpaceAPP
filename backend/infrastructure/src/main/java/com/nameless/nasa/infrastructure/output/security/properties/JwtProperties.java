package com.nameless.nasa.infrastructure.output.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {
}
