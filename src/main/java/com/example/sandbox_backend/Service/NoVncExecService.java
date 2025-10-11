package com.example.sandbox_backend.Service;

import com.example.sandbox_backend.entities.CustomUserDetails;
import com.example.sandbox_backend.util.NoVncUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

@Service
public class NoVncExecService {
    private static final Logger log = LoggerFactory.getLogger(NoVncExecService.class);
    private static final String CONTAINER_NAME = "ubuntu-novnc";
    private static final String CONTAINER_ID_REGEX = "^[0-9a-f]{64}$";

    private final DynamicRouteService dynamicRouteService;
    private final ConcurrentHashMap<String, NoVncUserDetails> user2noVncMap;
    private final ConcurrentHashMap<String, Lock> userLocks = new ConcurrentHashMap<>();

    public NoVncExecService(DynamicRouteService dynamicRouteService,
                            ConcurrentHashMap<String, NoVncUserDetails> user2noVncMap) {
        this.dynamicRouteService = dynamicRouteService;
        this.user2noVncMap = user2noVncMap;
    }

    public Flux<String> startExec() {
        return getCurrentUserId()
                .flatMapMany(userId -> {
                    String userKey = userId.toString();

                    Lock userLock = userLocks.computeIfAbsent(userKey, k -> new ReentrantLock());

                    return Mono.fromCallable(() -> {
                        userLock.lock();
                        try {
                            if (!isDockerAvailable()) {
                                throw new RuntimeException("Docker not available");
                            }

                            // destroy previous container if exists
                            cleanupExistingContainer(userKey);

                            // Create new container
                            String containerId = createContainer();
                            log.info("Created new container {} for user {}", containerId, userKey);

                            // Get container IP and create internal URL
                            String ip = getContainerIp(containerId);
                            URL internalUrl = new URL("http", ip, 6080, "/vnc.html");
                            log.info("Generated internal VNC URL: {}", internalUrl);

                            // Add dynamic route
                            RouteDefinition routeDefinition = dynamicRouteService.addUserRoute(
                                    userKey, internalUrl.toString());

                            // Save container details
                            URL proxyUrl = routeDefinition.getUri().toURL();
                            user2noVncMap.put(userKey, new NoVncUserDetails(containerId, proxyUrl));

                            return proxyUrl.toString();
                        } catch (Exception e) {
                            log.error("Error in startExec for user {}", userKey, e);
                            throw new RuntimeException("Failed to start container", e);
                        } finally {
                            userLock.unlock();
                        }
                    }).flux();
                })
                .onErrorResume(e -> {
                    log.error("Error in startExec", e);
                    return Flux.error(e);
                });
    }

    private void cleanupExistingContainer(String userKey) {
        NoVncUserDetails existingDetails = user2noVncMap.remove(userKey);
        if (existingDetails != null) {
            log.info("Cleaning up existing container {} for user: {}",
                    existingDetails.getContainerId(), userKey);
            destroyContainer(existingDetails.getContainerId());
            dynamicRouteService.removeUserRoute(userKey);
        }
    }

    private String createContainer() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "sudo", "docker", "run",
                "--rm", "-d",
                "--memory", "1024m",
                "--ulimit", "cpu=600",
                CONTAINER_NAME
        );
        Process process = processBuilder.start();
        String containerId = new String(process.getInputStream().readAllBytes()).trim();
        int exitCode = process.waitFor();

        if (exitCode != 0 || !isValidContainerId(containerId)) {
            throw new RuntimeException("Error creating container");
        }
        return containerId;
    }

    private boolean isValidContainerId(String containerId) {
        return Pattern.matches(CONTAINER_ID_REGEX, containerId);
    }

    private boolean isDockerAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "info");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.error("Docker availability check failed", e);
            return false;
        }
    }

    private String getContainerIp(String containerId) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "inspect", "-f",
                "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}",
                containerId
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String ip = reader.readLine();
            process.waitFor();

            if (ip == null || ip.isEmpty()) {
                throw new IllegalStateException("No IP found for container " + containerId);
            }

            return ip.trim();
        }
    }

    private Mono<UUID> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication().getPrincipal())
                .filter(principal -> principal instanceof CustomUserDetails)
                .map(principal -> ((CustomUserDetails) principal).getId());
    }

    private void destroyContainer(String containerId) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sudo", "docker", "rm", "-f", containerId);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(line -> log.debug("docker rm output: {}", line));
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Failed to destroy container {}", containerId);
            } else {
                log.info("Successfully destroyed container {}", containerId);
            }
        } catch (Exception e) {
            log.error("Error destroying container", e);
        }
    }
}