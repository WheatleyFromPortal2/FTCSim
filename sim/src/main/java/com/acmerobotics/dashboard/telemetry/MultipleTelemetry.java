package com.acmerobotics.dashboard.telemetry;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;

/** FTC Dashboard's MultipleTelemetry: forwards to several Telemetry instances. */
public class MultipleTelemetry implements Telemetry {
    private final List<Telemetry> telemetryList = new ArrayList<>();
    private final MultipleLog log = new MultipleLog();
    public MultipleTelemetry(Telemetry... telemetryList) { for (Telemetry t : telemetryList) this.telemetryList.add(t); }
    public void addTelemetry(Telemetry telemetry) { telemetryList.add(telemetry); }
    @Override public Item addData(String caption, String format, Object... args) { List<Item> items = new ArrayList<>(); for (Telemetry t : telemetryList) items.add(t.addData(caption, format, args)); return new MultipleItem(items); }
    @Override public Item addData(String caption, Object value) { List<Item> items = new ArrayList<>(); for (Telemetry t : telemetryList) items.add(t.addData(caption, value)); return new MultipleItem(items); }
    @Override public <T> Item addData(String caption, Func<T> valueProducer) { List<Item> items = new ArrayList<>(); for (Telemetry t : telemetryList) items.add(t.addData(caption, valueProducer)); return new MultipleItem(items); }
    @Override public <T> Item addData(String caption, String format, Func<T> valueProducer) { List<Item> items = new ArrayList<>(); for (Telemetry t : telemetryList) items.add(t.addData(caption, format, valueProducer)); return new MultipleItem(items); }
    @Override public boolean removeItem(Item item) { boolean r = true; if (item instanceof MultipleItem) { List<Item> items = ((MultipleItem) item).items; for (int i = 0; i < telemetryList.size() && i < items.size(); i++) r &= telemetryList.get(i).removeItem(items.get(i)); } return r; }
    @Override public void clear() { for (Telemetry t : telemetryList) t.clear(); }
    @Override public void clearAll() { for (Telemetry t : telemetryList) t.clearAll(); }
    @Override public Object addAction(Runnable action) { List<Object> tokens = new ArrayList<>(); for (Telemetry t : telemetryList) tokens.add(t.addAction(action)); return tokens; }
    @Override @SuppressWarnings("unchecked") public boolean removeAction(Object token) { boolean r = true; List<Object> tokens = (List<Object>) token; for (int i = 0; i < telemetryList.size() && i < tokens.size(); i++) r &= telemetryList.get(i).removeAction(tokens.get(i)); return r; }
    @Override public void speak(String text) { for (Telemetry t : telemetryList) t.speak(text); }
    @Override public void speak(String text, String languageCode, String countryCode) { for (Telemetry t : telemetryList) t.speak(text, languageCode, countryCode); }
    @Override public boolean update() { boolean r = true; for (Telemetry t : telemetryList) r &= t.update(); return r; }
    @Override public Line addLine() { List<Line> lines = new ArrayList<>(); for (Telemetry t : telemetryList) lines.add(t.addLine()); return new MultipleLine(lines); }
    @Override public Line addLine(String lineCaption) { List<Line> lines = new ArrayList<>(); for (Telemetry t : telemetryList) lines.add(t.addLine(lineCaption)); return new MultipleLine(lines); }
    @Override public boolean removeLine(Line line) { boolean r = true; if (line instanceof MultipleLine) { List<Line> lines = ((MultipleLine) line).lines; for (int i = 0; i < telemetryList.size() && i < lines.size(); i++) r &= telemetryList.get(i).removeLine(lines.get(i)); } return r; }
    @Override public boolean isAutoClear() { return telemetryList.isEmpty() || telemetryList.get(0).isAutoClear(); }
    @Override public void setAutoClear(boolean autoClear) { for (Telemetry t : telemetryList) t.setAutoClear(autoClear); }
    @Override public int getMsTransmissionInterval() { return telemetryList.isEmpty() ? 250 : telemetryList.get(0).getMsTransmissionInterval(); }
    @Override public void setMsTransmissionInterval(int ms) { for (Telemetry t : telemetryList) t.setMsTransmissionInterval(ms); }
    @Override public String getItemSeparator() { return telemetryList.isEmpty() ? " | " : telemetryList.get(0).getItemSeparator(); }
    @Override public void setItemSeparator(String s) { for (Telemetry t : telemetryList) t.setItemSeparator(s); }
    @Override public String getCaptionValueSeparator() { return telemetryList.isEmpty() ? " : " : telemetryList.get(0).getCaptionValueSeparator(); }
    @Override public void setCaptionValueSeparator(String s) { for (Telemetry t : telemetryList) t.setCaptionValueSeparator(s); }
    @Override public void setDisplayFormat(DisplayFormat displayFormat) { for (Telemetry t : telemetryList) t.setDisplayFormat(displayFormat); }
    @Override public Log log() { return log; }

    private static class MultipleItem implements Item {
        final List<Item> items; MultipleItem(List<Item> items) { this.items = items; }
        @Override public String getCaption() { for (Item i : items) if (i != null) return i.getCaption(); return null; }
        @Override public Item setCaption(String caption) { for (Item i : items) if (i != null) i.setCaption(caption); return this; }
        @Override public Item setValue(String format, Object... args) { for (Item i : items) if (i != null) i.setValue(format, args); return this; }
        @Override public Item setValue(Object value) { for (Item i : items) if (i != null) i.setValue(value); return this; }
        @Override public <T> Item setValue(Func<T> valueProducer) { for (Item i : items) if (i != null) i.setValue(valueProducer); return this; }
        @Override public <T> Item setValue(String format, Func<T> valueProducer) { for (Item i : items) if (i != null) i.setValue(format, valueProducer); return this; }
        @Override public Item setRetained(Boolean retained) { for (Item i : items) if (i != null) i.setRetained(retained); return this; }
        @Override public boolean isRetained() { for (Item i : items) if (i != null) return i.isRetained(); return false; }
        @Override public Item addData(String caption, String format, Object... args) { for (Item i : items) if (i != null) i.addData(caption, format, args); return this; }
        @Override public Item addData(String caption, Object value) { for (Item i : items) if (i != null) i.addData(caption, value); return this; }
        @Override public <T> Item addData(String caption, Func<T> valueProducer) { for (Item i : items) if (i != null) i.addData(caption, valueProducer); return this; }
        @Override public <T> Item addData(String caption, String format, Func<T> valueProducer) { for (Item i : items) if (i != null) i.addData(caption, format, valueProducer); return this; }
    }
    private static class MultipleLine implements Line {
        final List<Line> lines; MultipleLine(List<Line> lines) { this.lines = lines; }
        @Override public Item addData(String caption, String format, Object... args) { List<Item> items = new ArrayList<>(); for (Line l : lines) items.add(l == null ? null : l.addData(caption, format, args)); return new MultipleItem(items); }
        @Override public Item addData(String caption, Object value) { List<Item> items = new ArrayList<>(); for (Line l : lines) items.add(l == null ? null : l.addData(caption, value)); return new MultipleItem(items); }
        @Override public <T> Item addData(String caption, Func<T> valueProducer) { List<Item> items = new ArrayList<>(); for (Line l : lines) items.add(l == null ? null : l.addData(caption, valueProducer)); return new MultipleItem(items); }
        @Override public <T> Item addData(String caption, String format, Func<T> valueProducer) { List<Item> items = new ArrayList<>(); for (Line l : lines) items.add(l == null ? null : l.addData(caption, format, valueProducer)); return new MultipleItem(items); }
    }
    private class MultipleLog implements Log {
        @Override public int getCapacity() { return telemetryList.isEmpty() || telemetryList.get(0).log() == null ? 9 : telemetryList.get(0).log().getCapacity(); }
        @Override public void setCapacity(int capacity) { for (Telemetry t : telemetryList) if (t.log() != null) t.log().setCapacity(capacity); }
        @Override public DisplayOrder getDisplayOrder() { return telemetryList.isEmpty() || telemetryList.get(0).log() == null ? DisplayOrder.OLDEST_FIRST : telemetryList.get(0).log().getDisplayOrder(); }
        @Override public void setDisplayOrder(DisplayOrder displayOrder) { for (Telemetry t : telemetryList) if (t.log() != null) t.log().setDisplayOrder(displayOrder); }
        @Override public void add(String entry) { for (Telemetry t : telemetryList) if (t.log() != null) t.log().add(entry); }
        @Override public void add(String format, Object... args) { for (Telemetry t : telemetryList) if (t.log() != null) t.log().add(format, args); }
        @Override public void clear() { for (Telemetry t : telemetryList) if (t.log() != null) t.log().clear(); }
    }
}
