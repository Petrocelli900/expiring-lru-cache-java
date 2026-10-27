package dev.petrocelli;

import java.time.*;
import java.util.*;

/** Bounded single-process cache with expire-after-write semantics. */
public final class ExpiringLruCache<K,V> {
    private record Entry<V>(V value,Instant expiresAt) {}
    private final int capacity;
    private final Duration ttl;
    private final Clock clock;
    private final LinkedHashMap<K,Entry<V>> entries=new LinkedHashMap<>(16,0.75f,true);
    public ExpiringLruCache(int capacity,Duration ttl,Clock clock) {
        if(capacity<1) throw new IllegalArgumentException("Positive capacity required");
        this.ttl=Objects.requireNonNull(ttl); this.clock=Objects.requireNonNull(clock);
        if(ttl.isZero() || ttl.isNegative()) throw new IllegalArgumentException("Positive TTL required");
        this.capacity=capacity;
    }
    public synchronized void put(K key,V value) {
        Objects.requireNonNull(key); Objects.requireNonNull(value);
        var now=clock.instant();
        var expiresAt=now.plus(ttl); // Validate time arithmetic before mutating.
        purge(now);
        entries.put(key,new Entry<>(value,expiresAt));
        while(entries.size()>capacity) {
            var iterator=entries.keySet().iterator(); iterator.next(); iterator.remove();
        }
    }
    public synchronized Optional<V> get(K key) {
        Objects.requireNonNull(key);
        var now=clock.instant();
        var entry=entries.get(key);
        if(entry==null) return Optional.empty();
        if(!entry.expiresAt().isAfter(now)) {
            entries.remove(key); return Optional.empty();
        }
        return Optional.of(entry.value());
    }
    public synchronized boolean invalidate(K key) {
        Objects.requireNonNull(key); purge(clock.instant());
        return entries.remove(key)!=null;
    }
    public synchronized int size() { purge(clock.instant()); return entries.size(); }
    private void purge(Instant now) {
        entries.values().removeIf(entry->!entry.expiresAt().isAfter(now));
    }
}
