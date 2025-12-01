package com.edu.ossimulator.dto;

import jakarta.validation.constraints.Min;

public class InitializeMemoryRequest {
    @Min(64)
    private int totalSize;

    public InitializeMemoryRequest() {
    }

    public InitializeMemoryRequest(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }
}

