package com.edu.ossimulator.dto;

import com.edu.ossimulator.model.MemoryAllocationAlgorithm;
import com.edu.ossimulator.model.MemoryBlock;
import com.edu.ossimulator.model.MemorySegment;

import java.util.List;

public class MemoryStateResponse {
    private List<MemoryBlock> blocks;
    private int totalSize;
    private int usedSize;
    private int freeSize;
    private int internalFragmentation;
    private int externalFragmentation;
    private MemoryAllocationAlgorithm currentAlgorithm;
    private List<MemorySegment> segments;

    public MemoryStateResponse() {
    }

    public MemoryStateResponse(List<MemoryBlock> blocks, int totalSize, int usedSize, int freeSize,
                               int internalFragmentation, int externalFragmentation,
                               MemoryAllocationAlgorithm currentAlgorithm, List<MemorySegment> segments) {
        this.blocks = blocks;
        this.totalSize = totalSize;
        this.usedSize = usedSize;
        this.freeSize = freeSize;
        this.internalFragmentation = internalFragmentation;
        this.externalFragmentation = externalFragmentation;
        this.currentAlgorithm = currentAlgorithm;
        this.segments = segments;
    }

    public List<MemoryBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<MemoryBlock> blocks) {
        this.blocks = blocks;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(int usedSize) {
        this.usedSize = usedSize;
    }

    public int getFreeSize() {
        return freeSize;
    }

    public void setFreeSize(int freeSize) {
        this.freeSize = freeSize;
    }

    public int getInternalFragmentation() {
        return internalFragmentation;
    }

    public void setInternalFragmentation(int internalFragmentation) {
        this.internalFragmentation = internalFragmentation;
    }

    public int getExternalFragmentation() {
        return externalFragmentation;
    }

    public void setExternalFragmentation(int externalFragmentation) {
        this.externalFragmentation = externalFragmentation;
    }

    public MemoryAllocationAlgorithm getCurrentAlgorithm() {
        return currentAlgorithm;
    }

    public void setCurrentAlgorithm(MemoryAllocationAlgorithm currentAlgorithm) {
        this.currentAlgorithm = currentAlgorithm;
    }

    public List<MemorySegment> getSegments() {
        return segments;
    }

    public void setSegments(List<MemorySegment> segments) {
        this.segments = segments;
    }
}

