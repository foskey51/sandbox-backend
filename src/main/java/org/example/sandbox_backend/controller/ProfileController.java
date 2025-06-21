package org.example.sandbox_backend.controller;

import org.example.sandbox_backend.entities.UserInfo;
import org.example.sandbox_backend.interfaces.ProfileMapper;
import org.example.sandbox_backend.request.UserProfileRequestDTO;
import org.example.sandbox_backend.response.UserProfileResponseDTO;
import org.example.sandbox_backend.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private ProfileMapper profileMapper;

    @Autowired
    private ProfileService profileService;

    @GetMapping("")
    public ResponseEntity<?> getProfile() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId;
        String username;

        if (principal instanceof UserDetails) {
            UserInfo userInfo = (UserInfo) principal;
            userId = userInfo.getUserId();
            username = userInfo.getUsername();
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        log.info("Fetching profile for userId: {}, username: {}", userId, username);
        return profileService.getProfile(username)
                .map(profile -> {
                    String base64Image = profileService.getProfileImageAsBase64(profile);
                    return ResponseEntity.ok(new UserProfileResponseDTO(
                            username,
                            profile.getFullName(),
                            profile.getEmail(),
                            profile.getBio(),
                            base64Image
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new UserProfileResponseDTO(
                        username,
                        "",
                        "",
                        "",
                        (byte[]) null
                )));
    }

    @PatchMapping(value = "/update", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateProfileMultipart(
            @RequestPart(value = "data", required = false) UserProfileRequestDTO requestDTO,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String userId;
            String username;

            if (principal instanceof UserDetails) {
                UserInfo userInfo = (UserInfo) principal;
                userId = userInfo.getUserId();
                username = userInfo.getUsername();
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
            }

            log.info("Multipart update request for userId: {}, username: {}, data: {}, profileImage: {}, contentType: {}",
                    userId, username, requestDTO, profileImage != null ? profileImage.getContentType() : "absent",
                    profileImage != null ? profileImage.getContentType() : "none");

            var updatedProfile = profileService.updateProfile(username, requestDTO, profileImage);
            String base64Image = profileService.getProfileImageAsBase64(updatedProfile);
            return ResponseEntity.ok(new UserProfileResponseDTO(
                    username,
                    updatedProfile.getFullName(),
                    updatedProfile.getEmail(),
                    updatedProfile.getBio(),
                    base64Image
            ));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating profile: ", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request format: " + e.getMessage(), e);
        }
    }

    @PatchMapping(value = "/update", consumes = {"application/json"})
    public ResponseEntity<?> updateProfileJson(@RequestBody UserProfileRequestDTO requestDTO) {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String userId;
            String username;

            if (principal instanceof UserDetails) {
                UserInfo userInfo = (UserInfo) principal;
                userId = userInfo.getUserId();
                username = userInfo.getUsername();
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
            }

            log.info("JSON update request for userId: {}, username: {}, data: {}", userId, username, requestDTO);

            var updatedProfile = profileService.updateProfile(username, requestDTO, null);
            String base64Image = profileService.getProfileImageAsBase64(updatedProfile);
            return ResponseEntity.ok(new UserProfileResponseDTO(
                    username,
                    updatedProfile.getFullName(),
                    updatedProfile.getEmail(),
                    updatedProfile.getBio(),
                    base64Image
            ));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating profile: ", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON request format: " + e.getMessage(), e);
        }
    }
}