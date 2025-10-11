package com.example.sandbox_backend.controller;

import com.example.sandbox_backend.Service.UserProfileService;
import com.example.sandbox_backend.dto.UserProfileRequestDTO;
import com.example.sandbox_backend.entities.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping("/api/v1/profile")
@Slf4j
public class UserProfileController {

    private final UserProfileService profileService;

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public Mono<UserProfileRequestDTO> getProfile() {
        return profileService.getProfileDetails();
    }

    // JSON PATCH
    @PatchMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<?> updateProfileJson(@RequestBody UserProfileRequestDTO dto) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .flatMap(principal -> {
                    if (!(principal instanceof CustomUserDetails userDetails)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));
                    }
                    return profileService.updateProfile(userDetails.getEmail(), dto, null);
                });
    }

    @PatchMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<?> updateProfileMultipart(
            @RequestPart(value = "data", required = false) Mono<UserProfileRequestDTO> requestDTO,
            @RequestPart(value = "profileImage", required = false) Mono<FilePart> profileImage) {

        // Wrap requestDTO safely
        Mono<UserProfileRequestDTO> safeRequestDTO = Mono.justOrEmpty(requestDTO)
                .flatMap(Function.identity()) // unwrap nested Mono if any
                .switchIfEmpty(Mono.just(new UserProfileRequestDTO()));

        // Wrap profileImage safely
        Mono<FilePart> safeProfileImage = Mono.justOrEmpty(profileImage)
                .flatMap(Function.identity())
                .switchIfEmpty(Mono.empty()); // optional

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    Object principal = ctx.getAuthentication().getPrincipal();
                    if (principal instanceof CustomUserDetails userDetails) {
                        String username = userDetails.getUsername();

                        // Combine DTO + optional file safely
                        return Mono.zip(safeRequestDTO, safeProfileImage)
                                .flatMap(tuple -> profileService.updateProfile(
                                        userDetails.getEmail(),
                                        tuple.getT1(),
                                        tuple.getT2()
                                ));
                    }
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));
                });
    }


}
