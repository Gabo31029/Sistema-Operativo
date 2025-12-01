package com.edu.ossimulator.model;

public class MemorySegment {
    private long segmentId;
    private int baseAddress;
    private int limit;
    private long processId;

    public MemorySegment() {
    }

    public MemorySegment(long segmentId, int baseAddress, int limit, long processId) {
        this.segmentId = segmentId;
        this.baseAddress = baseAddress;
        this.limit = limit;
        this.processId = processId;
    }

    public long getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(long segmentId) {
        this.segmentId = segmentId;
    }

    public int getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(int baseAddress) {
        this.baseAddress = baseAddress;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public long getProcessId() {
        return processId;
    }

    public void setProcessId(long processId) {
        this.processId = processId;
    }

    @Override
    public String toString() {
        return String.format("MemorySegment{id=%d, base=%d, limit=%d, pid=%d}",
                segmentId, baseAddress, limit, processId);
    }
}

