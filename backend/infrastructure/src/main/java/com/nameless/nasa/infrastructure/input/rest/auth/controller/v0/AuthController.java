package com.nameless.nasa.infrastructure.input.rest.auth.controller.v0;

import com.nameless.nasa.infrastructure.input.rest.auth.request.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v0/auth")
@RequiredArgsConstructor
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
