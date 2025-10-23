package com.example.sandbox_backend.services;

import com.example.sandbox_backend.repositories.UserRepository;
import com.example.sandbox_backend.dto.UserProfileRequestDTO;
import com.example.sandbox_backend.entities.CustomUserDetails;
import com.example.sandbox_backend.entities.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Base64;
@Service
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Get current user's profile
    public Mono<UserProfileRequestDTO> getProfileDetails() {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    Object principal = ctx.getAuthentication().getPrincipal();
                    if (!(principal instanceof CustomUserDetails userDetails)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal"));
                    }
                    return userRepository.findByEmail(userDetails.getEmail())
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                            .map(this::toDto);
                });
    }

    // Update profile with optional FilePart
    public Mono<UserProfileRequestDTO> updateProfile(String currentEmail, UserProfileRequestDTO dto, FilePart profileImage) {
        return userRepository.findByEmail(currentEmail)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .flatMap(user -> {
                    // If email is being updated
                    if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(currentEmail)) {
                        return userRepository.findByEmail(dto.getEmail())
                                .flatMap(existing -> Mono.<UserProfileRequestDTO>error(
                                        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use")))
                                .switchIfEmpty(applyUpdates(user, dto, profileImage));
                    }
                    // If username already exist

                    


                    // No email change, just apply updates
                    return applyUpdates(user, dto, profileImage);
                });
    }


    // Apply partial updates
    private Mono<UserProfileRequestDTO> applyUpdates(User user, UserProfileRequestDTO dto, FilePart profileImage) {
        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getBio() != null) user.setBio(dto.getBio());

        Mono<Void> imageMono;
        if (profileImage != null) {
            imageMono = saveProfileImage(profileImage).map(bytes -> {
                user.setProfileImage(bytes);
                return bytes;
            }).then();
        } else if (dto.getProfileImage() != null) { // Base64 string from JSON
            try {
                user.setProfileImage(Base64.getDecoder().decode(dto.getProfileImage()));
            } catch (IllegalArgumentException e) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Base64 profile image"));
            }
            imageMono = Mono.empty();
        } else {
            imageMono = Mono.empty();
        }

        return imageMono.then(userRepository.save(user).map(this::toDto));
    }

    // Convert FilePart to byte[]
    private Mono<byte[]> saveProfileImage(FilePart filePart) {
        return filePart.content()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    return bytes;
                })
                .reduce(this::concatBytes);
    }

    private byte[] concatBytes(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private UserProfileRequestDTO toDto(User user) {
        String base64Image = user.getProfileImage() != null ? Base64.getEncoder().encodeToString(user.getProfileImage()) : null;
        return new UserProfileRequestDTO(user.getUsername(),user.getFullName(), user.getEmail(), user.getBio(), base64Image);
    }
}
