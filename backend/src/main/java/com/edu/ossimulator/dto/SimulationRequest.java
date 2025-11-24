package com.edu.ossimulator.dto;

import com.edu.ossimulator.model.SchedulerAlgorithm;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SimulationRequest {

    @NotNull
    private SchedulerAlgorithm algorithm;

    @Min(1)
    private Integer quantum;

    public SchedulerAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(SchedulerAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public Integer getQuantum() {
        return quantum;
    }

    public void setQuantum(Integer quantum) {
        this.quantum = quantum;
    }
}

