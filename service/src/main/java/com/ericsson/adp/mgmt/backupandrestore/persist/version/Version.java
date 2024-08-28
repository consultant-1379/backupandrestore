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
package com.ericsson.adp.mgmt.backupandrestore.persist.version;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * BRO Service Version
 * @param <T> Persisted Class
 */
public class Version<T> implements Comparable<Version<T>> {
    private final Function<Path, Path> pathMap;
    private final Function<String, Optional<T>> tryParse;
    private final Function<Integer, Integer> depthMap;
    private final Predicate<Path> fileFilter;
    private final int ordinal;

    /**
     * Constructor version
     * @param pathMap A mapping function from a base path to a version-specific file path.
     * @param tryParse Parsing function used in the version.
     * @param depthMap A mapping function that returns the maximum level of directories to visit and parse.
     * @param fileFilter filter Persisted object files subdirectory
     * @param ordinal version ordinal 0,1,2..
     */
    public Version(final Function<Path, Path> pathMap,
                   final Function<String, Optional<T>> tryParse,
                   final Function<Integer, Integer> depthMap,
                   final Predicate<Path> fileFilter,
                   final int ordinal) {
        this.pathMap = pathMap;
        this.tryParse = tryParse;
        this.depthMap = depthMap;
        this.fileFilter = fileFilter;
        this.ordinal = ordinal;
    }

    /**
     * Apply version-based path mapping
     * @param base Persisted object base path
     * @return Path resolved
     */
    public Path fromBase(final Path base) {
        return pathMap.apply(base);
    }

    /**
     * Parser persisted object
     * @param data persisted object
     * @return Persisted Class
     */
    public Optional<T> parse(final String data) {
        return tryParse.apply(data);
    }

    /**
     * Level to look up for persisted objects
     * @param baseDepth base level
     * @return level applied
     */
    public int fromDepth(final int baseDepth) {
        return depthMap.apply(baseDepth);
    }

    /**
     * Identify persisted objects
     * @param file file to be classified as persisted object
     * @return true if file is persisted object otherwise false
     */
    public boolean filterFile(final Path file) {
        return fileFilter.test(file);
    }

    @Override
    public int compareTo(@NotNull final Version<T> otherOrdinal) {
        return this.ordinal - otherOrdinal.ordinal;
    }
}
