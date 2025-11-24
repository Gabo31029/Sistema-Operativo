package com.edu.ossimulator.service;

import com.edu.ossimulator.dto.CreateProcessRequest;
import com.edu.ossimulator.dto.InterruptionRequest;
import com.edu.ossimulator.dto.SimulationRequest;
import com.edu.ossimulator.dto.SystemStateResponse;
import com.edu.ossimulator.dto.TimelineEntry;
import com.edu.ossimulator.model.ProcessControlBlock;
import com.edu.ossimulator.model.ProcessState;
import com.edu.ossimulator.model.SchedulerAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProcessSchedulerService {

    private final List<ProcessControlBlock> processTable = new ArrayList<>();
    private final Deque<ProcessControlBlock> readyQueue = new ArrayDeque<>();
    private final Deque<ProcessControlBlock> waitingQueue = new ArrayDeque<>();
    private final List<ProcessControlBlock> terminatedQueue = new ArrayList<>();
    private final List<TimelineEntry> timeline = new ArrayList<>();

    private SimulationStatus status = SimulationStatus.IDLE;
    private ProcessControlBlock runningProcess;
    private SchedulerAlgorithm lastAlgorithm = SchedulerAlgorithm.FCFS;
    private int lastQuantum = 2;

    public synchronized ProcessControlBlock createProcess(CreateProcessRequest request) {
        ProcessControlBlock pcb = new ProcessControlBlock(
                request.getName(),
                request.getArrivalTime(),
                request.getBurstTime(),
                Optional.ofNullable(request.getPriority()).orElse(1)
        );
        pcb.setState(ProcessState.READY);
        processTable.add(pcb);
        readyQueue.add(pcb);
        return pcb;
    }

    public synchronized List<ProcessControlBlock> getProcessTable() {
        return new ArrayList<>(processTable);
    }

    public synchronized SystemStateResponse getSystemState() {
        return new SystemStateResponse(
                status,
                new ArrayList<>(readyQueue),
                new ArrayList<>(waitingQueue),
                new ArrayList<>(terminatedQueue),
                runningProcess
        );
    }

    public synchronized List<TimelineEntry> getTimeline() {
        return new ArrayList<>(timeline);
    }

    public synchronized void startSimulation(SimulationRequest request) {
        Assert.notNull(request.getAlgorithm(), "Algorithm is required");
        this.status = SimulationStatus.RUNNING;
        this.lastAlgorithm = request.getAlgorithm();
        this.lastQuantum = Optional.ofNullable(request.getQuantum()).orElse(lastQuantum);
        resetQueues();

        List<ProcessControlBlock> workingSet = processTable.stream()
                .sorted(Comparator.comparingInt(ProcessControlBlock::getArrivalTime))
                .collect(Collectors.toList());

        List<TimelineEntry> newTimeline = switch (request.getAlgorithm()) {
            case FCFS -> runFcfs(workingSet);
            case ROUND_ROBIN -> runRoundRobin(workingSet, this.lastQuantum);
            case PRIORITY -> runPriority(workingSet);
            case SJF -> runSjf(workingSet);
        };

        timeline.clear();
        timeline.addAll(newTimeline);
        rebuildQueuesFromProcesses();
        this.status = SimulationStatus.COMPLETED;
    }

    public synchronized void pauseSimulation() {
        if (status == SimulationStatus.RUNNING) {
            status = SimulationStatus.PAUSED;
        }
    }

    public synchronized void resumeSimulation() {
        if (status == SimulationStatus.PAUSED) {
            status = SimulationStatus.RUNNING;
        }
    }

    public synchronized void stopSimulation() {
        status = SimulationStatus.STOPPED;
        runningProcess = null;
        readyQueue.clear();
        waitingQueue.clear();
    }

    public synchronized void emitInterruption(InterruptionRequest request) {
        ProcessControlBlock pcb = findProcess(request.getPid());
        switch (request.getType()) {
            case IO -> moveToWaiting(pcb, "I/O interrupt: " + request.getReason());
            case QUANTUM_EXPIRED -> moveToReady(pcb, "Quantum expired: " + request.getReason());
            case MANUAL_STOP -> moveToTerminated(pcb, "Manual termination: " + request.getReason());
            case MANUAL_PAUSE -> pauseTarget(pcb, request.getReason());
        }
    }

    private ProcessControlBlock findProcess(long pid) {
        return processTable.stream()
                .filter(p -> p.getPid() == pid)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("PID not found: " + pid));
    }

    private void moveToWaiting(ProcessControlBlock pcb, String reason) {
        updateState(pcb, ProcessState.WAITING, reason);
        waitingQueue.remove(pcb);
        waitingQueue.add(pcb);
        readyQueue.remove(pcb);
        if (runningProcess == pcb) {
            runningProcess = null;
        }
    }

    private void moveToReady(ProcessControlBlock pcb, String reason) {
        updateState(pcb, ProcessState.READY, reason);
        readyQueue.remove(pcb);
        readyQueue.add(pcb);
        waitingQueue.remove(pcb);
    }

    private void moveToTerminated(ProcessControlBlock pcb, String reason) {
        updateState(pcb, ProcessState.TERMINATED, reason);
        terminatedQueue.remove(pcb);
        terminatedQueue.add(pcb);
        waitingQueue.remove(pcb);
        readyQueue.remove(pcb);
        if (runningProcess == pcb) {
            runningProcess = null;
        }
    }

    private void pauseTarget(ProcessControlBlock pcb, String reason) {
        updateState(pcb, ProcessState.WAITING, "Manual pause: " + reason);
        waitingQueue.add(pcb);
        readyQueue.remove(pcb);
        runningProcess = null;
        status = SimulationStatus.PAUSED;
    }

    private void updateState(ProcessControlBlock pcb, ProcessState state, String reason) {
        pcb.setState(state);
        pcb.appendHistory(reason);
    }

    private void resetQueues() {
        readyQueue.clear();
        waitingQueue.clear();
        terminatedQueue.clear();
        runningProcess = null;
        processTable.forEach(ProcessControlBlock::resetRuntimeData);
        readyQueue.addAll(processTable);
    }

    private void rebuildQueuesFromProcesses() {
        readyQueue.clear();
        waitingQueue.clear();
        terminatedQueue.clear();
        for (ProcessControlBlock pcb : processTable) {
            switch (pcb.getState()) {
                case TERMINATED -> terminatedQueue.add(pcb);
                case WAITING -> waitingQueue.add(pcb);
                case READY, NEW, RUNNING -> readyQueue.add(pcb);
            }
        }
    }

    private List<TimelineEntry> runFcfs(List<ProcessControlBlock> processes) {
        List<ProcessControlBlock> ordered = processes.stream()
                .sorted(Comparator.comparingInt(ProcessControlBlock::getArrivalTime)
                        .thenComparingLong(ProcessControlBlock::getPid))
                .collect(Collectors.toList());

        List<TimelineEntry> entries = new ArrayList<>();
        int time = 0;
        for (ProcessControlBlock pcb : ordered) {
            time = Math.max(time, pcb.getArrivalTime());
            int start = time;
            int end = start + pcb.getBurstTime();
            pcb.setState(ProcessState.RUNNING);
            entries.add(new TimelineEntry(pcb.getPid(), pcb.getName(), start, end, SchedulerAlgorithm.FCFS));
            pcb.setState(ProcessState.TERMINATED);
            pcb.setRemainingTime(0);
            time = end;
        }
        return entries;
    }

    private List<TimelineEntry> runPriority(List<ProcessControlBlock> processes) {
        List<TimelineEntry> entries = new ArrayList<>();
        List<ProcessControlBlock> queue = processes.stream()
                .sorted(Comparator.comparingInt(ProcessControlBlock::getArrivalTime))
                .collect(Collectors.toList());

        int time = 0;
        while (!queue.isEmpty()) {
            int currentTime = time;
            List<ProcessControlBlock> available = queue.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime)
                    .collect(Collectors.toList());
            if (available.isEmpty()) {
                time = queue.get(0).getArrivalTime();
                continue;
            }
            ProcessControlBlock next = available.stream()
                    .min(Comparator.comparingInt(ProcessControlBlock::getPriority)
                            .thenComparing(ProcessControlBlock::getArrivalTime))
                    .orElseThrow();

            queue.remove(next);
            int start = time;
            int end = start + next.getBurstTime();
            entries.add(new TimelineEntry(next.getPid(), next.getName(), start, end, SchedulerAlgorithm.PRIORITY));
            next.setState(ProcessState.TERMINATED);
            next.setRemainingTime(0);
            time = end;
        }
        return entries;
    }

    private List<TimelineEntry> runSjf(List<ProcessControlBlock> processes) {
        List<TimelineEntry> entries = new ArrayList<>();
        List<ProcessControlBlock> queue = processes.stream()
                .sorted(Comparator.comparingInt(ProcessControlBlock::getArrivalTime))
                .collect(Collectors.toList());

        int time = 0;
        while (!queue.isEmpty()) {
            int currentTime = time;
            List<ProcessControlBlock> available = queue.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime)
                    .collect(Collectors.toList());
            if (available.isEmpty()) {
                time = queue.get(0).getArrivalTime();
                continue;
            }
            ProcessControlBlock next = available.stream()
                    .min(Comparator.comparingInt(ProcessControlBlock::getBurstTime)
                            .thenComparing(ProcessControlBlock::getArrivalTime))
                    .orElseThrow();

            queue.remove(next);
            int start = time;
            int end = start + next.getBurstTime();
            entries.add(new TimelineEntry(next.getPid(), next.getName(), start, end, SchedulerAlgorithm.SJF));
            next.setState(ProcessState.TERMINATED);
            next.setRemainingTime(0);
            time = end;
        }
        return entries;
    }

    private List<TimelineEntry> runRoundRobin(List<ProcessControlBlock> processes, int quantum) {
        List<TimelineEntry> entries = new ArrayList<>();
        Deque<ProcessControlBlock> queue = new ArrayDeque<>();
        List<ProcessControlBlock> sorted = processes.stream()
                .sorted(Comparator.comparingInt(ProcessControlBlock::getArrivalTime))
                .collect(Collectors.toList());

        int time = 0;
        int index = 0;
        while (!queue.isEmpty() || index < sorted.size()) {
            while (index < sorted.size() && sorted.get(index).getArrivalTime() <= time) {
                queue.offer(sorted.get(index));
                index++;
            }
            if (queue.isEmpty()) {
                time = sorted.get(index).getArrivalTime();
                continue;
            }
            ProcessControlBlock pcb = queue.poll();
            int start = time;
            int slice = Math.min(quantum, pcb.getRemainingTime());
            time += slice;
            pcb.setRemainingTime(pcb.getRemainingTime() - slice);
            entries.add(new TimelineEntry(pcb.getPid(), pcb.getName(), start, time, SchedulerAlgorithm.ROUND_ROBIN));
            if (pcb.getRemainingTime() > 0) {
                pcb.setState(ProcessState.READY);
                while (index < sorted.size() && sorted.get(index).getArrivalTime() <= time) {
                    queue.offer(sorted.get(index));
                    index++;
                }
                queue.offer(pcb);
            } else {
                pcb.setState(ProcessState.TERMINATED);
            }
        }
        return entries;
    }
}

