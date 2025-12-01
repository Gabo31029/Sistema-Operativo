package com.edu.ossimulator.controller;

import com.edu.ossimulator.dto.AlgorithmRequest;
import com.edu.ossimulator.dto.InitializeMemoryRequest;
import com.edu.ossimulator.dto.MemoryAllocationRequest;
import com.edu.ossimulator.dto.MemoryStateResponse;
import com.edu.ossimulator.service.MemoryManagerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memory")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class MemoryController {

    private final MemoryManagerService memoryManagerService;

    public MemoryController(MemoryManagerService memoryManagerService) {
        this.memoryManagerService = memoryManagerService;
    }

    @PostMapping("/initialize")
    public ResponseEntity<Void> initializeMemory(@Valid @RequestBody InitializeMemoryRequest request) {
        memoryManagerService.initializeMemory(request.getTotalSize());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/allocate")
    public ResponseEntity<Integer> allocateMemory(@Valid @RequestBody MemoryAllocationRequest request) {
        Integer address = memoryManagerService.allocateMemory(
                request.getProcessId(),
                request.getSize(),
                request.getAlgorithm()
        );
        if (address != null) {
            return ResponseEntity.ok(address);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/deallocate/{processId}")
    public ResponseEntity<Void> deallocateMemory(@PathVariable Long processId) {
        memoryManagerService.deallocateMemory(processId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/state")
    public ResponseEntity<MemoryStateResponse> getMemoryState() {
        return ResponseEntity.ok(memoryManagerService.getMemoryState());
    }

    @PutMapping("/algorithm")
    public ResponseEntity<Void> setAlgorithm(@RequestBody AlgorithmRequest request) {
        memoryManagerService.setAlgorithm(request.getAlgorithm());
        return ResponseEntity.ok().build();
    }
}

