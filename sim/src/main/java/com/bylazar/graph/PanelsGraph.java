package com.bylazar.graph;

public final class PanelsGraph {
    public static final PanelsGraph INSTANCE = new PanelsGraph();
    private static final GraphManager manager = new GraphManager();
    private PanelsGraph() {}
    public GraphManager getManager() { return manager; }
}
