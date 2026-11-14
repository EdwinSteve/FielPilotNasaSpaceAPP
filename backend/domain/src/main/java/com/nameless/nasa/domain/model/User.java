package com.nameless.nasa.domain.model;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String fullName,
        String phone,
        String email,
        String passwordHash,
        String municipality,
        String district,
        String policyVersion,
        Instant policyAcceptedAt,
        Instant createdAt
) {
}
