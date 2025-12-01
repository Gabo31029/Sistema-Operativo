package com.edu.ossimulator.model;

public class MemoryBlock {
    private long id;
    private int baseAddress;
    private int size;
    private boolean isAllocated;
    private Long processId;

    public MemoryBlock() {
    }

    public MemoryBlock(long id, int baseAddress, int size, boolean isAllocated, Long processId) {
        this.id = id;
        this.baseAddress = baseAddress;
        this.size = size;
        this.isAllocated = isAllocated;
        this.processId = processId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(int baseAddress) {
        this.baseAddress = baseAddress;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isAllocated() {
        return isAllocated;
    }

    public void setAllocated(boolean allocated) {
        isAllocated = allocated;
    }

    public Long getProcessId() {
        return processId;
    }

    public void setProcessId(Long processId) {
        this.processId = processId;
    }

    @Override
    public String toString() {
        return String.format("MemoryBlock{id=%d, base=%d, size=%d, allocated=%s, pid=%s}",
                id, baseAddress, size, isAllocated, processId);
    }
}

