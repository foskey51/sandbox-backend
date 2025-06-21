package org.example.sandbox_backend.response;

import java.util.Base64;

public record UserProfileResponseDTO(
        String username,
        String fullName,
        String email,
        String bio,
        String profileImage // Base64 string
) {
    public UserProfileResponseDTO(String username, String fullName, String email, String bio, byte[] profileImage) {
        this(
                username,
                fullName,
                email,
                bio,
                profileImage != null ? Base64.getEncoder().encodeToString(profileImage) : null
        );
    }
}