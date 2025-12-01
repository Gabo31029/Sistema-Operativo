package com.edu.ossimulator.controller;

import com.edu.ossimulator.dto.SimulationRequest;
import com.edu.ossimulator.dto.SystemStateResponse;
import com.edu.ossimulator.dto.TimelineEntry;
import com.edu.ossimulator.service.ProcessSchedulerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final ProcessSchedulerService schedulerService;

    public SimulationController(ProcessSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void start(@Valid @RequestBody SimulationRequest request) {
        schedulerService.startSimulation(request);
    }

    @PostMapping("/pause")
    public void pause() {
        schedulerService.pauseSimulation();
    }

    @PostMapping("/resume")
    public void resume() {
        schedulerService.resumeSimulation();
    }

    @PostMapping("/stop")
    public void stop() {
        schedulerService.stopSimulation();
    }

    @GetMapping("/state")
    public SystemStateResponse getState() {
        return schedulerService.getSystemState();
    }

    @GetMapping("/timeline")
    public List<TimelineEntry> getTimeline() {
        return schedulerService.getTimeline();
    }

    @PostMapping("/io-settings")
    @ResponseStatus(HttpStatus.OK)
    public void setIOSettings(@Valid @RequestBody com.edu.ossimulator.dto.IOSettingsRequest request) {
        schedulerService.setIOSettings(request.getIoProbability(), request.getIoDurationSeconds(), request.isAutoIOEnabled());
    }

    @PostMapping("/mode")
    @ResponseStatus(HttpStatus.OK)
    public void setMode(@RequestBody java.util.Map<String, Boolean> request) {
        Boolean automatic = request.get("automatic");
        if (automatic != null) {
            schedulerService.setAutomaticMode(automatic);
        }
    }

    @GetMapping("/mode")
    public java.util.Map<String, Boolean> getMode() {
        return java.util.Map.of("automatic", schedulerService.isAutomaticMode());
    }
}

