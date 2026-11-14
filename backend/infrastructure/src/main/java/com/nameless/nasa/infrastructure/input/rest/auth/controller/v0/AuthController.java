package com.nameless.nasa.infrastructure.input.rest.auth.controller.v0;

import com.nameless.nasa.application.handler.AuthHandler;
import com.nameless.nasa.infrastructure.input.rest.auth.mapper.AuthRestMapper;
import com.nameless.nasa.infrastructure.input.rest.auth.request.LoginRequest;
import com.nameless.nasa.infrastructure.input.rest.auth.request.RegisterUserRequest;
import com.nameless.nasa.infrastructure.input.rest.auth.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v0/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthHandler authHandler;
    private final AuthRestMapper mapper;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterUserRequest request) {
        authHandler.register(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(mapper.toResponse(authHandler.login(mapper.toCommand(request))));
    }
}
