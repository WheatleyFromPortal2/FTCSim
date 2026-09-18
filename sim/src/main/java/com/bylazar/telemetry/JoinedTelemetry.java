package com.bylazar.telemetry;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/** FTCSim stand-in for Panels' JoinedTelemetry: fans out to several Telemetry objects. */
public class JoinedTelemetry implements Telemetry {
    private final Telemetry[] delegates;
    private String itemSeparator = ", ", captionValueSeparator = ": ";
    public JoinedTelemetry(Telemetry... delegates) { this.delegates = delegates; }
    @Override public Item addData(String caption, String format, Object... args) { for (Telemetry t : delegates) t.addData(caption == null ? "" : caption, format == null ? "%s" : format, args); return null; }
    @Override public Item addData(String caption, Object value) { for (Telemetry t : delegates) t.addData(caption == null ? "" : caption, value); return null; }
    @Override public <T> Item addData(String caption, Func<T> valueProducer) { for (Telemetry t : delegates) t.addData(caption == null ? "" : caption, valueProducer); return null; }
    @Override public <T> Item addData(String caption, String format, Func<T> valueProducer) { for (Telemetry t : delegates) t.addData(caption == null ? "" : caption, format == null ? "%s" : format, valueProducer); return null; }
    @Override public boolean update() { boolean any = false; for (Telemetry t : delegates) if (t.update()) any = true; return any; }
    @Override public String getItemSeparator() { return delegates.length > 0 ? delegates[0].getItemSeparator() : itemSeparator; }
    @Override public void setItemSeparator(String s) { itemSeparator = s == null ? ", " : s; for (Telemetry t : delegates) t.setItemSeparator(itemSeparator); }
    @Override public String getCaptionValueSeparator() { return delegates.length > 0 ? delegates[0].getCaptionValueSeparator() : captionValueSeparator; }
    @Override public void setCaptionValueSeparator(String s) { captionValueSeparator = s == null ? ": " : s; for (Telemetry t : delegates) t.setCaptionValueSeparator(captionValueSeparator); }
    @Override public boolean removeItem(Item item) { return false; }
    @Override public void clear() { for (Telemetry t : delegates) t.clear(); }
    @Override public void clearAll() { for (Telemetry t : delegates) t.clearAll(); }
    @Override public Line addLine() { for (Telemetry t : delegates) t.addLine(); return null; }
    @Override public Line addLine(String lineCaption) { for (Telemetry t : delegates) t.addLine(lineCaption); return null; }
    @Override public boolean removeLine(Line line) { return false; }
    @Override public boolean isAutoClear() { return true; }
    @Override public void setAutoClear(boolean autoClear) { for (Telemetry t : delegates) t.setAutoClear(autoClear); }
    @Override public int getMsTransmissionInterval() { return delegates.length > 0 ? delegates[0].getMsTransmissionInterval() : 0; }
    @Override public void setMsTransmissionInterval(int ms) { for (Telemetry t : delegates) t.setMsTransmissionInterval(ms); }
    @Override public void setDisplayFormat(DisplayFormat displayFormat) { for (Telemetry t : delegates) t.setDisplayFormat(displayFormat); }
    @Override public Log log() { return null; }
    @Override public Object addAction(Runnable action) { return null; }
    @Override public boolean removeAction(Object token) { return false; }
    @Override public void speak(String text) {}
    @Override public void speak(String text, String languageCode, String countryCode) {}
}
