package com.example.sandbox_backend.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("refresh_tokens")
public class RefreshToken {
    @Id
    private UUID id;
    private UUID userId;
    private String token;
    private long expiresAt;  // epoch millis instead of OffsetDateTime
}

