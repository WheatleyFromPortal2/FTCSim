package com.bylazar.utils;

import java.util.ArrayList;
import java.util.List;

public class MovingAverageSmoother {
    private final int windowSize; private final List<Double> values = new ArrayList<>(); private double sum;
    public MovingAverageSmoother(int windowSize) { this.windowSize = windowSize; }
    public double getValue() { return values.isEmpty() ? 0 : sum / values.size(); }
    public double addValue(double value) { if (values.size() == windowSize) sum -= values.remove(0); values.add(value); sum += value; return sum / values.size(); }
    public void reset() { values.clear(); sum = 0; }
}
