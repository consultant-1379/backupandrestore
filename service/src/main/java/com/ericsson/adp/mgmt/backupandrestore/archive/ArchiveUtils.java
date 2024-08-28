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
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.archive;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.exception.FileDirectoryException;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProvider;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType.SCHEDULED;

/**
 * Class which implements a set of static utility methods for interacting with tar streams
 * */
@Component
public class ArchiveUtils {
    public static final String TAR_GZ = ".tar.gz";
    public static final int BLOCK_SIZE = 8 * 1024;

    private static final Logger log = LogManager.getLogger(ArchiveUtils.class);

    private PersistProvider provider;

    /**
     * Returns the name (in the File.getName() sense) of the tarball to be created
     *
     * @param backup - the backup to be compressed
     * @return the name of the file to be created to store the compressed backup
     * */
    public static String getTarballName(final Backup backup) {
        if (backup.getCreationType().equals(SCHEDULED)) {
            return backup.getName() + TAR_GZ;
        } else {
            final OffsetDateTime backupCreationTime = backup.getCreationTime();
            final String timestamp = DateTimeUtils.convertToString(backupCreationTime);
            return backup.getName() + "-" + timestamp + TAR_GZ;
        }
    }

    /**
     * Create a TarArchiveEntry out of the passed base path, source path and prefix
     *
     * @param base - The base path the source path is within, e.g. source = /a/b/c/d, base = /a/b
     * @param source - The source path. If this is a directory, a trailing "/" is appended to the entry name
     * @param destPrefix - the prefix of the location of the entry within the tarball. Dynamically handles prefixes missing trailing "/"
     * @return a TarArchiveEntry as described above
     * */
    public TarArchiveEntry newEntry(final Path base, final Path source, final String destPrefix) {
        final String prefix = destPrefix + (destPrefix.endsWith(File.separator) ? "" : File.separator);
        final boolean isDir = provider.isDir(source);
        return new TarArchiveEntry(prefix + base.relativize(source) + (isDir ? "/" : ""));
    }

    /**
     * Take a TarArchiveEntry and set it's size based on the size of the passed source file. Does no checking for
     * file existence, so tar archive entry size may be set to 0L by this method. Consumer is responsible for verifying
     * passed file exists, as there's nothing we can do to handle non-existing files here
     *
     * @param entry - The entry whose size we initialise
     * @param source - The location of the file which will be used as the source for this entry
     * @throws IOException if the files length cannot be retrieved
     * */
    public void initEntry(final TarArchiveEntry entry, final Path source) throws IOException {
        entry.setSize(provider.length(source));
    }

    /**
     * Takes a given location and returns an input stream to read the file at that location through
     *
     * @param location of the file to read from
     * @return an input stream built from that file
     * @throws IOException if the location passed is invalid
     * */
    public InputStream getInputStream(final Path location) throws IOException {
        return provider.newInputStream(location);
    }

    /**
     * Open an output stream to write data to for a given location. Will file at location if one does not exist, and
     * overwrite the present data if one does
     *
     * @param location to store bytes written to output steam in
     * @return an output stream which can be written to to store bytes to disk
     * @throws IOException if creation of output stream fails
     * */
    public OutputStream getOutputStream(final Path location) throws IOException {
        return provider.newOutputStream(location, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Walk a directory, returning a list of child paths in it, to an arbitrary depth.
     *
     * @param dir - the directory to walk
     * @return list of Path's representing files found in directory
     * @throws IOException if walk fails
     * */
    public List<Path> walk(final Path dir) throws IOException {
        return provider.walk(dir, Integer.MAX_VALUE).collect(Collectors.toList());
    }

    /**
     * Trivial wrapper around File.mkdir(). Added to ease later refactoring
     *
     * @param dir - location to create directory at
     * @return boolean, true if directory created
     * */
    public boolean mkdir(final Path dir) {
        return provider.mkdir(dir);
    }

    /**
     * This method deletes a file or directory and all subdirectories
     *
     *  @param file File object representing a file or directory to delete
     *
     */
    public void deleteFile(final Path file) {
        if (file != null && provider.exists(file)) {
            if (log != null) {
                log.info("Deleting file {}", file);
            }
            try (Stream<Path> stream = provider.walk(file, Integer.MAX_VALUE)) {
                // Delete file and all subdirectories/files,
                final List<Path> failed = stream
                    // longest first to avoid trying to delete a non-empty directory
                    .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
                    // Store any deletion failures to report on
                    .filter(provider::exists).filter(f -> {
                        try {
                            provider.delete(f);
                            return false;
                        } catch (IOException e) {
                            log.warn("Failed to delete file", e);
                            return true;
                        }
                    }).collect(Collectors.toList());
                if (!failed.isEmpty()) {
                    throw new FileDirectoryException("Failed to delete: " + failed);
                }
            } catch (final IOException e) {
                if (log != null) {
                    log.error("Error deleting file {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Start constructing an archive entry name prefix
     * @param root - root of entry to construct
     * @return Prefix builder object to construct name prefix with
     * */
    public static Prefix prefix(final String root) {
        return new Prefix(root);
    }


    public PersistProvider getProvider() {
        return provider;
    }

    @Autowired
    public void setProvider(final PersistProviderFactory providerFactory) {
        this.provider = providerFactory.getPersistProvider();
    }

    /**
     * Simple builder style class for use in ArchiveService to construct archive entry name prefixes
     * */
    public static class Prefix {
        private final StringBuilder builder;

        /**
         * Construct a new prefix object, starting with "start"
         * @param start - start of new prefix
         * */
        private Prefix(final String start) {
            builder = new StringBuilder(start);
        }

        /**
         * Add part to prefix
         * @param part to add
         * @return existing prefix object, having appended part
         * */
        public Prefix add(final String part) {
            builder.append(File.separator).append(part);
            return this;
        }

        /**
         * Fork the prefix, constructing a new Prefix object which is an exact copy of this one, and appending part to
         * /that/ object rather than this one. Useful for constructing multiple Prefix object with a shared set of starting
         * parts
         * @param part to add to new Prefix object
         * @return new Prefix object
         * */
        public Prefix fork(final String part) {
            return new Prefix(build() + part);
        }

        /**
         * Compile the constructed prefix to a string
         * @return the prefix compiled to a string, suitable for use in an archive entry
         * */
        public String build() {
            return builder.toString() + File.separator;
        }
    }
}
