package com.edu.ossimulator.dto;

import com.edu.ossimulator.model.MemoryAllocationAlgorithm;

public class AlgorithmRequest {
    private MemoryAllocationAlgorithm algorithm;

    public AlgorithmRequest() {
    }

    public AlgorithmRequest(MemoryAllocationAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public MemoryAllocationAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(MemoryAllocationAlgorithm algorithm) {
        this.algorithm = algorithm;
    }
}

