package com.example.sandbox_backend.websocket;

import com.example.sandbox_backend.Service.CodeExecService;
import com.example.sandbox_backend.dto.CompilerRequest;
import com.example.sandbox_backend.util.ValidationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pty4j.PtyProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ReactiveWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ReactiveWebSocketHandler.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();
    private final CodeExecService codeExecService;

    public ReactiveWebSocketHandler(CodeExecService codeExecService) {
        this.codeExecService = codeExecService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("WebSocket connection established: {}", session.getId());

        Flux<WebSocketMessage> output = session.receive()
                .flatMap(message -> handleMessage(session, message))
                .doOnError(e -> log.error("Error in WebSocket session", e))
                .doFinally(signalType -> cleanupResources(session));

        return session.send(output)
                .doOnTerminate(() -> log.info("WebSocket connection closed: {}", session.getId()));
    }

    private Flux<WebSocketMessage> handleMessage(WebSocketSession session, WebSocketMessage message) {
        String payload = message.getPayloadAsText();

        return Mono.fromCallable(() -> ValidationUtil.isJson(payload))
                .flatMapMany(isJson -> {
                    if (isJson) {
                        try {
                            CompilerRequest request = mapper.readValue(payload, CompilerRequest.class);
                            return codeExecService.startExec(session, request);
                        } catch (Exception e) {
                            log.error("Error parsing JSON", e);
                            return Flux.just(session.textMessage("Error: Invalid request format"));
                        }
                    } else {
                        return codeExecService.handleExec(session, payload)
                                .thenMany(Flux.empty());
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error handling message", e);
                    return Flux.just(session.textMessage("Error: " + e.getMessage()));
                });
    }

    private void cleanupResources(WebSocketSession session) {
        PtyProcess proc = (PtyProcess) session.getAttributes().remove("ptyProcess");
        String containerId = (String) session.getAttributes().remove("containerId");

        if (proc != null && proc.isAlive()) {
            try {
                proc.getInputStream().close();
                proc.getOutputStream().close();
                proc.destroy();
            } catch (Exception e) {
                log.warn("Failed to destroy PTY process", e);
            }
        }

        if (containerId != null) {
            boolean removed = codeExecService.destroyContainer(containerId);
            if (!removed) {
                log.warn("Container {} could not be destroyed (maybe already removed)", containerId);
            } else {
                log.info("Cleaned up resources for session {}", session.getId());
            }
        }

        session.getAttributes().clear();
    }
}