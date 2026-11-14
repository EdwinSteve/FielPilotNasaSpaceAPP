package com.nameless.nasa.infrastructure.input.rest.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    private String fullname;
    @NotBlank(message = "Phone number is required")
    private String phone;
    private String email;
    @NotBlank(message = "Password is required")
    private String password;
    private String municipality;
    private String district;
    private boolean acceptsPolicy;
    private String policyVersion;
}
