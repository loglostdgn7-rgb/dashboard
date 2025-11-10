package com.dashboard.controller;

import com.dashboard.service.SseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SseController {
    @Autowired
    private SseService sseService;

    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe() {
        return sseService.addEmitter();
    }
}
