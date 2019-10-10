package com.aservo.ldap.adapter.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LruCacheMapTest {

    @Test
    @Order(1)
    @DisplayName("it should remove the oldest elements")
    public void test001()
            throws Exception {

        LruCacheMap<Integer, Integer> cache = new LruCacheMap<>(4, Duration.of(1, ChronoUnit.HOURS));

        cache.put(1, 42);
        Assertions.assertArrayEquals(new Integer[]{1}, cache.keySet().toArray());

        cache.put(2, 43);
        Assertions.assertArrayEquals(new Integer[]{1, 2}, cache.keySet().toArray());

        cache.put(3, 44);
        Assertions.assertArrayEquals(new Integer[]{1, 2, 3}, cache.keySet().toArray());

        cache.put(4, 45);
        Assertions.assertArrayEquals(new Integer[]{1, 2, 3, 4}, cache.keySet().toArray());

        cache.put(5, 46);
        Assertions.assertArrayEquals(new Integer[]{2, 3, 4, 5}, cache.keySet().toArray());

        cache.put(6, 47);
        Assertions.assertArrayEquals(new Integer[]{3, 4, 5, 6}, cache.keySet().toArray());

        cache.put(7, 48);
        Assertions.assertArrayEquals(new Integer[]{4, 5, 6, 7}, cache.keySet().toArray());

        cache.put(8, 49);
        Assertions.assertArrayEquals(new Integer[]{5, 6, 7, 8}, cache.keySet().toArray());

        cache.put(9, 50);
        Assertions.assertArrayEquals(new Integer[]{6, 7, 8, 9}, cache.keySet().toArray());
    }

    @Test
    @Order(2)
    @DisplayName("it should remove the least recently used elements")
    public void test002()
            throws Exception {

        LruCacheMap<Integer, Integer> cache = new LruCacheMap<>(4, Duration.of(1, ChronoUnit.HOURS));

        cache.put(1, 42);
        Assertions.assertArrayEquals(new Integer[]{1}, cache.keySet().toArray());

        cache.put(2, 43);
        Assertions.assertArrayEquals(new Integer[]{1, 2}, cache.keySet().toArray());

        cache.put(3, 44);
        Assertions.assertArrayEquals(new Integer[]{1, 2, 3}, cache.keySet().toArray());

        cache.put(4, 45);
        Assertions.assertArrayEquals(new Integer[]{1, 2, 3, 4}, cache.keySet().toArray());

        cache.get(1);
        Assertions.assertArrayEquals(new Integer[]{2, 3, 4, 1}, cache.keySet().toArray());

        cache.put(5, 46);
        Assertions.assertArrayEquals(new Integer[]{3, 4, 1, 5}, cache.keySet().toArray());

        cache.put(6, 47);
        Assertions.assertArrayEquals(new Integer[]{4, 1, 5, 6}, cache.keySet().toArray());

        cache.get(5);
        Assertions.assertArrayEquals(new Integer[]{4, 1, 6, 5}, cache.keySet().toArray());

        cache.put(7, 48);
        Assertions.assertArrayEquals(new Integer[]{1, 6, 5, 7}, cache.keySet().toArray());

        cache.put(8, 49);
        Assertions.assertArrayEquals(new Integer[]{6, 5, 7, 8}, cache.keySet().toArray());

        cache.put(9, 50);
        Assertions.assertArrayEquals(new Integer[]{5, 7, 8, 9}, cache.keySet().toArray());
    }

    @Test
    @Order(3)
    @DisplayName("it should remove outdated elements")
    public void test003()
            throws Exception {

        LruCacheMap<Integer, Integer> cache = new LruCacheMap<>(4, Duration.of(2, ChronoUnit.SECONDS));

        cache.put(1, 42);
        Assertions.assertArrayEquals(new Integer[]{1}, cache.keySet().toArray());

        cache.put(2, 43);
        Assertions.assertArrayEquals(new Integer[]{1, 2}, cache.keySet().toArray());

        Thread.sleep(3000);

        cache.put(3, 44);
        Assertions.assertArrayEquals(new Integer[]{3}, cache.keySet().toArray());

        cache.put(4, 44);
        Assertions.assertArrayEquals(new Integer[]{3, 4}, cache.keySet().toArray());
    }
}
