package com.edu.ossimulator.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class IOSettingsRequest {
    @Min(0)
    @Max(100)
    private double ioProbability; // 0.0 a 1.0 (0% a 100%)
    
    @Min(1)
    @Max(10)
    private int ioDurationSeconds;
    
    private boolean autoIOEnabled;

    public double getIoProbability() {
        return ioProbability;
    }

    public void setIoProbability(double ioProbability) {
        this.ioProbability = ioProbability;
    }

    public int getIoDurationSeconds() {
        return ioDurationSeconds;
    }

    public void setIoDurationSeconds(int ioDurationSeconds) {
        this.ioDurationSeconds = ioDurationSeconds;
    }

    public boolean isAutoIOEnabled() {
        return autoIOEnabled;
    }

    public void setAutoIOEnabled(boolean autoIOEnabled) {
        this.autoIOEnabled = autoIOEnabled;
    }
}

