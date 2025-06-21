package org.example.sandbox_backend.repository;

import org.example.sandbox_backend.entities.ProfileInfo;
import org.example.sandbox_backend.entities.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<ProfileInfo, String>{
    Optional<ProfileInfo> findByuserId(String userId);
}