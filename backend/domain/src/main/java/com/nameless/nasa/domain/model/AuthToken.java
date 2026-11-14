package com.nameless.nasa.domain.model;

public record AuthToken(String accessToken, long expiresInSeconds) {
}
