package com.edu.ossimulator.dto;

import com.edu.ossimulator.model.InterruptionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class InterruptionRequest {

    @Min(1)
    private long pid;

    @NotNull
    private InterruptionType type;

    @NotBlank
    private String reason;

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public InterruptionType getType() {
        return type;
    }

    public void setType(InterruptionType type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

