/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.caching;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A caching wrapper around some T, such that calling get() returns a T that is at least as recently constructed
 * as the last call to invalidate(). Objects responsible for calling invalidate() are expected to register a "dropper"
 * function with the Cached, such that subsequent calls to Cached::drop will stop the "invalidator" object from marking
 * the cached value as invalid in the future.
 *
 * It is not recommended to store the result of get(), but instead to store the Cached<T> and use the result of get()
 * immediately, to avoid using a T that was constructed prior to a call to invalidate()
 *
 * @param <T> - the type of the inner object this caching wrapper wraps
 * */
public class Cached<T> {
    private static final Logger log = LogManager.getLogger(Cached.class);

    private final List<Consumer<Cached<T>>> droppers = new ArrayList<>();
    private final Lock dropLock = new ReentrantLock();
    private final Supplier<T> supplier;
    private final AtomicReference<T> inner = new AtomicReference<>();
    private final boolean lazy;

    /**
     * Construct a new cached object
     * @param supplier the supplier method called to construct a new T, following a call to invalidate
     * */
    public Cached(final Supplier<T> supplier) {
        this(supplier, true);
    }

    /**
     * Construct a new cached object
     * @param supplier the supplier method called to construct a new T, following a call to invalidate
     * @param lazy - if false, all calls to invalidate will result in a new T being built. Recommended value is true
     *             unless need for immediate rebuilds is identified
     * */
    public Cached(final Supplier<T> supplier, final boolean lazy) {
        this.supplier = supplier;
        this.lazy = lazy;
    }

    /**
     * Mark the cached inner value as invalid
     * */
    public final void invalidate() {
        inner.set(null);
        if (!lazy) {
            log.debug("Invalidated and not lazy, starting rebuild");
            build();
        }
    }

    /**
     * Retrieve a cached T, built at least as recently as the most recent call to invalidate()
     * @return a T
     * */
    public final T get() {
        final T result = inner.get();
        if (result != null) {
            return result;
        } else {
            return build();
        }
    }

    /**
     * Iterate across the registered invalidator droppers, calling each of them to remove this Cached object from the
     * set of Cached<T> objects they're managing the validity of, then clear the registered dropper list
     * */
    public final void drop() {
        dropLock.lock();
        droppers.forEach(c -> c.accept(this));
        droppers.clear();
        dropLock.unlock();
    }

    /**
     * Register as an invalidator by providing a method to the Cached which lets it tell you when to stop marking it
     * as invalid
     * @param dropper - the method this Cached will use to signal to the caller that the caller should no longer manage
     *                it's validity
     * */
    public final void addDropper(final Consumer<Cached<T>> dropper) {
        dropLock.lock();
        this.droppers.add(dropper);
        dropLock.unlock();
    }

    private T build() {
        log.debug("Rebuilding cached object");
        final T result = supplier.get();
        inner.set(result);
        return result;
    }
}
