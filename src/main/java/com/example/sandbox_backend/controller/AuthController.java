package com.example.sandbox_backend.controller;

import com.example.sandbox_backend.services.AuthService;
import com.example.sandbox_backend.dto.AuthRequest;
import com.example.sandbox_backend.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.access-token-minutes}") long access_expires_minutes;
    @Value("${jwt.refresh-token-days}") long refresh_expires_days;

    @PostMapping("/signup")
    public Mono<ResponseEntity<String>> signup(@RequestBody SignupRequest req) {
        return authService.signup(req)
                .map(msg -> ResponseEntity.ok().body(msg))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<String>> login(@RequestBody AuthRequest req) {
        return authService.login(req)
                .map(authResponse -> {
                    ResponseCookie accessCookie = ResponseCookie.from("access_token", authResponse.getAccessToken())
                            .httpOnly(true)
                            .secure(true)
                            .path("/")
                            .maxAge(Duration.ofMinutes(access_expires_minutes))
                            .sameSite("None")
                            .build();

                    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", authResponse.getRefreshToken())
                            .httpOnly(true)
                            .secure(true)
                            .path("/")
                            .maxAge(Duration.ofDays(refresh_expires_days))
                            .sameSite("None")
                            .build();
                    return ResponseEntity.ok()
                            .headers(h -> {
                                h.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
                                h.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
                            })
                            .body("Login successful");
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body("Login failed")));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<String>> refresh(@CookieValue(name = "refresh_token", required = true) String refreshTokenValue) {
        return authService.refresh(refreshTokenValue)
                .map(authResponse -> {
                    ResponseCookie accessCookie = ResponseCookie.from("access_token", authResponse.getAccessToken())
                            .httpOnly(true)
                            .secure(true)
                            .path("/")
                            .maxAge(Duration.ofMinutes(access_expires_minutes))
                            .sameSite("None")
                            .build();

                    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", authResponse.getRefreshToken())
                            .httpOnly(true)
                            .secure(true)
                            .path("/")
                            .maxAge(Duration.ofDays(refresh_expires_days))
                            .sameSite("None")
                            .build();
                    return ResponseEntity.ok()
                            .headers(h -> {
                                h.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
                                h.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
                            })
                            .body("Refresh successful");
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body("Failed to refresh token")));

    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<?>> logout(@CookieValue(value = "refresh_token",required = true) String refreshToken) {
        return authService.logout(refreshToken)
                .then(Mono.just(ResponseEntity.ok().body("Logged out")));
    }
}