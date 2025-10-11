package com.example.sandbox_backend.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.net.URL;

@AllArgsConstructor
@Getter
public class NoVncUserDetails {
    String containerId;
    URL url;
}
