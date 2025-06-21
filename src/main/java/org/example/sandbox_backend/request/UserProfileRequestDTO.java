package org.example.sandbox_backend.request;

public record UserProfileRequestDTO (
     String fullName,
     String email,
     String bio,
     String profileImage // Base64 string for frontend compatibility
){
}