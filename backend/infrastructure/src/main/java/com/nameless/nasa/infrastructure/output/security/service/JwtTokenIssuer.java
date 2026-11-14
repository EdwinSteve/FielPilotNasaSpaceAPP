package com.nameless.nasa.infrastructure.output.security.service;

import com.nameless.nasa.domain.model.AuthToken;
import com.nameless.nasa.domain.model.User;
import com.nameless.nasa.domain.ports.output.TokenIssuer;
import com.nameless.nasa.infrastructure.output.security.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuer {
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    @Override
    public AuthToken issue(User user) {
        Instant now = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.expiration()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthToken(token, properties.expiration().toSeconds());
    }
}
