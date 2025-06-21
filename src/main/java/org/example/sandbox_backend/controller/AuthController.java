package org.example.sandbox_backend.controller;

import org.example.sandbox_backend.entities.RefreshToken;
import org.example.sandbox_backend.model.UserInfoDto;
import org.example.sandbox_backend.request.AuthRequestDTO;
import org.example.sandbox_backend.service.JwtService;
import org.example.sandbox_backend.service.RefreshTokenService;
import org.example.sandbox_backend.service.UserDetailsServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@AllArgsConstructor
@RestController
@RequestMapping("/auth/v1")
@CrossOrigin(origins = "https://localhost:5173", allowCredentials = "true")
public class AuthController{

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateAndGetToken(@RequestBody AuthRequestDTO authRequestDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequestDTO.getUsername(), authRequestDTO.getPassword())
        );

        if (authentication.isAuthenticated()) {
            String accessToken = jwtService.GenerateToken(authRequestDTO.getUsername());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(authRequestDTO.getUsername());

            ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", accessToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(60 * 10) // 10 minutes
                    .sameSite("Lax")
                    .build();

            ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", refreshToken.getToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(60 * 60 * 24 *3) // 3 days
                    .sameSite("Lax")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .build();
        } else {
            return new ResponseEntity<>("Authentication failed", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refresh_token", required = false) String refreshTokenValue) {
        if (refreshTokenValue == null) {
            return new ResponseEntity<>("Refresh token missing", HttpStatus.BAD_REQUEST);
        }

        return refreshTokenService.findByToken(refreshTokenValue)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserInfo)
                .map(userInfo -> {
                    String accessToken = jwtService.GenerateToken(userInfo.getUsername());

                    ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", accessToken)
                            .httpOnly(true)
                            .secure(true)
                            .path("/")
                            .maxAge(60 * 10) //10 minutes
                            .sameSite("Lax")
                            .build();

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                            .build();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is expired"));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> SignUp(@RequestBody UserInfoDto userInfoDto){
        try{
            Boolean isSignUped = userDetailsService.signupUser(userInfoDto);
            if (isSignUped == null) {
                return new ResponseEntity<>("Invalid Username or Password", HttpStatus.BAD_REQUEST);
            } else if (Boolean.FALSE.equals(isSignUped)) {
                return new ResponseEntity<>("Username alredy exist", HttpStatus.CONFLICT);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body("Signup Successful");
        }catch (Exception ex){
            return new ResponseEntity<>("Exception in User Service", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refresh_token", required = false) String refreshTokenValue) {
        refreshTokenService.deleteRefreshToken(refreshTokenValue);
        ResponseCookie deleteAccessTokenCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // remove cookie
                .sameSite("Lax")
                .build();

        ResponseCookie deleteRefreshTokenCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // remove cookie
                .sameSite("Lax")
                .build();


        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteAccessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie.toString())
                .body("Logged out successfully");
    }


}
