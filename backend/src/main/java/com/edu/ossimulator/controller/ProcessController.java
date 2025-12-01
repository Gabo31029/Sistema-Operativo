package com.edu.ossimulator.controller;

import com.edu.ossimulator.dto.CreateProcessRequest;
import com.edu.ossimulator.model.ProcessControlBlock;
import com.edu.ossimulator.service.ProcessSchedulerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/processes")
public class ProcessController {

    private final ProcessSchedulerService schedulerService;

    public ProcessController(ProcessSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProcessControlBlock createProcess(@Valid @RequestBody CreateProcessRequest request) {
        return schedulerService.createProcess(request);
    }

    @GetMapping
    public List<ProcessControlBlock> listProcesses() {
        return schedulerService.getProcessTable();
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAllProcesses() {
        schedulerService.clearAllProcesses();
    }
}

