package com.acmerobotics.dashboard.canvas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** FTCSim stand-in for FTC Dashboard's Canvas: records field overlay operations (inches, FTC field frame). */
public class Canvas {
    private final List<Map<String, Object>> ops = new ArrayList<>();
    public Canvas() {}
    private Canvas op(String name, Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("op", name);
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        ops.add(m);
        return this;
    }
    public Canvas setScale(double scaleX, double scaleY) { return op("scale", "x", scaleX, "y", scaleY); }
    public Canvas setRotation(double radians) { return op("rotation", "r", radians); }
    public Canvas setTranslation(double x, double y) { return op("translate", "x", x, "y", y); }
    public Canvas setStrokeWidth(int width) { return op("strokeWidth", "w", width); }
    public Canvas setStroke(String color) { return op("stroke", "c", color); }
    public Canvas setFill(String color) { return op("fill", "c", color); }
    public Canvas setAlpha(double alpha) { return op("alpha", "a", alpha); }
    public Canvas strokeCircle(double x, double y, double radius) { return op("circle", "x", x, "y", y, "r", radius, "stroke", true); }
    public Canvas fillCircle(double x, double y, double radius) { return op("circle", "x", x, "y", y, "r", radius, "stroke", false); }
    public Canvas strokePolygon(double[] xPoints, double[] yPoints) { return op("polygon", "xs", xPoints.clone(), "ys", yPoints.clone(), "stroke", true); }
    public Canvas fillPolygon(double[] xPoints, double[] yPoints) { return op("polygon", "xs", xPoints.clone(), "ys", yPoints.clone(), "stroke", false); }
    public Canvas strokePolyline(double[] xPoints, double[] yPoints) { return op("polyline", "xs", xPoints.clone(), "ys", yPoints.clone()); }
    public Canvas strokeLine(double x1, double y1, double x2, double y2) { return op("line", "x1", x1, "y1", y1, "x2", x2, "y2", y2); }
    public Canvas fillRect(double x, double y, double width, double height) { return op("rect", "x", x, "y", y, "w", width, "h", height, "stroke", false); }
    public Canvas strokeRect(double x, double y, double width, double height) { return op("rect", "x", x, "y", y, "w", width, "h", height, "stroke", true); }
    public Canvas strokeText(String text, double x, double y, String font, double theta) { return op("text", "t", text, "x", x, "y", y, "font", font, "theta", theta, "stroke", true); }
    public Canvas strokeText(String text, double x, double y, String font, double theta, boolean usePageFrame) { return strokeText(text, x, y, font, theta); }
    public Canvas fillText(String text, double x, double y, String font, double theta) { return op("text", "t", text, "x", x, "y", y, "font", font, "theta", theta, "stroke", false); }
    public Canvas fillText(String text, double x, double y, String font, double theta, boolean usePageFrame) { return fillText(text, x, y, font, theta); }
    public Canvas drawImage(String path, double x, double y, double width, double height) { return op("image", "path", path, "x", x, "y", y, "w", width, "h", height); }
    public Canvas drawImage(String path, double x, double y, double width, double height, double theta, double pivotX, double pivotY, boolean usePageFrame) { return drawImage(path, x, y, width, height); }
    public Canvas drawGrid(double x, double y, double width, double height, int numTicksX, int numTicksY) { return op("grid", "x", x, "y", y, "w", width, "h", height, "nx", numTicksX, "ny", numTicksY); }
    public Canvas drawGrid(double x, double y, double width, double height, int numTicksX, int numTicksY, double theta, double pivotX, double pivotY, boolean usePageFrame) { return drawGrid(x, y, width, height, numTicksX, numTicksY); }
    public Canvas clear() { ops.clear(); return this; }
    public List<Map<String, Object>> getOperations() { return ops; }
}
