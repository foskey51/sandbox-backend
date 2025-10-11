package com.example.sandbox_backend.Service;

import com.example.sandbox_backend.Repositories.RefreshTokenRepository;
import com.example.sandbox_backend.Repositories.UserRepository;
import com.example.sandbox_backend.dto.AuthRequest;
import com.example.sandbox_backend.dto.AuthResponse;
import com.example.sandbox_backend.dto.SignupRequest;
import com.example.sandbox_backend.entities.CustomUserDetails;
import com.example.sandbox_backend.entities.RefreshToken;
import com.example.sandbox_backend.entities.User;
import com.example.sandbox_backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReactiveAuthenticationManager reactiveAuthenticationManager;
    private final JwtUtil jwtUtil;

    public Mono<String> signup(SignupRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank() ||
                req.getPassword() == null || req.getPassword().isBlank()) {
            return Mono.error(new RuntimeException("Email and password are required"));
        }

        return userRepository.findByEmail(req.getEmail().toLowerCase())
                .flatMap(existing -> Mono.<String>error(new RuntimeException("Email already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    User user = User.builder()
                            .email(req.getEmail().toLowerCase())
                            .password(passwordEncoder.encode(req.getPassword()))
                            .roles(List.of("USER"))
                            .build();

                    return userRepository.save(user)
                            .thenReturn("User registered successfully");
                }));
    }

    public Mono<AuthResponse> login(AuthRequest req) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword());

        return reactiveAuthenticationManager.authenticate(authToken)
                .flatMap(auth -> {
                    CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

                    // Convert authorities to strings
                    List<String> roles = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .toList();

                    // Generate tokens
                    String access = jwtUtil.generateAccessToken(userDetails.getId().toString(), roles);
                    String refresh = jwtUtil.generateRefreshToken(userDetails.getId().toString(), roles);
                    long expiresAt = System.currentTimeMillis() + jwtUtil.getRefreshExpiryMillis();

                    RefreshToken token = RefreshToken.builder()
                            .userId(userDetails.getId())
                            .token(refresh)
                            .expiresAt(expiresAt)
                            .build();

                    return refreshTokenRepository.save(token)
                            .map(saved -> new AuthResponse(access, refresh));
                })
                .onErrorMap(e -> new RuntimeException("Invalid credentials"));
    }


    public Mono<AuthResponse> refresh(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid refresh token")))
                .flatMap(rt -> {
                    if (rt.getExpiresAt() < System.currentTimeMillis()) {
                        return refreshTokenRepository.delete(rt)
                                .then(Mono.error(new RuntimeException("Refresh token expired")));
                    }
                    String access = jwtUtil.generateAccessToken(rt.getUserId().toString(), List.of("USER")); //User role hardCoded

                    return Mono.just(new AuthResponse(access, refreshToken));
                })
                .onErrorMap(e -> new RuntimeException("Invalid refresh token"));
    }

    public Mono<Void> logout(String refreshToken) {
        return refreshTokenRepository.deleteByToken(refreshToken);
    }
}
