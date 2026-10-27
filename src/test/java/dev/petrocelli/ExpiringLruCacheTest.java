package dev.petrocelli;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class ExpiringLruCacheTest {
    private static final class TestClock extends Clock {
        private Instant now=Instant.parse("2026-01-01T00:00:00Z");
        public Instant instant(){return now;}
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return Clock.fixed(now,zone);}
        void advance(long seconds){now=now.plusSeconds(seconds);}
    }
    private static void check(boolean result){if(!result)throw new AssertionError();}
    private static void rejects(Class<? extends Throwable> type,Runnable action) {
        try{action.run();}catch(Throwable e){if(type.isInstance(e))return;throw e;}
        throw new AssertionError("Expected "+type);
    }
    public static void main(String[] args)throws Exception {
        var clock=new TestClock();
        var cache=new ExpiringLruCache<String,String>(2,Duration.ofSeconds(10),clock);
        cache.put("a","A"); cache.put("b","B"); cache.get("a"); cache.put("c","C");
        check(cache.get("b").isEmpty()); check(cache.get("a").orElseThrow().equals("A"));
        clock.advance(9); check(cache.get("a").isPresent());
        clock.advance(1); check(cache.get("a").isEmpty()); check(cache.size()==0);
        cache.put("x","old"); clock.advance(5); cache.put("x","new");
        clock.advance(5); check(cache.get("x").orElseThrow().equals("new"));
        clock.advance(5); check(cache.size()==0);
        cache.put("x","X"); check(cache.invalidate("x")); check(!cache.invalidate("x"));
        rejects(NullPointerException.class,()->cache.put("null",null));
        rejects(IllegalArgumentException.class,()->new ExpiringLruCache<>(0,Duration.ofSeconds(1),clock));
        rejects(IllegalArgumentException.class,()->new ExpiringLruCache<>(1,Duration.ZERO,clock));
        var full=new ExpiringLruCache<Integer,Integer>(8,Duration.ofDays(1),clock);
        var pool=Executors.newFixedThreadPool(8); var futures=new ArrayList<Future<?>>();
        try {
            for(int i=0;i<100;i++){final int id=i;futures.add(pool.submit(()->{full.put(id,id);full.get(id);}));}
            for(var future:futures)future.get(10,TimeUnit.SECONDS);
        }finally{pool.shutdownNow();}
        check(full.size()==8);
        System.out.println("ExpiringLruCache: OK");
    }
}
