package android.util;

import java.util.TreeMap;

public class SparseIntArray implements Cloneable {
    private final TreeMap<Integer, Integer> map = new TreeMap<>();
    public SparseIntArray() {}
    public SparseIntArray(int initialCapacity) {}
    public int get(int key) { return get(key, 0); }
    public int get(int key, int valueIfKeyNotFound) { Integer v = map.get(key); return v != null ? v : valueIfKeyNotFound; }
    public void delete(int key) { map.remove(key); }
    public void put(int key, int value) { map.put(key, value); }
    public void append(int key, int value) { map.put(key, value); }
    public int size() { return map.size(); }
    public int keyAt(int index) { int i = 0; for (Integer k : map.keySet()) { if (i++ == index) return k; } throw new ArrayIndexOutOfBoundsException(index); }
    public int valueAt(int index) { int i = 0; for (Integer v : map.values()) { if (i++ == index) return v; } throw new ArrayIndexOutOfBoundsException(index); }
    public int indexOfKey(int key) { int i = 0; for (Integer k : map.keySet()) { if (k == key) return i; i++; } return -1; }
    public void clear() { map.clear(); }
}
