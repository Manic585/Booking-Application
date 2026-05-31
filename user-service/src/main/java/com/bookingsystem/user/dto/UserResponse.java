package com.bookingsystem.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean active;
    private Instant createdAt;
}
