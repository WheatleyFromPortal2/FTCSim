package com.bylazar.configurables;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** FTCSim stand-in for Panels' PanelsConfigurables object. */
public final class PanelsConfigurables {
    public static final PanelsConfigurables INSTANCE = new PanelsConfigurables();
    private static final List<Consumer<String>> refreshListeners = new CopyOnWriteArrayList<>();
    private PanelsConfigurables() {}
    public void refreshClass(Object cls) {
        String name = cls instanceof Class ? ((Class<?>) cls).getName() : cls.getClass().getName();
        for (Consumer<String> l : refreshListeners) l.accept(name);
    }
    public static void addRefreshListener(Consumer<String> l) { refreshListeners.add(l); }
}
