package com.bylazar.utils;

public class LoopTimer {
    private long startTime, endTime, ms; private double hz; private long innerStart, innerEnd;
    private final MovingAverageSmoother smoother;
    public LoopTimer() { this(5); }
    public LoopTimer(int smootherWindow) { smoother = new MovingAverageSmoother(smootherWindow); }
    public long getStartTime() { return startTime; } public long getEndTime() { return endTime; } public long getMs() { return ms; } public double getHz() { return hz; }
    public void start() { innerStart = System.currentTimeMillis(); }
    public void end() { innerEnd = System.currentTimeMillis(); startTime = innerStart; endTime = innerEnd; ms = endTime - startTime; double raw = ms > 0 ? 1000.0 / ms : 1000.0; hz = smoother.addValue(raw); }
}
