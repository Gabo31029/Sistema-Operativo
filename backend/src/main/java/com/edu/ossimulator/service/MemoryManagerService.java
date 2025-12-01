package com.edu.ossimulator.service;

import com.edu.ossimulator.dto.MemoryStateResponse;
import com.edu.ossimulator.model.MemoryAllocationAlgorithm;
import com.edu.ossimulator.model.MemoryBlock;
import com.edu.ossimulator.model.MemorySegment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class MemoryManagerService {

    // Tamaño fijo de bloque en KB (32KB) para cumplir el requerimiento de bloques 2^n
    private static final int BLOCK_SIZE_KB = 32;

    private final List<MemoryBlock> memoryBlocks = new ArrayList<>();
    private final List<MemorySegment> segments = new ArrayList<>();
    private int totalMemorySize = 1024; // Default 1024 KB
    private MemoryAllocationAlgorithm currentAlgorithm = MemoryAllocationAlgorithm.FIRST_FIT;
    private final AtomicLong blockIdSequence = new AtomicLong(1);
    private final AtomicLong segmentIdSequence = new AtomicLong(1);
    private boolean initialized = false;

    public synchronized void initializeMemory(int totalSize) {
        memoryBlocks.clear();
        segments.clear();

        // Ajustar el tamaño total a múltiplo de BLOCK_SIZE_KB (32KB)
        int blocks = totalSize / BLOCK_SIZE_KB;
        if (blocks <= 0) {
            throw new IllegalArgumentException("Total memory size must be at least " + BLOCK_SIZE_KB + " KB");
        }
        this.totalMemorySize = blocks * BLOCK_SIZE_KB;

        MemoryBlock initialBlock = new MemoryBlock(
                blockIdSequence.getAndIncrement(),
                0,
                this.totalMemorySize,
                false,
                null
        );
        memoryBlocks.add(initialBlock);
        initialized = true;
    }

    public synchronized Integer allocateMemory(long processId, int size, MemoryAllocationAlgorithm algorithm) {
        if (!initialized) {
            initializeMemory(totalMemorySize);
        }

        if (size <= 0) {
            throw new IllegalArgumentException("Requested size must be greater than 0");
        }

        // Tamaño lógico solicitado por el proceso (en KB)
        int requestedSize = size;

        // Redondear al tamaño de bloque fijo (32KB)
        int requiredBlocks = (int) Math.ceil(requestedSize / (double) BLOCK_SIZE_KB);
        int roundedSize = requiredBlocks * BLOCK_SIZE_KB;

        if (roundedSize > totalMemorySize) {
            throw new IllegalArgumentException("Requested size exceeds total memory");
        }

        MemoryAllocationAlgorithm algo = algorithm != null ? algorithm : currentAlgorithm;

        Integer address = switch (algo) {
            case FIRST_FIT -> allocateFirstFit(roundedSize);
            case BEST_FIT -> allocateBestFit(roundedSize);
            case WORST_FIT -> allocateWorstFit(roundedSize);
            case SEGMENTATION -> allocateSegmentation(processId, roundedSize);
        };

        if (address != null) {
            // Update block to mark as allocated
            for (MemoryBlock block : memoryBlocks) {
                if (block.getBaseAddress() == address && !block.isAllocated()) {
                    if (block.getSize() > roundedSize) {
                        // Split block: create new free block for remaining space
                        MemoryBlock remaining = new MemoryBlock(
                                blockIdSequence.getAndIncrement(),
                                address + roundedSize,
                                block.getSize() - roundedSize,
                                false,
                                null
                        );
                        memoryBlocks.add(remaining);
                    }
                    block.setSize(roundedSize);
                    block.setAllocated(true);
                    block.setProcessId(processId);
                    block.setRequestedSize(requestedSize);
                    break;
                }
            }
        }

        return address;
    }

    private Integer allocateFirstFit(int size) {
        for (MemoryBlock block : memoryBlocks) {
            if (!block.isAllocated() && block.getSize() >= size) {
                return block.getBaseAddress();
            }
        }
        return null;
    }

    private Integer allocateBestFit(int size) {
        return memoryBlocks.stream()
                .filter(b -> !b.isAllocated() && b.getSize() >= size)
                .min(Comparator.comparingInt(MemoryBlock::getSize))
                .map(MemoryBlock::getBaseAddress)
                .orElse(null);
    }

    private Integer allocateWorstFit(int size) {
        return memoryBlocks.stream()
                .filter(b -> !b.isAllocated() && b.getSize() >= size)
                .max(Comparator.comparingInt(MemoryBlock::getSize))
                .map(MemoryBlock::getBaseAddress)
                .orElse(null);
    }

    private Integer allocateSegmentation(long processId, int size) {
        // Try to find contiguous free space
        List<MemoryBlock> freeBlocks = memoryBlocks.stream()
                .filter(b -> !b.isAllocated())
                .sorted(Comparator.comparingInt(MemoryBlock::getBaseAddress))
                .collect(Collectors.toList());

        for (MemoryBlock block : freeBlocks) {
            if (block.getSize() >= size) {
                int address = block.getBaseAddress();
                
                // Split block if necessary
                if (block.getSize() > size) {
                    // Create new free block for remaining space
                    MemoryBlock remaining = new MemoryBlock(
                            blockIdSequence.getAndIncrement(),
                            address + size,
                            block.getSize() - size,
                            false,
                            null
                    );
                    memoryBlocks.add(remaining);
                }
                
                // Update block to mark as allocated
                block.setSize(size);
                block.setAllocated(true);
                block.setProcessId(processId);
                
                // Create segment
                MemorySegment segment = new MemorySegment(
                        segmentIdSequence.getAndIncrement(),
                        address,
                        size,
                        processId
                );
                segments.add(segment);
                return address;
            }
        }
        return null;
    }

    public synchronized void deallocateMemory(long processId) {
        // Deallocate blocks
        for (MemoryBlock block : memoryBlocks) {
            if (block.isAllocated() && block.getProcessId() != null && block.getProcessId().equals(processId)) {
                block.setAllocated(false);
                block.setProcessId(null);
                block.setRequestedSize(0);
            }
        }

        // Remove segments
        segments.removeIf(s -> s.getProcessId() == processId);

        // Merge adjacent free blocks
        mergeFreeBlocks();
    }

    private void mergeFreeBlocks() {
        List<MemoryBlock> sorted = memoryBlocks.stream()
                .sorted(Comparator.comparingInt(MemoryBlock::getBaseAddress))
                .collect(Collectors.toList());

        List<MemoryBlock> merged = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            MemoryBlock current = sorted.get(i);
            if (current.isAllocated()) {
                merged.add(current);
            } else {
                // Try to merge with next free blocks
                int mergedSize = current.getSize();
                int endAddress = current.getBaseAddress() + mergedSize;
                int j = i + 1;
                while (j < sorted.size() && !sorted.get(j).isAllocated()) {
                    MemoryBlock next = sorted.get(j);
                    if (next.getBaseAddress() == endAddress) {
                        mergedSize += next.getSize();
                        endAddress = next.getBaseAddress() + next.getSize();
                        j++;
                    } else {
                        break;
                    }
                }
                MemoryBlock mergedBlock = new MemoryBlock(
                        current.getId(),
                        current.getBaseAddress(),
                        mergedSize,
                        false,
                        null
                );
                merged.add(mergedBlock);
                i = j - 1;
            }
        }
        memoryBlocks.clear();
        memoryBlocks.addAll(merged);
    }

    public synchronized MemoryStateResponse getMemoryState() {
        if (!initialized) {
            initializeMemory(totalMemorySize);
        }

        // Tamaño físico total reservado (múltiplos de 32KB)
        int physicalUsedSize = memoryBlocks.stream()
                .filter(MemoryBlock::isAllocated)
                .mapToInt(MemoryBlock::getSize)
                .sum();

        // Tamaño lógico total solicitado por los procesos
        int requestedTotal = memoryBlocks.stream()
                .filter(MemoryBlock::isAllocated)
                .mapToInt(MemoryBlock::getRequestedSize) // tamaño lógico solicitado
                .sum();

        // Memoria libre física (no reservada por ningún bloque)
        int freeSize = totalMemorySize - physicalUsedSize;

        // Fragmentación interna: diferencia entre tamaño reservado y solicitado
        int internalFragmentation = Math.max(0, physicalUsedSize - requestedTotal);

        int externalFragmentation = memoryBlocks.stream()
                .filter(b -> !b.isAllocated())
                .filter(b -> b.getSize() < totalMemorySize * 0.1) // Blocks < 10% of total
                .mapToInt(MemoryBlock::getSize)
                .sum();

        // usedSize en la respuesta debe representar la memoria REQUERIDA por los procesos,
        // no la físicamente reservada (para que coincida con la suma ejecutable+datos+variable).
        int usedSize = requestedTotal;

        return new MemoryStateResponse(
                new ArrayList<>(memoryBlocks),
                totalMemorySize,
                usedSize,
                freeSize,
                internalFragmentation,
                externalFragmentation,
                currentAlgorithm,
                new ArrayList<>(segments)
        );
    }

    public synchronized void setAlgorithm(MemoryAllocationAlgorithm algorithm) {
        this.currentAlgorithm = algorithm;
    }

    public synchronized MemoryAllocationAlgorithm getCurrentAlgorithm() {
        return currentAlgorithm;
    }
}

