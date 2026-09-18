package android.preference;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** In-memory preferences; the simulator does not persist Android preferences. */
public class PreferenceManager {
    private static final Map<String, MemoryPrefs> STORES = new HashMap<>();

    public static synchronized SharedPreferences getDefaultSharedPreferences(Context context) { return getSharedPreferences("default"); }

    public static synchronized SharedPreferences getSharedPreferences(String name) {
        return STORES.computeIfAbsent(name, n -> new MemoryPrefs());
    }

    static final class MemoryPrefs implements SharedPreferences {
        private final Map<String, Object> map = Collections.synchronizedMap(new HashMap<>());
        private final List<OnSharedPreferenceChangeListener> listeners = new ArrayList<>();
        @Override public Map<String, ?> getAll() { return new HashMap<>(map); }
        @Override public String getString(String key, String defValue) { Object v = map.get(key); return v instanceof String ? (String) v : defValue; }
        @SuppressWarnings("unchecked") @Override public Set<String> getStringSet(String key, Set<String> defValues) { Object v = map.get(key); return v instanceof Set ? (Set<String>) v : defValues; }
        @Override public int getInt(String key, int defValue) { Object v = map.get(key); return v instanceof Integer ? (Integer) v : defValue; }
        @Override public long getLong(String key, long defValue) { Object v = map.get(key); return v instanceof Long ? (Long) v : defValue; }
        @Override public float getFloat(String key, float defValue) { Object v = map.get(key); return v instanceof Float ? (Float) v : defValue; }
        @Override public boolean getBoolean(String key, boolean defValue) { Object v = map.get(key); return v instanceof Boolean ? (Boolean) v : defValue; }
        @Override public boolean contains(String key) { return map.containsKey(key); }
        @Override public Editor edit() {
            return new Editor() {
                private final Map<String, Object> pending = new HashMap<>();
                private final List<String> removals = new ArrayList<>();
                private boolean clearAll;
                @Override public Editor putString(String key, String value) { pending.put(key, value); return this; }
                @Override public Editor putStringSet(String key, Set<String> values) { pending.put(key, values); return this; }
                @Override public Editor putInt(String key, int value) { pending.put(key, value); return this; }
                @Override public Editor putLong(String key, long value) { pending.put(key, value); return this; }
                @Override public Editor putFloat(String key, float value) { pending.put(key, value); return this; }
                @Override public Editor putBoolean(String key, boolean value) { pending.put(key, value); return this; }
                @Override public Editor remove(String key) { removals.add(key); return this; }
                @Override public Editor clear() { clearAll = true; return this; }
                @Override public boolean commit() { apply(); return true; }
                @Override public void apply() {
                    if (clearAll) map.clear();
                    for (String k : removals) map.remove(k);
                    for (Map.Entry<String, Object> e : pending.entrySet()) { if (e.getValue() == null) map.remove(e.getKey()); else map.put(e.getKey(), e.getValue()); }
                    List<OnSharedPreferenceChangeListener> ls; synchronized (listeners) { ls = new ArrayList<>(listeners); }
                    for (String k : pending.keySet()) for (OnSharedPreferenceChangeListener l : ls) { try { l.onSharedPreferenceChanged(MemoryPrefs.this, k); } catch (RuntimeException ignored) {} }
                }
            };
        }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { synchronized (listeners) { listeners.add(l); } }
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { synchronized (listeners) { listeners.remove(l); } }
    }
}
