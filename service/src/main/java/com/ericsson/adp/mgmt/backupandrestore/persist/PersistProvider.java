/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.persist;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Interface describing basic operations of object persistence providers (e.g PVC, AWS)
 *
 * */
public abstract class PersistProvider {

    /**
     * Write the passed bytes to the persistence layer
     * @param folder - the "folder" (hierarchy location) to write to
     * @param file - the "file" (object name) to write to
     * @param content - the data to write
     * */
    public abstract void write(final Path folder, final Path file, final byte[] content);

    /**
     * Walk a directory tree, returning a list of all fs nodes
     * found below path (files and folders). Guaranteed to include
     * the source Path passed. Same semantics as Files.walk, with the
     * following divergences:
     *  - Not guaranteed to lazily walk the underlying filesystem
     *  - The particulars of the IO operations done to obtain the
     *    file information are not specified (e.g. the underlying
     *    persist provider may or may not access the file attributes)
     *  - If the underlying persistence layer supports symbolic links,
     *    they are not followed. Following symbolic links is not
     *    supported
     * Note particularly the walk order: Guaranteed to be depth first,
     * in line with Files.walk. Implementing persist providers are
     * required to return file objects in a depth first manner.
     * @param path - the path to start the walk from
     * @param maxDepth - the maximum depth to traverse to during the walk
     * @throws IOException - if an I/O error is thrown when accessing the underlying persistence layer.
     * @return a stream consisting of the paths found
     * */
    public abstract Stream<Path> walk(Path path, int maxDepth) throws IOException;

    /**
     * Walk a directory tree, returning a list of all fs nodes
     * found below path (files and folders). Guaranteed to include
     * the source Path passed. Same semantics as Files.walk, with the
     * following divergences:
     *  - Not guaranteed to lazily walk the underlying filesystem
     *  - The particulars of the IO operations done to obtain the
     *    file information are not specified (e.g. the underlying
     *    persist provider may or may not access the file attributes)
     *  - If the underlying persistence layer supports symbolic links,
     *    they are not followed. Following symbolic links is not
     *    supported
     * Note particularly the walk order: Guaranteed to be depth first,
     * in line with Files.walk. Implementing persist providers are
     * required to return file objects in a depth first manner.
     * @param path - the path to start the walk from
     * @param maxDepth - the maximum depth to traverse to during the walk
     * @param ordered -- true output is order by creation time
     * @throws IOException - if an I/O error is thrown when accessing the underlying persistence layer.
     * @return a stream consisting of the paths found
     * */
    public abstract Stream<Path> walk(Path path, int maxDepth, boolean ordered) throws IOException;

    /**
     * Read a given persisted object and return the string contents in the persist layer
     * @param path - the location of the object to read
     * @return data retrieved from persistence layer
     * */
    public abstract String read(final Path path);

    /**
     * Delete a given persisted object from the persistence layer
     * @param path - location of object to delete
     * @throws IOException - should deletion fail
     * */
    public abstract void delete(final Path path) throws IOException;

    /**
     * Checks whether an object exists at the given location in the persistence layer
     * @param path - location to check for object
     * @return true if resource exists
     * */
    public abstract boolean exists(final Path path);

    /**
     * List the files and directories inside a dir
     * @param dir the dir to be listed.
     * @return a list of path which contains the files and dirs.
     */
    public abstract  List<Path> list(Path dir);

    /**
     * Check if a path represents a dir or not
     * @param path the path to be checked
     * @return true if it's a dir
     */
    public abstract boolean isDir(Path path);

    /**
     * Check if a path represents a file or not
     * @param path the path to be checked
     * @return true if it's a file
     */
    public abstract boolean isFile(Path path);

    /**
     * Get the length of a file
     * @param path - the path of the file to retrieve the length of
     * @throws IOException - if communication with the underlying persistence layer fails
     * @return the length of the file
     * */
    public abstract long length(Path path) throws IOException;

    /**
     * Open an input stream to read large amounts of data from the underlying persistence layer
     * @param path - the path of the file to read data from
     * @param options - the options to open the file with (for whatever that means for the underlying persistence layer)
     * @throws IOException - if communication with the underlying persistence layer fails
     * @return an input stream
     * */
    public abstract InputStream newInputStream(Path path, OpenOption... options) throws IOException;

    /**
     * Open an output stream to write large amounts of data to the underlying persistence layer
     * @param path - the path of the file to write data to
     * @param options - the options to open the file with (for whatever that means for the underlying persistence layer)
     * @throws IOException - if communication with the underlying persistence layer fails
     * @return an output stream
     * */
    public abstract OutputStream newOutputStream(Path path, OpenOption... options) throws IOException;

    /**
     * Make a directory and all necessary parent directories
     * @param dir - the path to to make the directory at
     * @return true if directory created successfully
     * */
    public abstract boolean mkdirs(final Path dir);

    /**
     * Make a directory
     * @param dir - the path to to make the directory at
     * @return true if directory created successfully
     * */
    public abstract boolean mkdir(final Path dir);

    /**
     * delete a dummy file to release some space
     * @throws IOException on error writing dummy file
     */
    public abstract void deleteDummyFile() throws IOException;

    /**
     * Creates a dummy file
     * @param size file size expected to be stored
     */
    public abstract void createDummyFile(final int size);

    /**
     * Specified the path used to contain a dummy file used to reserve space
     * @param path location of the dummy file
     */
    public abstract void setReservedSpace(final Path path);

    /**
     * Specified the path used to contain a dummy file used to reserve space.
     *
     * Atomic moves are not supported.
     * @param src location of the source file
     * @param dst destination to copy file to
     * @param replace control whether dst existing causes operation failure
     * @throws IOException if an error occurs
     * @return true if a file was overwritten
     */
    public abstract boolean copy(final Path src, final Path dst, final boolean replace) throws IOException;
}
