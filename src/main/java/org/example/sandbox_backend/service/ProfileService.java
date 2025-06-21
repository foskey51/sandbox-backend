package org.example.sandbox_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.example.sandbox_backend.entities.ProfileInfo;
import org.example.sandbox_backend.entities.UserInfo;
import org.example.sandbox_backend.interfaces.ProfileMapper;
import org.example.sandbox_backend.repository.UserProfileRepository;
import org.example.sandbox_backend.request.UserProfileRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
public class ProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ProfileMapper profileMapper;

    @Transactional(readOnly = true)
    public Optional<ProfileInfo> getProfile(String username) {
        String userId = getUserIdFromSecurityContext();
        return userProfileRepository.findByuserId(userId);
    }

    public String getUserIdFromSecurityContext() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            UserInfo userInfo = (UserInfo) principal;
            return userInfo.getUserId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
    }

    @Transactional
    public ProfileInfo updateProfile(String username, UserProfileRequestDTO requestDTO, MultipartFile profileImage) {
        String userId = getUserIdFromSecurityContext();

        ProfileInfo profile = userProfileRepository.findByuserId(userId)
                .orElse(new ProfileInfo());

        // Set userId in ProfileInfo
        profile.setUserId(userId);

        // Update text fields if provided
        if (requestDTO != null) {
            profileMapper.updateProfileFromDto(requestDTO, profile);
        }

        // Update profile image if provided
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                byte[] imageBytes = profileImage.getBytes();
                profile.setProfileImage(imageBytes);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to process profile image: " + e.getMessage());
            }
        }

        return userProfileRepository.save(profile);
    }

    public String getProfileImageAsBase64(ProfileInfo profile) {
        if (profile.getProfileImage() != null && profile.getProfileImage().length > 0) {
            return Base64.getEncoder().encodeToString(profile.getProfileImage());
        }
        return null;
    }
}