package android.util;

import java.util.TreeMap;

/** Simple desktop implementation of android.util.SparseArray. */
public class SparseArray<E> implements Cloneable {
    private final TreeMap<Integer, E> map = new TreeMap<>();
    public SparseArray() {}
    public SparseArray(int initialCapacity) {}
    public E get(int key) { return map.get(key); }
    public E get(int key, E valueIfKeyNotFound) { E v = map.get(key); return v != null ? v : valueIfKeyNotFound; }
    public void delete(int key) { map.remove(key); }
    public void remove(int key) { map.remove(key); }
    public void removeAt(int index) { map.remove(keyAt(index)); }
    public void put(int key, E value) { map.put(key, value); }
    public void append(int key, E value) { map.put(key, value); }
    public int size() { return map.size(); }
    public int keyAt(int index) { int i = 0; for (Integer k : map.keySet()) { if (i++ == index) return k; } throw new ArrayIndexOutOfBoundsException(index); }
    public E valueAt(int index) { int i = 0; for (E v : map.values()) { if (i++ == index) return v; } throw new ArrayIndexOutOfBoundsException(index); }
    public void setValueAt(int index, E value) { map.put(keyAt(index), value); }
    public int indexOfKey(int key) { int i = 0; for (Integer k : map.keySet()) { if (k == key) return i; i++; } return -1; }
    public int indexOfValue(E value) { int i = 0; for (E v : map.values()) { if (v == value) return i; i++; } return -1; }
    public boolean contains(int key) { return map.containsKey(key); }
    public void clear() { map.clear(); }
    @Override @SuppressWarnings("unchecked")
    public SparseArray<E> clone() { SparseArray<E> c = new SparseArray<>(); c.map.putAll(map); return c; }
    @Override public String toString() { return map.toString(); }
}
