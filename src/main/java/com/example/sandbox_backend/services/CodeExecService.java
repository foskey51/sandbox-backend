package com.example.sandbox_backend.services;

import com.example.sandbox_backend.dto.CompilerRequest;
import com.example.sandbox_backend.mappings.Language;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Service
public class CodeExecService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecService.class.getName());
    private final Language language;

    public CodeExecService(Language language) {
        this.language = language;
    }

    public Flux<WebSocketMessage> startExec(WebSocketSession session, CompilerRequest compilerRequestDTO) {
        return Mono.fromCallable(this::isDockerAvailable)
                .flatMapMany(dockerAvailable -> {
                    if (!dockerAvailable) {
                        log.error("Docker is not available or try running with sudo(linux)");
                        return Flux.error(new RuntimeException("Docker not available"));
                    }
                    return executeInDocker(session, compilerRequestDTO);
                })
                .onErrorResume(e -> {
                    log.error("Error in startExec", e);
                    return Flux.just(session.textMessage("Error: " + e.getMessage()));
                });
    }

    private Flux<WebSocketMessage> executeInDocker(WebSocketSession session, CompilerRequest compilerRequestDTO) {
        return Mono.fromCallable(() -> getContainerId(compilerRequestDTO))
                .flatMap(containerId ->
                        Mono.fromCallable(() -> {
                            saveFileToContainer(compilerRequestDTO, containerId);
                            return containerId;
                        })
                )
                .flatMapMany(containerId ->
                        executePtyProcess(session, compilerRequestDTO, containerId)
                                .doFinally(signalType ->
                                        Mono.fromRunnable(() -> {
                                            if (!destroyContainer(containerId)) {
                                                log.error("Container with id {} could not be destroyed.", containerId);
                                            }
                                        }).subscribeOn(Schedulers.boundedElastic()).subscribe()
                                )
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<WebSocketMessage> executePtyProcess(WebSocketSession session,
                                                     CompilerRequest compilerRequestDTO,
                                                     String containerId) {
        return Flux.create(sink -> {
            try {
                String command = language.getExecCommand().get(compilerRequestDTO.language().trim());
                String[] cmd = {
                        "sudo", "docker", "exec", "-it", containerId,
                        "sh", "-c", command
                };

                PtyProcess process = new PtyProcessBuilder(cmd).start();
                session.getAttributes().put("ptyProcess", process);
                session.getAttributes().put("containerId", containerId);

                InputStream input = process.getInputStream();
                byte[] buffer = new byte[1024];
                int len;

                while ((len = input.read(buffer)) != -1) {
                    String output = new String(buffer, 0, len);
                    sink.next(session.textMessage(output));
                }

                int exitCode = process.waitFor();
                sink.next(session.textMessage("\n ----- Exited with status " + exitCode + " -----"));
                sink.complete();

            } catch (Exception e) {
                log.error("Error while reading process output", e);
                sink.error(e);
            }
        });
    }

    public Mono<Void> handleExec(WebSocketSession session, String data) {
        return Mono.fromRunnable(() -> {
            try {
                PtyProcess process = (PtyProcess) session.getAttributes().get("ptyProcess");
                if (process != null) {
                    OutputStream output = process.getOutputStream();
                    output.write(data.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                }
            } catch (Exception e) {
                log.error("handleExec: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public String getContainerId(CompilerRequest compilerRequestDTO) throws Exception {
        String languageTrimmed = compilerRequestDTO.language().trim();
        ProcessBuilder processBuilder = new ProcessBuilder(
                "sudo", "docker", "run",
                "--rm", "-d",
                "--user", "1002",
                "--memory", "256m",
                "--pids-limit", "100",
                "--ulimit", "cpu=15",
                "--ulimit", "nproc=100",
                "--ulimit", "fsize=5000000",
                "--security-opt", "no-new-privileges:true",
                languageTrimmed + "-image"
        );
        Process process = processBuilder.start();
        String containerId = new String(process.getInputStream().readAllBytes()).trim();
        int exitCode = process.waitFor();

        if (!(exitCode == 0) || !isValidContainerId(containerId)) {
            throw new RuntimeException("Error spinning up the container");
        }
        return containerId;
    }

    public void saveFileToContainer(CompilerRequest compilerRequestDTO, String containerId) throws Exception {
        String extension = language.getLanguageMap().get(compilerRequestDTO.language());
        String base64Code = compilerRequestDTO.getSourceCode2base64();
        String command = String.format("echo '%s' | base64 -d > App%s", base64Code, extension);

        String[] cmd = {
                "sudo", "docker", "exec", "-i", containerId,
                "sh", "-c", "stty -echo; " + command
        };

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Error saving file to container");
        }
    }

    private static boolean isValidContainerId(String containerId) {
        String regex = "^[0-9a-f]{64}$";
        return Pattern.matches(regex, containerId);
    }

    public boolean isDockerAvailable() {
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

    public boolean destroyContainer(String containerId) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sudo", "docker", "rm", "-f", containerId);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.error("Error destroying the container", e);
            return false;
        }
    }
}