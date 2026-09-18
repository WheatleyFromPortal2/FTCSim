package android.os;

import java.util.HashMap;

public class Bundle {
    private final HashMap<String, Object> map = new HashMap<>();
    public Bundle() {}
    public void putString(String k, String v) { map.put(k, v); }
    public String getString(String k) { Object o = map.get(k); return o instanceof String ? (String) o : null; }
    public void putInt(String k, int v) { map.put(k, v); }
    public int getInt(String k) { Object o = map.get(k); return o instanceof Integer ? (Integer) o : 0; }
    public void putBoolean(String k, boolean v) { map.put(k, v); }
    public boolean getBoolean(String k) { Object o = map.get(k); return o instanceof Boolean && (Boolean) o; }
    public boolean containsKey(String k) { return map.containsKey(k); }
    public Object get(String k) { return map.get(k); }
    public void putSerializable(String k, java.io.Serializable v) { map.put(k, v); }
}
