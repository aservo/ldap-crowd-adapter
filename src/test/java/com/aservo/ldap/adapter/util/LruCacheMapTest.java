package com.aservo.ldap.adapter.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LruCacheMapTest {

    @Test
    public void test_001_removeByCreationOrder()
            throws Exception {

        LruCacheMap<Integer, Integer> cache = new LruCacheMap<>(4, Duration.of(1, ChronoUnit.HOURS));

        cache.put(1, 42);
        Assert.assertArrayEquals(new Integer[]{1}, cache.keySet().toArray());

        cache.put(2, 43);
        Assert.assertArrayEquals(new Integer[]{1, 2}, cache.keySet().toArray());

        cache.put(3, 44);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3}, cache.keySet().toArray());

        cache.put(4, 45);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4}, cache.keySet().toArray());

        cache.put(5, 46);
        Assert.assertArrayEquals(new Integer[]{2, 3, 4, 5}, cache.keySet().toArray());

        cache.put(6, 47);
        Assert.assertArrayEquals(new Integer[]{3, 4, 5, 6}, cache.keySet().toArray());

        cache.put(7, 48);
        Assert.assertArrayEquals(new Integer[]{4, 5, 6, 7}, cache.keySet().toArray());

        cache.put(8, 49);
        Assert.assertArrayEquals(new Integer[]{5, 6, 7, 8}, cache.keySet().toArray());

        cache.put(9, 50);
        Assert.assertArrayEquals(new Integer[]{6, 7, 8, 9}, cache.keySet().toArray());
    }

    @Test
    public void test_002_removeByAccessOrder()
            throws Exception {

        LruCacheMap<Integer, Integer> cache = new LruCacheMap<>(4, Duration.of(1, ChronoUnit.HOURS));

        cache.put(1, 42);
        Assert.assertArrayEquals(new Integer[]{1}, cache.keySet().toArray());

        cache.put(2, 43);
        Assert.assertArrayEquals(new Integer[]{1, 2}, cache.keySet().toArray());

        cache.put(3, 44);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3}, cache.keySet().toArray());

        cache.put(4, 45);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4}, cache.keySet().toArray());

        cache.get(1);
        Assert.assertArrayEquals(new Integer[]{2, 3, 4, 1}, cache.keySet().toArray());

        cache.put(5, 46);
        Assert.assertArrayEquals(new Integer[]{3, 4, 1, 5}, cache.keySet().toArray());

        cache.put(6, 47);
        Assert.assertArrayEquals(new Integer[]{4, 1, 5, 6}, cache.keySet().toArray());

        cache.get(5);
        Assert.assertArrayEquals(new Integer[]{4, 1, 6, 5}, cache.keySet().toArray());

        cache.put(7, 48);
        Assert.assertArrayEquals(new Integer[]{1, 6, 5, 7}, cache.keySet().toArray());

        cache.put(8, 49);
        Assert.assertArrayEquals(new Integer[]{6, 5, 7, 8}, cache.keySet().toArray());

        cache.put(9, 50);
        Assert.assertArrayEquals(new Integer[]{5, 7, 8, 9}, cache.keySet().toArray());
    }

    @Test
    public void test_003_removeByPriorityOrder()
            throws Exception {

        LruCacheMap<Integer, Integer> cache = new LruCacheMap<>(4, Duration.of(2, ChronoUnit.SECONDS));

        cache.put(1, 42);
        Assert.assertArrayEquals(new Integer[]{1}, cache.keySet().toArray());

        cache.put(2, 43);
        Assert.assertArrayEquals(new Integer[]{1, 2}, cache.keySet().toArray());

        Thread.sleep(3000);

        cache.put(3, 44);
        Assert.assertArrayEquals(new Integer[]{3}, cache.keySet().toArray());

        cache.put(4, 44);
        Assert.assertArrayEquals(new Integer[]{3, 4}, cache.keySet().toArray());
    }
}
