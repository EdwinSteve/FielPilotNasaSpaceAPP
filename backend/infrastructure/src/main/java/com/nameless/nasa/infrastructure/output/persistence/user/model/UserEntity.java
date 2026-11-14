package com.nameless.nasa.infrastructure.output.persistence.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
public class UserEntity {
    @Id
    private UUID id;
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;
    @Column(nullable = false, unique = true, length = 20)
    private String phone;
    @Column(unique = true, length = 160)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false, length = 80)
    private String municipality;
    @Column(length = 80)
    private String district;
    @Column(name = "policy_version", nullable = false, length = 20)
    private String policyVersion;
    @Column(name = "policy_accepted_at", nullable = false)
    private Instant policyAcceptedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserEntity() {
    }
}
