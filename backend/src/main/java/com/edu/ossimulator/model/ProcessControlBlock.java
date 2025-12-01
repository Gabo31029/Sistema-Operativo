package com.edu.ossimulator.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ProcessControlBlock {

    private static final AtomicLong PID_SEQUENCE = new AtomicLong(1);

    private final long pid;
    private String name;
    private ProcessState state;
    private int arrivalTime;
    private int burstTime;
    private int remainingTime;
    private int priority;
    private Instant createdAt;
    private final List<String> history = new ArrayList<>();
    private Integer memoryAddress;
    private Integer memorySize;
    // Desglose de memoria del proceso (en KB)
    private Integer executableSize;
    private Integer dataSize;
    private Integer variableSize;

    public ProcessControlBlock(String name, int arrivalTime, int burstTime, int priority) {
        this.pid = PID_SEQUENCE.getAndIncrement();
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = priority;
        this.state = ProcessState.NEW;
        this.createdAt = Instant.now();
        appendHistory("Process created");
    }

    public long getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
        appendHistory("State changed to " + state);
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public void setBurstTime(int burstTime) {
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(int remainingTime) {
        this.remainingTime = Math.max(remainingTime, 0);
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getHistory() {
        return history;
    }

    public void appendHistory(String event) {
        history.add(String.format("[%s] %s", Instant.now(), event));
    }

    public void resetRuntimeData() {
        this.remainingTime = this.burstTime;
        this.state = ProcessState.READY;
    }

    public Integer getMemoryAddress() {
        return memoryAddress;
    }

    public void setMemoryAddress(Integer memoryAddress) {
        this.memoryAddress = memoryAddress;
    }

    public Integer getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Integer memorySize) {
        this.memorySize = memorySize;
    }

    public Integer getExecutableSize() {
        return executableSize;
    }

    public void setExecutableSize(Integer executableSize) {
        this.executableSize = executableSize;
    }

    public Integer getDataSize() {
        return dataSize;
    }

    public void setDataSize(Integer dataSize) {
        this.dataSize = dataSize;
    }

    public Integer getVariableSize() {
        return variableSize;
    }

    public void setVariableSize(Integer variableSize) {
        this.variableSize = variableSize;
    }
}

