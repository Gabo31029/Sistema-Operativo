package com.edu.ossimulator.dto;

import com.edu.ossimulator.model.MemoryAllocationAlgorithm;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class MemoryAllocationRequest {
    @NotNull
    private Long processId;

    @Min(1)
    private int size;

    private MemoryAllocationAlgorithm algorithm;

    public MemoryAllocationRequest() {
    }

    public MemoryAllocationRequest(Long processId, int size, MemoryAllocationAlgorithm algorithm) {
        this.processId = processId;
        this.size = size;
        this.algorithm = algorithm;
    }

    public Long getProcessId() {
        return processId;
    }

    public void setProcessId(Long processId) {
        this.processId = processId;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public MemoryAllocationAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(MemoryAllocationAlgorithm algorithm) {
        this.algorithm = algorithm;
    }
}

