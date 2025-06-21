package org.example.sandbox_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.example.sandbox_backend.entities.UserInfo;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserInfo, String>{
    public UserInfo findByUsername(String username);
}