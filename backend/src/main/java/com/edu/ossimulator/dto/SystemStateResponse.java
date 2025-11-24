package com.edu.ossimulator.dto;

import com.edu.ossimulator.model.ProcessControlBlock;
import com.edu.ossimulator.service.SimulationStatus;

import java.util.List;

public class SystemStateResponse {

    private SimulationStatus status;
    private List<ProcessControlBlock> readyQueue;
    private List<ProcessControlBlock> waitingQueue;
    private List<ProcessControlBlock> terminatedQueue;
    private ProcessControlBlock runningProcess;

    public SystemStateResponse(SimulationStatus status,
                               List<ProcessControlBlock> readyQueue,
                               List<ProcessControlBlock> waitingQueue,
                               List<ProcessControlBlock> terminatedQueue,
                               ProcessControlBlock runningProcess) {
        this.status = status;
        this.readyQueue = readyQueue;
        this.waitingQueue = waitingQueue;
        this.terminatedQueue = terminatedQueue;
        this.runningProcess = runningProcess;
    }

    public SimulationStatus getStatus() {
        return status;
    }

    public List<ProcessControlBlock> getReadyQueue() {
        return readyQueue;
    }

    public List<ProcessControlBlock> getWaitingQueue() {
        return waitingQueue;
    }

    public List<ProcessControlBlock> getTerminatedQueue() {
        return terminatedQueue;
    }

    public ProcessControlBlock getRunningProcess() {
        return runningProcess;
    }
}

