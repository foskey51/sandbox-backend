package com.example.sandbox_backend.controller;

import com.example.sandbox_backend.Service.NoVncExecService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URL;

@RestController
@RequestMapping("/api/v1/no-vnc")
public class NoVncController {

    private final NoVncExecService noVncExecService;

    public NoVncController(NoVncExecService noVncExecService) {
        this.noVncExecService = noVncExecService;
    }

    @GetMapping("")
    public Flux<String> getProxyUrl() {
        return noVncExecService.startExec()
                .flatMap(url -> {
                    if (url == null) {
                        return Mono.error(new IllegalStateException("No URL returned from service"));
                    }
                    return Mono.just(url);
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty response from service")));
    }

}
