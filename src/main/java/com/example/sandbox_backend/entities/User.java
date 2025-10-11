package com.example.sandbox_backend.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("users")
public class User {
    @Id
    private UUID id;
    @Column("full_name")
    private String fullName;
    private String username;
    private String email;
    private String bio;
    private String password;  // BCrypt hashed
    private List<String> roles;     // e.g. "USER"
    @Column("profile_image")
    private byte[] profileImage; // <--- binary image data
}

