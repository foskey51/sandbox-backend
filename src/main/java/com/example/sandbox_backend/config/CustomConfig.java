package com.example.sandbox_backend.config;

import com.example.sandbox_backend.util.NoVncUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class CustomConfig {

    @Bean
    public ConcurrentHashMap<String, NoVncUserDetails> user2noVncMap() {
        return new ConcurrentHashMap<>();
    }
}
