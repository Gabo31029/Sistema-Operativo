package com.edu.ossimulator.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.edu.ossimulator.dto.CreateProcessRequest;
import com.edu.ossimulator.dto.InterruptionRequest;
import com.edu.ossimulator.dto.SimulationRequest;
import com.edu.ossimulator.dto.SystemStateResponse;
import com.edu.ossimulator.dto.TimelineEntry;
import com.edu.ossimulator.model.ProcessControlBlock;
import com.edu.ossimulator.model.ProcessState;
import com.edu.ossimulator.model.SchedulerAlgorithm;
import com.edu.ossimulator.service.MemoryManagerService;

@Service
public class ProcessSchedulerService {

    private final List<ProcessControlBlock> processTable = new ArrayList<>();
    private final Deque<ProcessControlBlock> readyQueue = new ArrayDeque<>();
    private final Deque<ProcessControlBlock> waitingQueue = new ArrayDeque<>();
    private final List<ProcessControlBlock> terminatedQueue = new ArrayList<>();
    private final List<TimelineEntry> timeline = new ArrayList<>();
    private final MemoryManagerService memoryManagerService;

    private SimulationStatus status = SimulationStatus.IDLE;
    private ProcessControlBlock runningProcess;
    private SchedulerAlgorithm lastAlgorithm = SchedulerAlgorithm.FCFS;
    private int lastQuantum = 2;
    private Thread simulationThread;
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);
    private final Object pauseLock = new Object();
    private boolean isPaused = false;
    
    // Configuración para interrupciones I/O automáticas
    private double ioInterruptProbability = 0.3; // 30% de probabilidad de I/O durante ejecución
    private int ioDurationSeconds = 3; // Duración de I/O en segundos
    private boolean autoIOEnabled = true; // Habilitar I/O automático
    
    // Modo de operación: true = automático, false = manual
    private boolean automaticMode = true;

    public ProcessSchedulerService(MemoryManagerService memoryManagerService) {
        this.memoryManagerService = memoryManagerService;
    }

    public synchronized ProcessControlBlock createProcess(CreateProcessRequest request) {
        ProcessControlBlock pcb = new ProcessControlBlock(
                request.getName(),
                request.getArrivalTime(),
                request.getBurstTime(),
                Optional.ofNullable(request.getPriority()).orElse(1)
        );
        pcb.setState(ProcessState.READY);
        
        // Allocate memory if requested
        if (request.getMemorySize() != null && request.getMemorySize() > 0) {
            try {
                Integer memoryAddress = memoryManagerService.allocateMemory(
                        pcb.getPid(),
                        request.getMemorySize(),
                        null // Use current algorithm
                );
                if (memoryAddress != null) {
                    pcb.setMemoryAddress(memoryAddress);
                    pcb.setMemorySize(request.getMemorySize());
                    pcb.appendHistory("Memory allocated at address " + memoryAddress);
                }
            } catch (Exception e) {
                pcb.appendHistory("Memory allocation failed: " + e.getMessage());
            }
        }
        
        processTable.add(pcb);
        readyQueue.add(pcb);
        
        // Auto-start simulation only if in automatic mode
        if (automaticMode && status == SimulationStatus.IDLE && !readyQueue.isEmpty()) {
            // Start with default algorithm (FCFS) or last used algorithm
            // En modo automático, llamar directamente sin validación de modo
            this.status = SimulationStatus.RUNNING;
            this.shouldStop.set(false);
            this.isPaused = false;
            
            List<ProcessControlBlock> workingSet = processTable.stream()
                    .filter(p -> p.getState() != ProcessState.TERMINATED)
                    .sorted(Comparator.comparingInt(ProcessControlBlock::getArrivalTime))
                    .collect(Collectors.toList());

            // Ejecutar la simulación en un hilo separado
            simulationThread = new Thread(() -> {
                try {
                    switch (lastAlgorithm) {
                        case FCFS -> runFcfsReal(workingSet);
                        case ROUND_ROBIN -> runRoundRobinReal(workingSet, this.lastQuantum);
                        case PRIORITY -> runPriorityReal(workingSet);
                        case SJF -> runSjfReal(workingSet);
                    }
                    synchronized (this) {
                        if (status != SimulationStatus.STOPPED) {
                            // Si no hay procesos pero puede haber nuevos, mantener en IDLE para auto-inicio
                            if (processTable.isEmpty() || processTable.stream().allMatch(p -> p.getState() == ProcessState.TERMINATED)) {
                                this.status = SimulationStatus.IDLE;
                            } else {
                                this.status = SimulationStatus.COMPLETED;
                                // Si hay procesos listos, reiniciar automáticamente
                                if (!readyQueue.isEmpty() || !waitingQueue.isEmpty()) {
                                    // Reiniciar con el mismo algoritmo
                                    this.status = SimulationStatus.IDLE;
                                    // Esto se manejará cuando se cree un nuevo proceso
                                }
                            }
                        }
                        runningProcess = null;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    synchronized (this) {
                        this.status = SimulationStatus.STOPPED;
                        runningProcess = null;
                    }
                }
            });
            
            simulationThread.start();
        }
        
        return pcb;
    }

    public synchronized List<ProcessControlBlock> getProcessTable() {
        return new ArrayList<>(processTable);
    }

    public synchronized void clearAllProcesses() {
        // Deallocate memory for all processes before clearing
        for (ProcessControlBlock pcb : processTable) {
            if (pcb.getMemoryAddress() != null && pcb.getMemorySize() != null) {
                try {
                    memoryManagerService.deallocateMemory(pcb.getPid());
                } catch (Exception e) {
                    // Ignore errors during cleanup
                }
            }
        }
        
        processTable.clear();
        readyQueue.clear();
        waitingQueue.clear();
        terminatedQueue.clear();
        timeline.clear();
        runningProcess = null;
        status = SimulationStatus.IDLE;
        if (simulationThread != null && simulationThread.isAlive()) {
            shouldStop.set(true);
            simulationThread.interrupt();
        }
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
        
        // Si hay una simulación en curso, no reiniciarla si está corriendo
        // Solo reiniciar si está pausada o detenida
        if (status == SimulationStatus.PAUSED) {
            resumeSimulation();
            return;
        }
        
        if (status == SimulationStatus.RUNNING) {
            // Si ya está corriendo, no hacer nada (evitar reiniciar)
            return;
        }
        
        this.status = SimulationStatus.RUNNING;
        this.lastAlgorithm = request.getAlgorithm();
        this.lastQuantum = Optional.ofNullable(request.getQuantum()).orElse(lastQuantum);
        this.shouldStop.set(false);
        this.isPaused = false;
        
        // No limpiar las colas si ya hay procesos ejecutándose
        // Solo resetear si no hay nada corriendo
        if (runningProcess == null && timeline.isEmpty()) {
            resetQueues();
        }
        
        // No limpiar timeline si ya hay entradas (continuar desde donde está)
        if (timeline.isEmpty()) {
            timeline.clear();
        }

        List<ProcessControlBlock> workingSet = processTable.stream()
                .filter(p -> p.getState() != ProcessState.TERMINATED)
                .sorted(Comparator.comparingInt(ProcessControlBlock::getArrivalTime))
                .collect(Collectors.toList());

        // Ejecutar la simulación en un hilo separado
        simulationThread = new Thread(() -> {
            try {
                switch (request.getAlgorithm()) {
                    case FCFS -> runFcfsReal(workingSet);
                    case ROUND_ROBIN -> runRoundRobinReal(workingSet, this.lastQuantum);
                    case PRIORITY -> runPriorityReal(workingSet);
                    case SJF -> runSjfReal(workingSet);
                }
                synchronized (this) {
                    if (status != SimulationStatus.STOPPED) {
                        this.status = SimulationStatus.COMPLETED;
                    }
                    runningProcess = null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                synchronized (this) {
                    this.status = SimulationStatus.STOPPED;
                    runningProcess = null;
                }
            }
        });
        
        simulationThread.start();
    }

    public synchronized void pauseSimulation() {
        // No permitir pausar en modo automático
        if (automaticMode) {
            return;
        }
        if (status == SimulationStatus.RUNNING) {
            status = SimulationStatus.PAUSED;
            isPaused = true;
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }
    }

    public synchronized void resumeSimulation() {
        // No permitir reanudar en modo automático
        if (automaticMode) {
            return;
        }
        if (status == SimulationStatus.PAUSED) {
            status = SimulationStatus.RUNNING;
            isPaused = false;
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }
    }

    public synchronized void stopSimulation() {
        // No permitir detener en modo automático
        if (automaticMode) {
            return;
        }
        shouldStop.set(true);
        isPaused = false;
        status = SimulationStatus.STOPPED;
        runningProcess = null;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        if (simulationThread != null && simulationThread.isAlive()) {
            simulationThread.interrupt();
        }
    }
    
    private void waitForPause() throws InterruptedException {
        synchronized (pauseLock) {
            while (isPaused && !shouldStop.get()) {
                pauseLock.wait();
            }
        }
    }
    
    private void sleepWithPause(long milliseconds) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < milliseconds) {
            if (shouldStop.get()) {
                throw new InterruptedException("Simulation stopped");
            }
            waitForPause();
            long remaining = milliseconds - (System.currentTimeMillis() - startTime);
            if (remaining > 0) {
                Thread.sleep(Math.min(remaining, 100)); // Sleep in small chunks to check pause/stop
            }
        }
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
        
        // Programar que el proceso vuelva a READY después de ioDurationSeconds (simulando I/O completado)
        new Thread(() -> {
            try {
                Thread.sleep(ioDurationSeconds * 1000L); // Esperar según duración configurada
                synchronized (this) {
                    if (pcb.getState() == ProcessState.WAITING && !shouldStop.get()) {
                        moveToReady(pcb, "I/O completed");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void moveToReady(ProcessControlBlock pcb, String reason) {
        updateState(pcb, ProcessState.READY, reason);
        readyQueue.remove(pcb);
        readyQueue.add(pcb);
        waitingQueue.remove(pcb);
    }
    
    public synchronized void setIOSettings(double probability, int duration, boolean enabled) {
        this.ioInterruptProbability = Math.max(0.0, Math.min(1.0, probability));
        this.ioDurationSeconds = Math.max(1, Math.min(10, duration));
        this.autoIOEnabled = enabled;
    }
    
    public synchronized void setAutomaticMode(boolean enabled) {
        this.automaticMode = enabled;
        // Si se cambia a modo manual y hay simulación corriendo, detenerla
        if (!enabled && status == SimulationStatus.RUNNING) {
            shouldStop.set(true);
            status = SimulationStatus.STOPPED;
            isPaused = false;
            if (simulationThread != null && simulationThread.isAlive()) {
                simulationThread.interrupt();
            }
        }
    }
    
    public synchronized boolean isAutomaticMode() {
        return automaticMode;
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
        
        // Deallocate memory when process terminates
        if (pcb.getMemoryAddress() != null) {
            try {
                memoryManagerService.deallocateMemory(pcb.getPid());
                pcb.appendHistory("Memory deallocated");
            } catch (Exception e) {
                pcb.appendHistory("Memory deallocation failed: " + e.getMessage());
            }
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
    
    private void runFcfsReal(List<ProcessControlBlock> processes) throws InterruptedException {
        // Usar una cola dinámica que se actualiza con procesos que vuelven de WAITING
        Deque<ProcessControlBlock> queue = new ArrayDeque<>();
        List<ProcessControlBlock> allProcesses = new ArrayList<>(processes);
        allProcesses.forEach(p -> p.resetRuntimeData());
        
        int time = 0;
        int nextArrivalIndex = 0;
        
        while ((!queue.isEmpty() || nextArrivalIndex < allProcesses.size() || 
                !waitingQueue.isEmpty() || !readyQueue.isEmpty()) && !shouldStop.get()) {
            
            // Agregar procesos que han llegado
            while (nextArrivalIndex < allProcesses.size()) {
                ProcessControlBlock p = allProcesses.get(nextArrivalIndex);
                if (p.getArrivalTime() <= time) {
                    synchronized (this) {
                        if (p.getState() != ProcessState.TERMINATED && p.getState() != ProcessState.WAITING) {
                            p.setState(ProcessState.READY);
                            if (!queue.contains(p) && !readyQueue.contains(p)) {
                                queue.offer(p);
                            }
                        }
                    }
                    nextArrivalIndex++;
                } else {
                    break;
                }
            }
            
            // Agregar nuevos procesos creados dinámicamente durante la simulación
            synchronized (this) {
                for (ProcessControlBlock newPcb : processTable) {
                    if (newPcb.getState() == ProcessState.READY && 
                        newPcb.getArrivalTime() <= time &&
                        !allProcesses.contains(newPcb) &&
                        !queue.contains(newPcb) &&
                        newPcb.getRemainingTime() > 0) {
                        allProcesses.add(newPcb);
                        queue.offer(newPcb);
                    }
                }
            }
            
            // Agregar nuevos procesos creados dinámicamente durante la simulación
            synchronized (this) {
                for (ProcessControlBlock newPcb : processTable) {
                    if (newPcb.getState() == ProcessState.READY && 
                        newPcb.getArrivalTime() <= time &&
                        !allProcesses.contains(newPcb) &&
                        !queue.contains(newPcb) &&
                        newPcb.getRemainingTime() > 0) {
                        allProcesses.add(newPcb);
                        queue.offer(newPcb);
                    }
                }
            }
            
            // Agregar procesos que volvieron de WAITING a READY
            synchronized (this) {
                List<ProcessControlBlock> readyFromWaiting = new ArrayList<>(readyQueue);
                for (ProcessControlBlock p : readyFromWaiting) {
                    if (p.getState() == ProcessState.READY && !queue.contains(p) && 
                        p.getArrivalTime() <= time && p.getRemainingTime() > 0) {
                        queue.offer(p);
                    }
                }
            }
            
            // Si no hay procesos listos, esperar hasta el próximo evento (pero nunca detenerse completamente)
            if (queue.isEmpty()) {
                int nextTime = Integer.MAX_VALUE;
                if (nextArrivalIndex < allProcesses.size()) {
                    nextTime = Math.min(nextTime, allProcesses.get(nextArrivalIndex).getArrivalTime());
                }
                // Verificar si hay procesos en WAITING que podrían volver pronto
                synchronized (this) {
                    if (!waitingQueue.isEmpty()) {
                        // Esperar un poco para que los procesos en WAITING puedan volver
                        nextTime = Math.min(nextTime, time + ioDurationSeconds + 1);
                    }
                    // Verificar si hay nuevos procesos en la tabla que aún no están en allProcesses
                    for (ProcessControlBlock newPcb : processTable) {
                        if (newPcb.getState() == ProcessState.READY && 
                            newPcb.getArrivalTime() <= time &&
                            !allProcesses.contains(newPcb)) {
                            // Hay un nuevo proceso, agregarlo inmediatamente
                            allProcesses.add(newPcb);
                            queue.offer(newPcb);
                            nextTime = time; // No esperar
                            break;
                        }
                    }
                }
                if (nextTime == Integer.MAX_VALUE) {
                    // No hay más procesos conocidos, esperar un poco y verificar nuevos procesos
                    sleepWithPause(1000L); // Esperar 1 segundo
                    synchronized (this) {
                        // Verificar si hay nuevos procesos creados
                        for (ProcessControlBlock newPcb : processTable) {
                            if (newPcb.getState() == ProcessState.READY && 
                                !allProcesses.contains(newPcb)) {
                                allProcesses.add(newPcb);
                                queue.offer(newPcb);
                            }
                        }
                    }
                    continue;
                }
                if (nextTime > time) {
                    sleepWithPause((nextTime - time) * 1000L);
                    time = nextTime;
                }
                continue;
            }
            
            ProcessControlBlock pcb = queue.poll();
            
            // Verificar que el proceso esté listo para ejecutarse
            synchronized (this) {
                if (pcb.getState() != ProcessState.READY || pcb.getRemainingTime() <= 0) {
                    continue;
                }
                pcb.setState(ProcessState.RUNNING);
                runningProcess = pcb;
                readyQueue.remove(pcb);
                int start = time;
                timeline.add(new TimelineEntry(pcb.getPid(), pcb.getName(), start, start + pcb.getRemainingTime(), SchedulerAlgorithm.FCFS));
            }
            
            // Esperar el burst time real, actualizando remainingTime cada segundo
            int remaining = pcb.getRemainingTime();
            int executed = 0;
            for (int i = 0; i < remaining && !shouldStop.get(); i++) {
                sleepWithPause(1000L);
                synchronized (this) {
                    // Verificar si el proceso fue movido a WAITING por una interrupción
                    if (pcb.getState() == ProcessState.WAITING || pcb.getState() == ProcessState.TERMINATED) {
                        // El proceso fue interrumpido o terminado, salir del loop
                        break;
                    }
                    if (pcb.getRemainingTime() > 0) {
                        pcb.setRemainingTime(pcb.getRemainingTime() - 1);
                        executed++;
                    }
                }
            }
            
            synchronized (this) {
                // Verificar el estado final del proceso
                if (pcb.getState() == ProcessState.WAITING) {
                    // El proceso fue interrumpido y está en WAITING
                    runningProcess = null;
                    time += executed;
                    // El proceso ya está en waitingQueue, continuar con el siguiente
                    continue;
                } else if (pcb.getRemainingTime() <= 0 || pcb.getState() == ProcessState.TERMINATED) {
                    // Proceso terminado
                    pcb.setState(ProcessState.TERMINATED);
                    pcb.setRemainingTime(0);
                    runningProcess = null;
                    terminatedQueue.add(pcb);
                    time += executed;
                } else {
                    // Esto no debería pasar, pero por seguridad
                    pcb.setState(ProcessState.TERMINATED);
                    pcb.setRemainingTime(0);
                    runningProcess = null;
                    terminatedQueue.add(pcb);
                    time += executed;
                }
            }
        }
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
    
    private void runPriorityReal(List<ProcessControlBlock> processes) throws InterruptedException {
        List<ProcessControlBlock> queue = new ArrayList<>(processes.stream()
                .sorted(Comparator.comparingInt(ProcessControlBlock::getArrivalTime))
                .collect(Collectors.toList()));

        int time = 0;
        while (!queue.isEmpty() && !shouldStop.get()) {
            int currentTime = time;
            List<ProcessControlBlock> available = queue.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime)
                    .collect(Collectors.toList());
            
            if (available.isEmpty()) {
                int nextArrival = queue.get(0).getArrivalTime();
                int waitTime = nextArrival - time;
                if (waitTime > 0) {
                    sleepWithPause(waitTime * 1000L);
                }
                time = nextArrival;
                continue;
            }
            
            ProcessControlBlock next = available.stream()
                    .min(Comparator.comparingInt(ProcessControlBlock::getPriority)
                            .thenComparing(ProcessControlBlock::getArrivalTime))
                    .orElseThrow();

            queue.remove(next);
            
            synchronized (this) {
                next.setState(ProcessState.RUNNING);
                runningProcess = next;
                readyQueue.remove(next);
                int start = time;
                timeline.add(new TimelineEntry(next.getPid(), next.getName(), start, start + next.getBurstTime(), SchedulerAlgorithm.PRIORITY));
            }
            
            // Esperar el burst time real, actualizando remainingTime cada segundo
            int remaining = next.getRemainingTime();
            int executed = 0;
            for (int i = 0; i < remaining && !shouldStop.get(); i++) {
                sleepWithPause(1000L); // Esperar 1 segundo
                synchronized (this) {
                    // Verificar si el proceso fue movido a WAITING por una interrupción
                    if (next.getState() == ProcessState.WAITING || next.getState() == ProcessState.TERMINATED) {
                        // El proceso fue interrumpido o terminado, salir del loop
                        break;
                    }
                    
                    // Interrupción I/O automática (si está habilitada)
                    if (autoIOEnabled && next.getState() == ProcessState.RUNNING && 
                        Math.random() < ioInterruptProbability && executed > 0 && next.getRemainingTime() > 1) {
                        // Generar interrupción I/O automática
                        moveToWaiting(next, "Auto I/O interrupt (simulated)");
                        break;
                    }
                    
                    if (next.getRemainingTime() > 0) {
                        next.setRemainingTime(next.getRemainingTime() - 1);
                        executed++;
                    }
                }
            }
            
            synchronized (this) {
                // Verificar el estado final del proceso
                if (next.getState() == ProcessState.WAITING) {
                    // El proceso fue interrumpido y está en WAITING
                    runningProcess = null;
                    time += executed;
                    // El proceso ya está en waitingQueue, continuar con el siguiente
                    continue;
                } else {
                    // Proceso terminado
                    next.setState(ProcessState.TERMINATED);
                    next.setRemainingTime(0);
                    runningProcess = null;
                    terminatedQueue.add(next);
                    time += executed;
                }
            }
        }
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
    
    private void runSjfReal(List<ProcessControlBlock> processes) throws InterruptedException {
        List<ProcessControlBlock> queue = new ArrayList<>(processes.stream()
                .sorted(Comparator.comparingInt(ProcessControlBlock::getArrivalTime))
                .collect(Collectors.toList()));

        int time = 0;
        while (!queue.isEmpty() && !shouldStop.get()) {
            int currentTime = time;
            List<ProcessControlBlock> available = queue.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime)
                    .collect(Collectors.toList());
            
            if (available.isEmpty()) {
                int nextArrival = queue.get(0).getArrivalTime();
                int waitTime = nextArrival - time;
                if (waitTime > 0) {
                    sleepWithPause(waitTime * 1000L);
                }
                time = nextArrival;
                continue;
            }
            
            ProcessControlBlock next = available.stream()
                    .min(Comparator.comparingInt(ProcessControlBlock::getBurstTime)
                            .thenComparing(ProcessControlBlock::getArrivalTime))
                    .orElseThrow();

            queue.remove(next);
            
            synchronized (this) {
                next.setState(ProcessState.RUNNING);
                runningProcess = next;
                readyQueue.remove(next);
                int start = time;
                timeline.add(new TimelineEntry(next.getPid(), next.getName(), start, start + next.getBurstTime(), SchedulerAlgorithm.SJF));
            }
            
            // Esperar el burst time real, actualizando remainingTime cada segundo
            int remaining = next.getRemainingTime();
            int executed = 0;
            for (int i = 0; i < remaining && !shouldStop.get(); i++) {
                sleepWithPause(1000L); // Esperar 1 segundo
                synchronized (this) {
                    // Verificar si el proceso fue movido a WAITING por una interrupción
                    if (next.getState() == ProcessState.WAITING || next.getState() == ProcessState.TERMINATED) {
                        // El proceso fue interrumpido o terminado, salir del loop
                        break;
                    }
                    
                    // Interrupción I/O automática (si está habilitada)
                    if (autoIOEnabled && next.getState() == ProcessState.RUNNING && 
                        Math.random() < ioInterruptProbability && executed > 0 && next.getRemainingTime() > 1) {
                        // Generar interrupción I/O automática
                        moveToWaiting(next, "Auto I/O interrupt (simulated)");
                        break;
                    }
                    
                    if (next.getRemainingTime() > 0) {
                        next.setRemainingTime(next.getRemainingTime() - 1);
                        executed++;
                    }
                }
            }
            
            synchronized (this) {
                // Verificar el estado final del proceso
                if (next.getState() == ProcessState.WAITING) {
                    // El proceso fue interrumpido y está en WAITING
                    runningProcess = null;
                    time += executed;
                    // El proceso ya está en waitingQueue, continuar con el siguiente
                    continue;
                } else {
                    // Proceso terminado
                    next.setState(ProcessState.TERMINATED);
                    next.setRemainingTime(0);
                    runningProcess = null;
                    terminatedQueue.add(next);
                    time += executed;
                }
            }
        }
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
    
    private void runRoundRobinReal(List<ProcessControlBlock> processes, int quantum) throws InterruptedException {
        Deque<ProcessControlBlock> queue = new ArrayDeque<>();
        List<ProcessControlBlock> sorted = processes.stream()
                .sorted(Comparator.comparingInt(ProcessControlBlock::getArrivalTime))
                .collect(Collectors.toList());

        int time = 0;
        int index = 0;
        while ((!queue.isEmpty() || index < sorted.size() || !waitingQueue.isEmpty() || !readyQueue.isEmpty()) && !shouldStop.get()) {
            // Agregar procesos que han llegado
            while (index < sorted.size() && sorted.get(index).getArrivalTime() <= time) {
                ProcessControlBlock pcb = sorted.get(index);
                synchronized (this) {
                    if (pcb.getState() != ProcessState.TERMINATED && pcb.getState() != ProcessState.WAITING) {
                        pcb.setState(ProcessState.READY);
                        if (!queue.contains(pcb)) {
                            queue.offer(pcb);
                        }
                    }
                }
                index++;
            }
            
            // Agregar nuevos procesos creados dinámicamente durante la simulación
            synchronized (this) {
                for (ProcessControlBlock newPcb : processTable) {
                    if (newPcb.getState() == ProcessState.READY && 
                        newPcb.getArrivalTime() <= time &&
                        !sorted.contains(newPcb) &&
                        !queue.contains(newPcb) &&
                        newPcb.getRemainingTime() > 0) {
                        sorted.add(newPcb);
                        sorted.sort(Comparator.comparingInt(ProcessControlBlock::getArrivalTime));
                        queue.offer(newPcb);
                    }
                }
            }
            
            // Agregar procesos que volvieron de WAITING a READY
            synchronized (this) {
                List<ProcessControlBlock> readyFromWaiting = new ArrayList<>(readyQueue);
                for (ProcessControlBlock p : readyFromWaiting) {
                    if (p.getState() == ProcessState.READY && !queue.contains(p) && 
                        p.getArrivalTime() <= time && p.getRemainingTime() > 0) {
                        queue.offer(p);
                    }
                }
            }
            
            if (queue.isEmpty()) {
                int nextTime = Integer.MAX_VALUE;
                if (index < sorted.size()) {
                    nextTime = Math.min(nextTime, sorted.get(index).getArrivalTime());
                }
                synchronized (this) {
                    if (!waitingQueue.isEmpty()) {
                        // Esperar hasta 6 segundos para que los procesos en WAITING puedan volver
                        nextTime = Math.min(nextTime, time + 6);
                    }
                }
                if (nextTime == Integer.MAX_VALUE) break;
                if (nextTime > time) {
                    sleepWithPause((nextTime - time) * 1000L);
                    time = nextTime;
                }
                continue;
            }
            
            ProcessControlBlock pcb = queue.poll();
            int start = time;
            int slice = Math.min(quantum, pcb.getRemainingTime());
            
            synchronized (this) {
                pcb.setState(ProcessState.RUNNING);
                runningProcess = pcb;
                readyQueue.remove(pcb);
                timeline.add(new TimelineEntry(pcb.getPid(), pcb.getName(), start, start + slice, SchedulerAlgorithm.ROUND_ROBIN));
            }
            
            // Esperar el tiempo del quantum, actualizando remainingTime cada segundo
            int executedTime = 0;
            for (int i = 0; i < slice && !shouldStop.get(); i++) {
                sleepWithPause(1000L); // Esperar 1 segundo
                synchronized (this) {
                    // Verificar si el proceso fue movido a WAITING o TERMINATED por una interrupción
                    if (pcb.getState() == ProcessState.WAITING || pcb.getState() == ProcessState.TERMINATED) {
                        // El proceso fue interrumpido o terminado, salir del loop
                        break;
                    }
                    if (pcb.getRemainingTime() > 0) {
                        pcb.setRemainingTime(pcb.getRemainingTime() - 1);
                        executedTime++;
                    }
                }
            }
            
            synchronized (this) {
                time += executedTime;
                
                // Verificar el estado final del proceso
                if (pcb.getState() == ProcessState.WAITING) {
                    // El proceso fue interrumpido y está en WAITING
                    runningProcess = null;
                    // El proceso ya está en waitingQueue, continuar con el siguiente
                } else if (pcb.getRemainingTime() > 0) {
                    // El proceso aún tiene tiempo restante, volver a READY
                    pcb.setState(ProcessState.READY);
                    runningProcess = null;
                    // Agregar procesos que llegaron durante la ejecución
                    while (index < sorted.size() && sorted.get(index).getArrivalTime() <= time) {
                        ProcessControlBlock newPcb = sorted.get(index);
                        synchronized (this) {
                            if (newPcb.getState() != ProcessState.TERMINATED && newPcb.getState() != ProcessState.WAITING) {
                                newPcb.setState(ProcessState.READY);
                                if (!queue.contains(newPcb)) {
                                    queue.offer(newPcb);
                                }
                            }
                        }
                        index++;
                    }
                    if (!queue.contains(pcb)) {
                        queue.offer(pcb);
                    }
                } else {
                    // Proceso terminado
                    pcb.setState(ProcessState.TERMINATED);
                    pcb.setRemainingTime(0);
                    runningProcess = null;
                    terminatedQueue.add(pcb);
                }
            }
        }
    }
}

