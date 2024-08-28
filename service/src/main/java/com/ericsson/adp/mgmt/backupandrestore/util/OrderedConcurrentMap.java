/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A thread-safe map that maintains the order of elements.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class OrderedConcurrentMap<K, V> {
    private static final Logger log = LogManager.getLogger(OrderedConcurrentMap.class);
    private final List<K> order;
    private final ConcurrentHashMap<K, V> map;

    /**
     * Initializes a new instance of the map and the order list.
     */
    public OrderedConcurrentMap() {
        order = new LinkedList<>();
        map = new ConcurrentHashMap<>();
    }

    /**
     * Returns a Set view of the mappings contained in this map.
     *
     * @return a set view of the mappings contained in this map
     */
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                final Iterator<K> keyIterator = order.iterator();

                return new Iterator<Map.Entry<K, V>>() {
                    private K currentKey;

                    @Override
                    public boolean hasNext() {
                        return keyIterator.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        currentKey = keyIterator.next();
                        return new AbstractMap.SimpleEntry<>(currentKey, map.get(currentKey));
                    }

                    @Override
                    public void remove() {
                        if (currentKey == null) {
                            throw new IllegalStateException();
                        }
                        keyIterator.remove();
                        map.remove(currentKey);
                        currentKey = null;
                    }
                };
            }

            @Override
            public int size() {
                return map.size();
            }
        };
    }

    /**
     * Checks if the map contains the specified key.
     *
     * @param key the key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key
     */
    public boolean containsKey(final K key) {
        return map.containsKey(key);
    }

    /**
     * Replaces the entry for the specified key only if it is currently
     * mapped to some value.
     *
     * @param key the key of the entry to replace
     * @param value the new value to be associated with the specified key
     */
    public void replace(final K key, final V value) {
        map.replace(key, value);
    }

    /**
     * Associates the specified value with the specified key in this map and
     * adds the key to the order list.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    public void put(final K key, final V value) {
        map.put(key, value);
        order.add(key);
    }

    /**
     * Removes the entry for the specified key from this map and the order list.
     *
     * @param key the key of the entry to remove
     * @return the previous value associated with the specified key, or null
     * if there was no mapping for the key
     */
    public V remove(final K key) {
        final V removedValue = map.remove(key);
        order.remove(key);
        return removedValue;
    }

    /**
     * Replaces the key of an entry in this map and the order list.
     *
     * @param oldKey the old key of the entry to replace
     * @param newKey the new key to be associated with the value of the old key
     */
    public void replaceKey(final K oldKey, final K newKey) {
        if (map.containsKey(oldKey)) {
            final V value = map.remove(oldKey);
            map.put(newKey, value);
            order.set(order.indexOf(oldKey), newKey);
        }
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or null if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     * null if this map contains no mapping for the key
     */
    public V get(final K key) {
        return map.get(key);
    }

    /**
     * Returns a Collection view of the values contained in this map.
     *
     * @return a collection view of the values contained in this map
     */
    public Collection<V> values() {
        final List<V> orderedValues = new ArrayList<>();
        for (final K key : order) {
            orderedValues.add(map.get(key));
        }
        return orderedValues;
    }

    /**
     * Prints the entries of this map in the order they were added.
     */
    public void printOrderedMap() {
        log.debug("<{}>", order);
    }
}