/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class to help with Exception related information
 *
 */
public class ExceptionUtils {

    private ExceptionUtils() {}

    /**
    * Gets the root cause of a nested Exception
    *
    * @param throwable nested exception
    * @return rootCause
    */
    public static Throwable getRootCause(final Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    /**
     * Executes a Runnable.
     * If the runnable throws an exception, a Consumer function is run to handle the exception.
     * @param toTry the runnable function
     * @param toCatch the Consumer to handle the exception.
     */
    public static void tryCatch(final Runnable toTry, final Consumer<Exception> toCatch) {
        tryCatch(() -> { toTry.run(); return null; }, toCatch);
    }

    /**
     * Executes a Supplier function.
     * If the Supplier throws an exception, a consumer function is run to handle the exception.
     * Otherwise, it returns an Optional wrapping the result of running the Supplier function.
     * @param <T> the type of object returned by the Supplier function
     * @param toTry the Supplier function
     * @param toCatch the Consumer function to handle the exception
     * @return the result of running the Supplier function.
     */
    public static <T> Optional<T> tryCatch(final Supplier<T> toTry, final Consumer<Exception> toCatch) {
        Optional<T> result = Optional.empty();
        try {
            result = Optional.ofNullable(toTry.get());
        } catch (Exception e) {
            toCatch.accept(e);
        }
        return result;
    }

}
