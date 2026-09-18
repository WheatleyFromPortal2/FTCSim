package com.bylazar.field;

import ftcsim.bridge.DashboardBridge;

import java.util.*;

/** FTCSim stand-in for Panels' FieldManager (a turtle-style drawing API for the field view). */
public class FieldManager {
    private Canvas canvas = new Canvas();
    private Canvas lastCanvas = new Canvas();
    private final Map<UUID, String> images = new LinkedHashMap<>();
    private UUID defaultBgID;
    private long updateInterval = 50;
    private long lastUpdate = 0;
    private double cursorX, cursorY, cursorHeading;
    private String currentFill = PanelsField.WHITE, currentOutlineFill = PanelsField.TRANSPARENT;
    private double currentOutlineWidth = 0.1;

    public FieldManager() { canvas.setPreset(FieldPresets.PANELS); lastCanvas.setPreset(FieldPresets.PANELS); }

    public void init() {}
    public Canvas getCanvas() { return canvas; } public void setCanvas(Canvas c) { canvas = c; }
    public Canvas getLastCanvas() { return lastCanvas; }
    public Map<UUID, String> getImages() { return images; }
    public UUID getDefaultBgID() { return defaultBgID; }
    public long getUpdateInterval() { return updateInterval; }
    public long getLastUpdate() { return lastUpdate; }
    public long getTimeSinceLastUpdate() { return System.currentTimeMillis() - lastUpdate; }
    public boolean getShouldUpdateCanvas() { return getTimeSinceLastUpdate() >= updateInterval; }
    public double getCursorX() { return cursorX; } public double getCursorY() { return cursorY; } public double getCursorHeading() { return cursorHeading; }
    public String getCurrentFill() { return currentFill; } public String getCurrentOutlineFill() { return currentOutlineFill; } public double getCurrentOutlineWidth() { return currentOutlineWidth; }
    public Style getCurrentStyle() { return new Style(currentFill, currentOutlineFill, currentOutlineWidth); }

    public void moveCursor(double x, double y) { cursorX = x; cursorY = y; }
    public void setFill(String fill) { currentFill = fill; }
    public void setOutlineFill(String fill) { currentOutlineFill = fill; }
    public void setOutlineWidth(double width) { currentOutlineWidth = width; }
    public void setOutline(String fill, double width) { setOutlineFill(fill); setOutlineWidth(width); }
    public void setStyle(String fill, String outline, double width) { setFill(fill); setOutline(outline, width); }
    public void setStyle(Style style) { setFill(style.getFill()); setOutline(style.getOutlineFill(), style.getOutlineWidth()); }
    public void clearFill() { setFill(PanelsField.TRANSPARENT); }
    public void clearOutline() { setOutline(PanelsField.TRANSPARENT, 0.0); }
    public void clearStyle() { clearFill(); clearOutline(); }
    public synchronized void circle(double r) { canvas.getItems().add(new Circle(cursorX, cursorY, r, getCurrentStyle())); }
    public synchronized void line(double x2, double y2) { canvas.getItems().add(new Line(cursorX, cursorY, x2, y2, getCurrentStyle())); }
    public synchronized void rect(double w, double h) { canvas.getItems().add(new Rectangle(cursorX, cursorY, w, h, getCurrentStyle())); }
    public UUID registerImage(ImagePreset preset) { return registerBase64Image(preset.get()); }
    public UUID registerBase64Image(String base64) {
        for (Map.Entry<UUID, String> e : images.entrySet()) if (e.getValue().equals(base64)) return e.getKey();
        UUID id = UUID.randomUUID(); images.put(id, base64); return id;
    }
    public UUID registerImage(String path) { return registerBase64Image(path); }
    public synchronized void img(double w, double h, UUID id) { canvas.getItems().add(new Image(cursorX, cursorY, w, h, id)); }
    public void setBase64Background(String base64) { canvas.setBgID(registerBase64Image(base64)); }
    public void setBackground(String path) { setBase64Background(path); }
    public void setBackground(UUID id) { canvas.setBgID(id); }
    public void setBackground(ImagePreset image) { setBase64Background(image.get()); }
    public void setOffsets(FieldPresetParams f) { canvas.setPreset(f); }

    public synchronized void update() {
        if (getShouldUpdateCanvas()) {
            List<Map<String, Object>> ops = new ArrayList<>();
            for (Drawable d : canvas.getItems()) {
                Map<String, Object> m = new LinkedHashMap<>();
                if (d instanceof Circle) { Circle c = (Circle) d; m.put("op", "circle"); m.put("x", c.getX()); m.put("y", c.getY()); m.put("r", c.getR()); style(m, c.getStyle()); }
                else if (d instanceof Rectangle) { Rectangle r = (Rectangle) d; m.put("op", "rect"); m.put("x", r.getX()); m.put("y", r.getY()); m.put("w", r.getW()); m.put("h", r.getH()); style(m, r.getStyle()); }
                else if (d instanceof Line) { Line l = (Line) d; m.put("op", "line"); m.put("x1", l.getX1()); m.put("y1", l.getY1()); m.put("x2", l.getX2()); m.put("y2", l.getY2()); style(m, l.getStyle()); }
                else continue;
                ops.add(m);
            }
            String preset = canvas.getPreset() == null ? "" : canvas.getPreset().getName();
            String frame = "Pedro Pathing".equals(preset) ? "pedro" : "ftc";
            DashboardBridge.panelsDrawing(frame, ops);
            lastUpdate = System.currentTimeMillis();
            lastCanvas.setBgID(canvas.getBgID());
            lastCanvas.setItems(new ArrayList<>(canvas.getItems()));
            lastCanvas.setPreset(canvas.getPreset());
        }
        canvas.reset();
        cursorX = 0; cursorY = 0; cursorHeading = 0;
    }

    private static void style(Map<String, Object> m, Style s) {
        if (s == null) return;
        m.put("fill", s.getFill()); m.put("stroke", s.getOutlineFill()); m.put("width", s.getOutlineWidth());
    }
}
