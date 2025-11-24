package com.edu.ossimulator.controller;

import com.edu.ossimulator.dto.InterruptionRequest;
import com.edu.ossimulator.service.ProcessSchedulerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interruptions")
public class InterruptionController {

    private final ProcessSchedulerService schedulerService;

    public InterruptionController(ProcessSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void emit(@Valid @RequestBody InterruptionRequest request) {
        schedulerService.emitInterruption(request);
    }
}

