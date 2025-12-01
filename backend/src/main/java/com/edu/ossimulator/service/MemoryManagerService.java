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
        this.totalMemorySize = totalSize;
        MemoryBlock initialBlock = new MemoryBlock(
                blockIdSequence.getAndIncrement(),
                0,
                totalSize,
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

        if (size > totalMemorySize) {
            throw new IllegalArgumentException("Requested size exceeds total memory");
        }

        MemoryAllocationAlgorithm algo = algorithm != null ? algorithm : currentAlgorithm;

        Integer address = switch (algo) {
            case FIRST_FIT -> allocateFirstFit(size);
            case BEST_FIT -> allocateBestFit(size);
            case WORST_FIT -> allocateWorstFit(size);
            case SEGMENTATION -> allocateSegmentation(processId, size);
        };

        if (address != null) {
            // Update block to mark as allocated
            for (MemoryBlock block : memoryBlocks) {
                if (block.getBaseAddress() == address && !block.isAllocated()) {
                    if (block.getSize() > size) {
                        // Split block: create new free block for remaining space
                        MemoryBlock remaining = new MemoryBlock(
                                blockIdSequence.getAndIncrement(),
                                address + size,
                                block.getSize() - size,
                                false,
                                null
                        );
                        memoryBlocks.add(remaining);
                    }
                    block.setSize(size);
                    block.setAllocated(true);
                    block.setProcessId(processId);
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

        int usedSize = memoryBlocks.stream()
                .filter(MemoryBlock::isAllocated)
                .mapToInt(MemoryBlock::getSize)
                .sum();

        int freeSize = totalMemorySize - usedSize;

        // Internal fragmentation: difference between allocated block size and actual used size
        // For simplicity, we'll track this by storing requested size separately
        // For now, assume minimal fragmentation (could be enhanced with a map)
        int internalFragmentation = 0;

        int externalFragmentation = memoryBlocks.stream()
                .filter(b -> !b.isAllocated())
                .filter(b -> b.getSize() < totalMemorySize * 0.1) // Blocks < 10% of total
                .mapToInt(MemoryBlock::getSize)
                .sum();

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

