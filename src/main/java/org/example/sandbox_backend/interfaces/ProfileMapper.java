package org.example.sandbox_backend.interfaces;

import org.example.sandbox_backend.entities.ProfileInfo;
import org.example.sandbox_backend.request.UserProfileRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import java.util.Base64;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProfileMapper {
    void updateProfileFromDto(UserProfileRequestDTO dto, @MappingTarget ProfileInfo entity);

    default byte[] map(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid Base64 string for profile image", e);
        }
    }
}