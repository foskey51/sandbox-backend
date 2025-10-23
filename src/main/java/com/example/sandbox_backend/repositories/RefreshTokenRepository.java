package com.example.sandbox_backend.repositories;

import com.example.sandbox_backend.entities.RefreshToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, UUID> {
    Mono<RefreshToken> findByToken(String token);
    Mono<Void> deleteByToken(String token);
}
