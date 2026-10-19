package it.edo.codecprobe;

import java.util.*;

/** Process-local LRU with an entry and byte budget. Contains no views or open resources. */
final class MetadataMemoryCache<T> {
    private static final class Value<T> {
        final T data; final long weight;
        Value(T data, long weight) { this.data = data; this.weight = weight; }
    }
    private final LinkedHashMap<String, Value<T>> entries = new LinkedHashMap<>(16, .75f, true);
    private final int limit;
    private final long budget;
    private long bytes, epoch;
    MetadataMemoryCache(int limit, long budget) { this.limit = limit; this.budget = budget; }
    synchronized T get(String key) { Value<T> value = entries.get(key); return value == null ? null : value.data; }
    synchronized long epoch() { return epoch; }
    synchronized boolean put(String key, T data, long weight, long expected) {
        if (expected != epoch || weight < 0 || weight > budget) return false;
        Value<T> previous = entries.remove(key);
        if (previous != null) bytes -= previous.weight;
        entries.put(key, new Value<>(data, weight)); bytes += weight;
        Iterator<Value<T>> values = entries.values().iterator();
        while (entries.size() > limit || bytes > budget) { bytes -= values.next().weight; values.remove(); }
        return true;
    }
    synchronized void remove(String key) { Value<T> previous = entries.remove(key); if (previous != null) bytes -= previous.weight; epoch++; }
    synchronized void clear() { entries.clear(); bytes = 0; epoch++; }
    synchronized int size() { return entries.size(); }
    synchronized long bytes() { return bytes; }
}
