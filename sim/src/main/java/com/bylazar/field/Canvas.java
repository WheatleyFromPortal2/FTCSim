package com.bylazar.field;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Canvas {
    private FieldPresetParams preset = new FieldPresetParams();
    private List<Drawable> items = new ArrayList<>();
    private UUID bgID;
    public FieldPresetParams getPreset() { return preset; } public void setPreset(FieldPresetParams p) { preset = p; }
    public List<Drawable> getItems() { return items; } public void setItems(List<Drawable> i) { items = i; }
    public UUID getBgID() { return bgID; } public void setBgID(UUID id) { bgID = id; }
    public void reset() { items.clear(); }
    public void resetOffsets() { preset = new FieldPresetParams(); }
}
