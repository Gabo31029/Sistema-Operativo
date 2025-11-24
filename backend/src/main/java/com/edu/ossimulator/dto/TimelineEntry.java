package com.edu.ossimulator.dto;

import com.edu.ossimulator.model.SchedulerAlgorithm;

public class TimelineEntry {
    private long pid;
    private String name;
    private int start;
    private int end;
    private SchedulerAlgorithm algorithm;

    public TimelineEntry(long pid, String name, int start, int end, SchedulerAlgorithm algorithm) {
        this.pid = pid;
        this.name = name;
        this.start = start;
        this.end = end;
        this.algorithm = algorithm;
    }

    public long getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public SchedulerAlgorithm getAlgorithm() {
        return algorithm;
    }
}

