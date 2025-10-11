package com.example.sandbox_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Builder
@NotNull
public record CompilerRequest(String language, String sourceCode) {
    public String getSourceCode2base64(){
        return Base64.getEncoder().encodeToString(sourceCode.getBytes(StandardCharsets.UTF_8));
    }
}