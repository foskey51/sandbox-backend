package org.example.sandbox_backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pty4j.PtyProcess;
import org.example.sandbox_backend.request.CompilerRequestDTO;
import org.example.sandbox_backend.service.CodeExecService;
import org.example.sandbox_backend.service.JwtService;
import org.example.sandbox_backend.utils.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class WebSocketController extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class.getName());
    public final ObjectMapper mapper = new ObjectMapper();
    public CodeExecService codeExecService;

    @Autowired
    public WebSocketController(CodeExecService codeExecService) {
        this.codeExecService = codeExecService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established");
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            if (ValidationUtil.isJson(message.getPayload())) {
                CompilerRequestDTO request = mapper.readValue(message.getPayload(), CompilerRequestDTO.class);
                //log.info("Received message: lang: " + request.language() + "\n code:" + request.sourceCode());
                codeExecService.startExec(session, request);
            }else{
                codeExecService.handleExec(session, message);
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            closeSocket(session, CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        PtyProcess proc = (PtyProcess) session.getAttributes().remove("ptyProcess");
        String containerId = (String) session.getAttributes().remove("containerId");
        Thread readerThread = (Thread) session.getAttributes().remove("readerThread");

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

        session.getAttributes().remove("ptyProcess");
        session.getAttributes().remove("containerId");
        session.getAttributes().remove("readerThread");
    }



    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        exception.printStackTrace();
    }

    public void closeSocket(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception e) {
            log.error("Error closing socket: " + e.getMessage());
        }
    }
}

