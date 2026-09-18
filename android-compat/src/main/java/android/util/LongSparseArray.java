package android.util;

import java.util.TreeMap;

/** Simple desktop implementation of android.util.LongSparseArray. */
public class LongSparseArray<E> implements Cloneable {
    private final TreeMap<Long, E> map = new TreeMap<>();
    public LongSparseArray() {}
    public LongSparseArray(int initialCapacity) {}
    public E get(long key) { return map.get(key); }
    public E get(long key, E valueIfKeyNotFound) { E v = map.get(key); return v != null ? v : valueIfKeyNotFound; }
    public void delete(long key) { map.remove(key); }
    public void remove(long key) { map.remove(key); }
    public void removeAt(int index) { map.remove(keyAt(index)); }
    public void put(long key, E value) { map.put(key, value); }
    public void append(long key, E value) { map.put(key, value); }
    public int size() { return map.size(); }
    public long keyAt(int index) { int i = 0; for (Long k : map.keySet()) { if (i++ == index) return k; } throw new ArrayIndexOutOfBoundsException(index); }
    public E valueAt(int index) { int i = 0; for (E v : map.values()) { if (i++ == index) return v; } throw new ArrayIndexOutOfBoundsException(index); }
    public void setValueAt(int index, E value) { map.put(keyAt(index), value); }
    public int indexOfKey(long key) { int i = 0; for (Long k : map.keySet()) { if (k == key) return i; i++; } return -1; }
    public int indexOfValue(E value) { int i = 0; for (E v : map.values()) { if (v == value) return i; i++; } return -1; }
    public void clear() { map.clear(); }
    @Override @SuppressWarnings("unchecked")
    public LongSparseArray<E> clone() { LongSparseArray<E> c = new LongSparseArray<>(); c.map.putAll(map); return c; }
    @Override public String toString() { return map.toString(); }
}
