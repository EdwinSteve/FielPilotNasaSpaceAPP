package com.nameless.nasa.infrastructure.input.rest.auth.response;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
