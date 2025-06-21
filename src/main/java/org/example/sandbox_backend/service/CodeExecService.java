package org.example.sandbox_backend.service;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.example.sandbox_backend.mappings.Language;
import org.example.sandbox_backend.request.CompilerRequestDTO;
import org.example.sandbox_backend.websocket.WebSocketController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Service
public class CodeExecService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecService.class.getName());
    public final Language language;

    public CodeExecService(Language language) {
        this.language = language;
    }

    public void startExec(WebSocketSession session, CompilerRequestDTO compilerRequestDTO) throws IOException, InterruptedException {
        if(!isDockerAvailable()){
            log.error("Docker is not available or try running with sudo(linux)");
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }
        String containerId = getContainerId(compilerRequestDTO);
        saveFileTOContainer(compilerRequestDTO, containerId);

        String command = language.getExecCommand().get(compilerRequestDTO.language().trim());
        String[] cmd = {
                "sudo", "docker", "exec", "-it", containerId,
                "sh", "-c", command
        };

        PtyProcess process = new PtyProcessBuilder(cmd).start();
        // Save process reference
        session.getAttributes().put("ptyProcess", process);

        // Start reading output
        Thread vt = Thread.ofVirtual().start(() -> {
            try {
                InputStream input = process.getInputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = input.read(buffer)) != -1) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(buffer));
                    } else {
                        log.warn("WebSocket session closed. Stopping output thread.");
                        break;
                    }
                }
                int exitCode = process.waitFor();
                if(session.isOpen()) {
                    session.sendMessage(new TextMessage("\n ----- Exited with status "+exitCode+" -----"));
                }
                session.close(CloseStatus.NORMAL);
            } catch (Exception e) {
                log.error("startExec: error while reading process output", e);
            }
        });
    }


    public void handleExec(WebSocketSession session, TextMessage textMessage) throws IOException, InterruptedException {
        PtyProcess process = (PtyProcess) session.getAttributes().get("ptyProcess");

        try {
            OutputStream output = process.getOutputStream();
            output.write(textMessage.asBytes());
            output.flush();
        } catch (Exception e) {
            log.error("handleExec: " + e.getMessage());
        }

    }

    public String getContainerId(CompilerRequestDTO compilerRequestDTO) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("sudo", "docker", "run", "--rm", "-d", compilerRequestDTO.language().trim() + "-image");
        Process process = processBuilder.start();
        String containerId = new String(process.getInputStream().readAllBytes()).trim();
        int exitCode = process.waitFor();
        if (!(exitCode == 0) || !isValidContainerId(containerId)) {
            throw new RuntimeException("Error spinning up the container");
        }
        return containerId;
    }

    public void saveFileTOContainer(CompilerRequestDTO compilerRequestDTO, String containerId) throws IOException, InterruptedException {
        String extension = language.getLanguageMap().get(compilerRequestDTO.language());
        String base64Code = compilerRequestDTO.getSourceCode2base64();

        String command = String.format("echo '%s' | base64 -d > App%s", base64Code, extension);

        ProcessBuilder processBuilder = new ProcessBuilder("sudo", "docker", "exec", "-i", containerId, "sh", "-c", command);

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
        } catch (IOException | InterruptedException e) {
            System.err.print("Docker availability check failed "+ e);
            return false;
        }
    }
}
