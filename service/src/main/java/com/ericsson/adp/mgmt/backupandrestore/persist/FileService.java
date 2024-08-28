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
package com.ericsson.adp.mgmt.backupandrestore.persist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.adp.mgmt.backupandrestore.exception.FilePersistenceException;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.backupandrestore.util.SecurityEventLogger;

/**
 * Responsible for writing/reading objects to/from files.
 * @param <T> Persisted Class
 */
public abstract class FileService<T extends Versioned<T>> {
    protected static final String JSON_EXTENSION = ".json";
    protected static final int DEFAULT_DEPTH_OF_FILES = 1;
    private static final Logger logger = LogManager.getLogger(FileService.class);
    private static final Pattern timezonePattern = Pattern.compile("(.*?)\\{\"timezone\":\"(.*?)\"\\}");
    private static final String BRO_CERT_ISSUE_CATEGORY = "BRO-Certificate-Issue";
    private static final String CLASS_STRING = FileService.class.getName();

    protected JsonService jsonService;
    protected PersistProvider provider;

    /**
     * Default FileService constructor, defaults to relying on PVC provider
     * */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    protected FileService() {
        setProvider(new PersistProviderFactory()); // Default to PVC use unless configured differently after construction
    }

    /**
     * Writes file.
     *
     * @param folder where the file is.
     * @param file to be written.
     * @param content of file.
     * @param version - the version of the persisted object being written
     */
    protected void writeFile(final Path folder, Path file, final byte[] content, final Version<T> version) {
        // Apply necessary version-based path mapping, then write data to disk
        file = version.fromBase(file);
        final Path toFolder = file.getParent();
        provider.write(toFolder, file, content);
    }

    /**
     * Create a directory and all necessary parent directories
     *
     * @param folder the directory to be created.
     * @return true if directory created successfully, false otherwise.
     */
    protected boolean mkdirs(final Path folder) {
        return provider.mkdirs(folder);
    }

    /**
     * Read all files as objects.
     * @param rootFolder where to start reading from.
     * @param order if the output should be order by creation time
     * @return objects read from files.
     */
    protected List<T> readObjectsFromFilesOrdered(final Path rootFolder, final boolean order) {
        return readObjectsFromFiles(rootFolder, p -> null, order);
    }

    /**
     * Read all files as objects.
     * @param rootFolder where to start reading from.
     * @return objects read from files.
     */
    protected List<T> readObjectsFromFiles(final Path rootFolder) {
        return readObjectsFromFiles(rootFolder, p -> null, false);
    }

    /**
     * Read all files as objects under the passed path, using Version information as necessary to retrieve T's of
     * different versions
     *
     * @param rootFolder where to start reading from.
     * @param defaultSupplier A callback that returns a default object of type T when the file can't be parsed into JSON
     * @return objects read from files.
     */
    protected List<T> readObjectsFromFiles(final Path rootFolder, final Function<Path, T> defaultSupplier) {
        return readObjectsFromFiles(rootFolder, defaultSupplier, false);
    }

    /**
     * Read all files as objects under the passed path, using Version information as necessary to retrieve T's of
     * different versions
     *
     * @param rootFolder where to start reading from.
     * @param defaultSupplier A callback that returns a default object of type T when the file can't be parsed into JSON
     * @param order if the output should be order by creation time
     * @return objects read from files.
     */
    protected List<T> readObjectsFromFiles(final Path rootFolder, final Function<Path, T> defaultSupplier, final boolean order) {
        try {
            final int depth = getVersions().stream().mapToInt(v -> v.fromDepth(getMaximumDepth())).max().orElse(getMaximumDepth());
            return provider.walk(rootFolder, depth, order)
                    .map(p -> getOrDefault(defaultSupplier, rootFolder, p))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (final FilePersistenceException e) {
            logger.error("File handling error while reading files of persisted objects", e);
            throw e;
        } catch (final Exception e) {
            logger.error("Error handling objects persisted as files", e);
            throw new FilePersistenceException(e);
        }
    }


    private T getOrDefault(final Function<Path, T> defaultSupplier, final Path rootFolder, final Path filePath) {
        // This is the nicest way I could figure out how to do this - you can probably do better
        boolean passedFilter = false;
        //Always try newest version first
        for (final Version<T> version: getVersions().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
            if (!version.filterFile(filePath)) {
                continue;
            }
            if (filePath.getNameCount() > rootFolder.getNameCount() + version.fromDepth(getMaximumDepth())) {
                continue;
            }
            // At this point at least one Version /tried/ to parse the file, so we should return a default object is
            // necessary. Otherwise, the file path wasn't valid, so we should return null
            passedFilter = true;
            final Optional<T> maybeValue = version.parse(readContentOfFile(filePath));
            if (maybeValue.isEmpty()) {
                continue;
            }
            final T value = maybeValue.get();
            value.setVersion(version);
            return value;
        }
        if (!passedFilter) {
            return null;
        }
        final T defaultConstructed = defaultSupplier.apply(filePath);
        if (defaultConstructed != null) {
            defaultConstructed.setVersion(getLatestVersion());
        }
        return defaultConstructed;
    }

    /**
     * How far down the file tree to look for.
     *
     * @return maximum depth.
     */
    protected int getMaximumDepth() {
        return DEFAULT_DEPTH_OF_FILES;
    }

    /**
     * Retrieve the list of versions associated with this FileService<T>
     *
     * @return The list of Version<T>'s associated with this T
     */
    protected abstract List<Version<T>> getVersions();

    /**
     * Returns default BRO version
     * @param parse Parser associated to the version
     * @param filter persisted actions filter function
     * @return Default Version
     */
    protected final Version<T> getDefaultVersion(final Function<String, Optional<T>> parse, final Predicate<Path> filter) {
        return new Version<>(p -> p, parse, d -> d, filter, 0);
    }

    public Version<T> getLatestVersion() {
        return getVersions()
                .stream()
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new NoSuchElementException("FileService provided no version"));
    }

    /**
     * Gets file.
     * @param entityName name of entity to be written to file
     * @return file
     */
    protected Path getFile(final String entityName) {
        return Paths.get(entityName + JSON_EXTENSION);
    }

    /**
     * return fileName as backupManagerId
     * @param localPath path to convert into string
     * @return String representing backup manager id
     */
    protected String getBackupManagerId(final Path localPath) {
        return localPath.getFileName().toString();
    }

    /**
     * Read the content of a file.
     *
     * @param path
     *            Path of the file to read.
     * @return File contents as a String.
     */
    protected String readContentOfFile(final Path path) {
        return provider.read(path);
    }

    /**
     * Get the size of a directory in PVC.
     * @param folder
     *            Path of the directory to query.
     * @return size of the directory. The method returns 0 if the directory is not found
     *            or an IOException is encountered while traversing the directory.
     */
    public long getFolderSize(final Path folder) {
        try (Stream<Path> fileStream = Files.walk(folder)) {
            return fileStream
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (final NoSuchFileException e) {
            logger.warn("The folder {} is not found, returning 0.", folder);
            return 0;
        } catch (final IOException e) {
            logger.error("Error calculating folder size, returning 0.", e);
            return 0;
        }
    }

    /**
     * Validates If Path is valid and is readable
     * NOTE: this will still hit the direct filesystem when object store is enabled,
     *       however, it is only used for keystore configurations, so it's not a problem
     * reside on a volume even when object store is enabled
     * @param path path to be tested
     * @return boolean if is readable
     */
    public static boolean isPathValidReadable(final String path) {
        if (isPathValid (path)) {
            final Path testPath = Paths.get(path);
            if (!Files.isReadable(testPath)) {
                SecurityEventLogger.logSecurityErrorEvent(
                    CLASS_STRING, BRO_CERT_ISSUE_CATEGORY,
                    "Certificate not Available: Error validating Path "
                    + path + " not readable.");
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Validate a non blank Path
     * @param path String Path to be validated
     * @return true is the Path is not empty, and is a valid Path
     */
    public static boolean isPathValid(final String path) {
        if (path.isBlank()) {
            return false;
        }
        try {
            Paths.get(path);
        } catch (final InvalidPathException ex) {
            SecurityEventLogger.logSecurityErrorEvent(
                    CLASS_STRING, BRO_CERT_ISSUE_CATEGORY,
                    "Certificate not Available: Invalid path provided for certificate or keystore: " + path);
            logger.error("Invalid path <{}>", path, ex);
            return false;
        }
        return true;
    }

    /**
     * Delete a given object from the persistence layer
     * @param path - The object/file to delete
     * @throws IOException - if the deletion fails
     * */
    public void delete(final Path path) throws IOException {
        provider.delete(path);
    }

    /**
     * Delete a given directory from the persistence layer
     * @param path - The directory to be removed
     * @throws IOException - if the directory traversal fails
     */
    public void deleteDirectory(final Path path) throws IOException {
        if (provider.isDir(path)) {
            provider.walk(path, Integer.MAX_VALUE)
                .sorted(Comparator.reverseOrder())
                .forEach(file -> {
                    try {
                        provider.delete(file);
                    } catch (IOException e) {
                        logger.error("Failed to delete the path/object {}", file);
                    }
                });
        } else {
            logger.error("The path <{}> is not a directory", path);
        }
    }

    /**
     * Does the given path exist in the persistence layer?
     * @param path - The object/file to test
     * @return true if the resource exists
     * */
    public boolean exists(final Path path) {
        return provider.exists(path);
    }

    /**
     * Delete a dummy file to recover space
     * @throws IOException on error writing dummy file
     */
    public void deleteDummyFile() throws IOException {
        provider.deleteDummyFile();
    }

    /**
     * Creates a dummyFile with 1MB by default
     */
    public void createDummyFile() {
        provider.createDummyFile(1024 * 1024);
    }

    /**
     * Set the reserved space path for dummy file
     * @param pathReserved path to write the dummy file used to reserve space
     */
    public void setReservedSpace(final Path pathReserved) {
        provider.setReservedSpace(pathReserved);
    }

    /**
     * Returns persisted timezone information
     *
     * @param fileContent File contents as a String
     *
     * @return persisted timezone information if exists if not returns null
     *
     */
    protected String getTimeZone(final String fileContent) {
        final Matcher matcher = timezonePattern.matcher(fileContent);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }

    @Autowired
    public void setJsonService(final JsonService jsonService) {
        this.jsonService = jsonService;
    }

    /**
     * Setup the persistence provider used by the file service
     * @param configuration - provider configuration used
     * */
    @Autowired
    public void setProvider(final PersistProviderFactory configuration) {
        provider = configuration.getPersistProvider();
    }
}