/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Queue used to process each element by a REST Processor
 * @param <T> item to be enqueued
 *
 */
public class ManagedQueueingWorker<T> {
    private static final Logger log = LogManager.getLogger(ManagedQueueingWorker.class);

    /** how long we have to wait for reading a new message **/
    private static final int MAX_WAIT_TIME_NEW_MESSAGES = 5;
    private static final int DEFAULT_QUEUE_SIZE = 200;
    private static final long PROCESS_SECONDS_WAIT = 10;

    private final long notProcessSecondsTimeout;

    private final BlockingDeque<T> queue;
    /** Control the process is running */
    private final AtomicBoolean processRunning = new AtomicBoolean();

    private final ProcessorEngine<T> processorEngine;
    private final Processor processor;

    private Thread worker;

    /**
     * Blocking queue to process each Message
     * @param processorEngine External REST processor
     */
    public ManagedQueueingWorker(final ProcessorEngine<T> processorEngine) {
        this(processorEngine, DEFAULT_QUEUE_SIZE, PROCESS_SECONDS_WAIT);
    }

    /**
     * Blocking queue to process each Message
     * @param processorEngine External REST processor
     * @param capacity Queue capacity
     */
    public ManagedQueueingWorker(final ProcessorEngine<T> processorEngine, final int capacity) {
        this(processorEngine, capacity, PROCESS_SECONDS_WAIT);
    }

    /**
     * Blocking queue to process each Message.
     * Assign different TTL for each item queued based on service state.
     * on timeout the item is removed from queue
     * @param processorEngine External REST processor
     * @param capacity Queue capacity
     * @param notProcessSecondsTimeout TTL for a consumer processor if service is overloading
     */
    public ManagedQueueingWorker(final ProcessorEngine<T> processorEngine,
                                 final int capacity,
                                 final long notProcessSecondsTimeout) {
        queue = new LinkedBlockingDeque<>(capacity);
        this.notProcessSecondsTimeout = notProcessSecondsTimeout;
        this.processorEngine = processorEngine;
        this.processor =  new Processor();
        worker = new Thread(this.processor);
        worker.start();
        processRunning.set(true); // We've just started, so we're running
    }

    /**
     * Indicate if the queue is empty
     * @return boolean true if no elements in queue
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Peeks the first queue element
     * @return element or null if not exist.
     */
    public T getTop() {
        return queue.peekFirst();
    }

    /**
     * Put message as the first element in the processor queue
     * @param message CMM message to be added
     * @return true if added otherwise false
     */
    public boolean offerFirst(final T message) {
        if (!processRunning.get()) { // If we're not accepting work, bail out here
            return false;
        }
        return queue.offerFirst(message);
    }

    /**
     * Add a CMM Message into the blocking queue
     * @param message CMM message to be added
     * @return true if added otherwise false
     */
    public boolean add(final T message) {
        if (processRunning.get() && !worker.isAlive()) { // If the worker died but we're still accepting work, restart it
            worker = new Thread(new Processor());
            worker.start();
        }
        if (!processRunning.get()) { // If we're not accepting work, bail out here
            return false;
        }

        boolean result;
        try {
            result = queue.offer(message, notProcessSecondsTimeout, TimeUnit.SECONDS);
        } catch (final InterruptedException  interruptedException) {
            result = false;
            Thread.currentThread().interrupt();
        }
        return result;
    }

    /**
     * Process a CMM Message and wait for the result
     * @param message CMM message to be added
     */
    public void addAndWait(final T message) {
        if (processRunning.get() && !worker.isAlive()) { // If the worker died but we're still accepting work, restart it
            worker = new Thread(new Processor());
            worker.start();
        }
        processorEngine.transferMessage(message);
    }

    /**
     * Stop the queue processing
     */
    public void stopProcessing() {
        processRunning.set(false);
        try {
            worker.join(5 * PROCESS_SECONDS_WAIT * 1000); // Wait for 5 times longer than the worker should
        } catch (final InterruptedException e) {
            worker.interrupt();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Return worker thread
     * @return boolean true if the process is running
     */
    public Thread getWorker() {
        return worker;
    }

    private class Processor implements Runnable {

        @Override
        public void run() {
            while (processRunning.get()) {
                try {
                    final Optional<T> message = Optional.ofNullable(queue.poll(MAX_WAIT_TIME_NEW_MESSAGES, TimeUnit.SECONDS));
                    if (message.isPresent()) {
                        if (log.isDebugEnabled()) {
                            log.debug("processing queued message: <{}>", message.get());
                        }
                        final T result = processorEngine.transferMessage(message.get());
                        if (result != null) {
                            queue.putFirst(result);
                        }
                    }
                } catch (final InterruptedException e) {
                    log.warn(String.format("Interrupt queue processor - queue capacity: %d size: %d ",
                            queue.remainingCapacity(), queue.size()));
                    Thread.currentThread().interrupt();
                    // We break here without clearing processRunning to indicate the worker thread was interrupted, not stopped via stopProcessing
                    // - This means if someone calls add() the worker will be started again.
                    break;
                } catch (final Exception e) {
                    log.error("Caught unexpected exception while handling message: ", e);
                }
            }
        }
    }

    /**
     * Indicate is the process running
     * @return boolean true if the process is running
     */
    public boolean isProcessRunning() {
        return processRunning.get();
    }

    /**
     * create a CopyOnWriteArrayList of the elements in the queue
     * @return the list of elements
     */
    public CopyOnWriteArrayList<T> toList() {
        return new CopyOnWriteArrayList<>(queue);
    }

    /**
     * Search for the 1st element compliant with the filter and offerit first in the queue
     * @param filterBy predicate to use as a filter
     * @return true on moved false on otherwise
     */
    public boolean ToTop(final Predicate<T> filterBy) {
        final AtomicBoolean removed = new AtomicBoolean();
        // Ensure thread safety during modification
        synchronized (queue) {
            final Optional<T> optionalMessage = queue.stream()
                    .filter(filterBy)
                    .findFirst();
            optionalMessage.ifPresent(message -> {
                if (queue.remove(message)) {
                    log.debug("Prioritizing the message {} to be processed", message);
                    queue.offerFirst(message);
                    removed.compareAndExchange(false, true);
                }
            });
        }
        return removed.get();
    }
}
