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
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.exception.FilePersistenceException;

/**
 * Persistence provider which relies on local disk storage to provide
 * persistence layer
 */
public class PVCPersistProvider extends PersistProvider {
    private static final Logger logger = LogManager.getLogger(PVCPersistProvider.class);
    private static final String BACKUP_FILE_DUMMY_NAME = "dummy.txt";
    private static final Path DUMMY_FILE_LOCATION = Paths.get(System.getProperty("java.io.tmpdir"), BACKUP_FILE_DUMMY_NAME)
            .toAbsolutePath().normalize();

    protected Path dummyLocation = DUMMY_FILE_LOCATION;
    private Path dummyFile;

    @Override
    public void write(final Path folder, final Path file, final byte[] content) {
        try {
            Files.createDirectories(folder);
            Files.write(file, content);
        } catch (final Exception e) {
            throw new FilePersistenceException(e);
        }
    }

    @Override
    public Stream<Path> walk(final Path rootFolder, final int maxDepth, final boolean ordered) throws IOException {
        if (ordered) {
            return getSortedFilesStream(rootFolder, maxDepth);
        } else {
            return walk(rootFolder, maxDepth);
        }
    }

    @Override
    public Stream<Path> walk(final Path rootFolder, final int maxDepth) throws IOException {
        return Files.walk(rootFolder, maxDepth);
    }

    /*
     * Due Files is a final classes it can't work properly in junit
     */
    @SuppressWarnings("unchecked")
    private Stream<Path> getSortedFilesStream(final Path rootFolder, final int maxDepth) {
        try {
            return Files.walk(rootFolder, maxDepth)
                    .sorted(Comparator.comparingLong(path -> {
                        try {
                            logger.debug("Reading backupManager <{}> - time:<{}>", path,
                                    Files.readAttributes(path, BasicFileAttributes.class).creationTime().toMillis());
                            return Files.readAttributes(path, BasicFileAttributes.class).creationTime().toMillis();
                        } catch (IOException e) {
                            return Long.MAX_VALUE;
                        }
                    }));
        } catch (IOException e) {
            logger.error("Error reading persisted sorted BackupManager files", e);
            throw new FilePersistenceException(e);
        }
    }

    @Override
    public String read(final Path path) {
        try {
            logger.debug("Reading persisted file <{}>", path);
            return Files.readString(path);
        } catch (final IOException e) {
            throw new FilePersistenceException(e);
        }
    }

    @Override
    public void delete(final Path path) throws IOException {
        Files.delete(path);
    }

    @Override
    public boolean exists(final Path path) {
        return path.toFile().exists();
    }

    /**
     * List the files and directories inside a dir
     *
     * @param dir the dir to be listed.
     * @return a list of path which contains the files and dirs.
     */
    @Override
    public List<Path> list(final Path dir) {
        try (Stream<Path> pathStream = Files.list(dir)) {
            return pathStream.collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Check if a path represents a dir or not
     *
     * @param path the path to be checked
     * @return true if it's a dir
     */
    @Override
    public boolean isDir(final Path path) {
        return Files.isDirectory(path);
    }

    /**
     * Check if a path represents a file or not
     *
     * @param path the path to be checked
     * @return true if it's a file
     */
    @Override
    public boolean isFile(final Path path) {
        return Files.exists(path) && !isDir(path);
    }

    @Override
    public long length(final Path path) throws IOException {
        return Files.size(path);
    }

    @Override
    public InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {
        return Files.newInputStream(path, options);
    }

    @Override
    public OutputStream newOutputStream(final Path path, final OpenOption... options) throws IOException {
        return Files.newOutputStream(path, options);
    }

    @Override
    public boolean mkdirs(final Path dir) {
        return dir.toFile().mkdirs();
    }

    @Override
    public boolean mkdir(final Path dir) {
        return dir.toFile().mkdir();
    }


    /**
     * Create a Dummy file in PVC provider
     * @param size bytes file expected
     */
    @Override
    public void createDummyFile(final int size) {
        if (dummyFile != null && !exists(dummyFile)) {
            final byte[] bytes = new byte[size];
            Arrays.fill( bytes, (byte) 1 );
            try {
                this.write(dummyFile.getParent(), dummyFile, bytes);
            } catch (Exception exception) {
                logger.error("Cannot reserve support space, validate your disk space", exception);
            }
        }
    }

    /**
     * Remove the dummy files
     * @throws IOException if not able to remove the reserved space
     */
    @Override
    public void deleteDummyFile() throws IOException {
        delete(dummyFile);
    }

    @Override
    public void setReservedSpace(final Path pathReserved) {
        dummyFile = pathReserved.resolve(BACKUP_FILE_DUMMY_NAME);
    }

    @Override
    public boolean copy(final Path src, final Path dst, final boolean replace) throws IOException {
        final boolean res = exists(dst);
        if (replace) {
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.copy(src, dst);
        }
        return res;
    }
}
