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

import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3Client;
import com.ericsson.adp.mgmt.backupandrestore.exception.AWSPersistenceException;
import com.ericsson.adp.mgmt.backupandrestore.exception.FilePersistenceException;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Persistence provider which relies on AWS S3 storage to provide
 * persistence layer
 */
public class S3PersistProvider extends PersistProvider {
    private static final Logger log = LogManager.getLogger(S3PersistProvider.class);

    private final S3MultipartClient client;

    /**
     * Constructor
     * @param client - the AWSSimpleClient used by this persistProvider
     * */
    public S3PersistProvider(final S3MultipartClient client) {
        this.client = client;
    }

    @Override
    public void write(final Path folder, final Path file, final byte[] content) {
        try {
            client.uploadObject(client.getDefaultBucketName(), S3Client.toObjectKey(file.toAbsolutePath()), content);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error while writing data", e);
            throw new FilePersistenceException(e);
        }
    }

    @Override
    public Stream<Path> walk(final Path base, final int maxDepth, final boolean ordered) {
        final List<Path> allChildren;
        if (ordered) {
            allChildren = client.getObjectListOrder(client.getDefaultBucketName(), S3Client.toObjectKey(base))
                    .stream()
                    .map(S3Client::fromObjectKey)
                    .filter(p -> p.startsWith(base))
                    .map(base::relativize)
                    .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
                    .collect(Collectors.toList());
        } else {
            allChildren = client.getObjectList(client.getDefaultBucketName(), S3Client.toObjectKey(base))
                    .stream()
                    .map(S3Client::fromObjectKey)
                    .filter(p -> p.startsWith(base))
                    .map(base::relativize)
                    .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
                    .collect(Collectors.toList());
        }
        // Some notes on complexity here:
        // This ends up being something like O(n^2) where n is the count of objects under the passed method
        // because of how list membership testing works. This is pretty bad, but we generally expect n to be very small
        // so for now I'm not going to optimise it. If this does turn out to be a bottleneck, the easiest optimisation
        // would be to also keep a Set<Path> called something like "seen" and use seen.contains() instead of result.contains()
        // everywhere result.contains() is used, and do seen.add() everywhere we do result.add(). This would take it from
        // O(n^2) to O(n) by making the "is this path already in the returned stream" check O(1) instead of O(n). Worth noting
        // here that the actual worst case complexity includes maxDepth^2 as a term, which would be significantly larger than
        // n in many cases. The Set<Path> optimisation would change that term from maxDepth^2 to maxDepth, which is nice,
        // although, to be clear, this only applies in worst-case complexity, which is extremely unlikely to occur given our
        // usage of the filesystem (we don't create extremely nested, completely random folder hierarchies and then walk their
        // entire depth)
        final List<Path> result = new ArrayList<>();
        result.add(base); // The top level directory is always returned by Files.walk
        for (final Path child: allChildren) { // n
            Path prefix = base;
            for (int i = 0; i < child.getNameCount() && i < maxDepth; i++) { // min(depth of child, maxDepth)
                prefix = prefix.resolve(child.getName(i));
                if (!result.contains(prefix)) { // worst case ((n*maxDepth) - some amortizing factor), generally ~n
                    result.add(prefix);
                }
            }
        }
        return result.stream();
    }

    @Override
    public Stream<Path> walk(final Path base, final int maxDepth) {
        return walk(base, maxDepth, false);
    }

    @Override
    public String read(final Path path) {
        try (InputStream stream = client.downloadObject(client.getDefaultBucketName(), S3Client.toObjectKey(path))) {
            return new String(stream.readAllBytes());
        } catch (IOException e) {
            throw new AWSPersistenceException(e);
        }
    }

    @Override
    public void delete(final Path path) {
        if (isDir(path)) { // It doesn't make sense to try and delete a "directory" in S3
            return;
        }
        client.removeObject(client.getDefaultBucketName(), S3Client.toObjectKey(path));
    }

    @Override
    public boolean exists(final Path path) {
        return !client.getObjectList(client.getDefaultBucketName(), S3Client.toObjectKey(path), 1).isEmpty();
    }

    /**
     * List the files and directories inside a dir
     *
     * @param dir the dir to be listed.
     * @return a list of absolute path which contains the files and dirs.
     */
    @Override
    public List<Path> list(final Path dir) {
        final String prefix = S3Client.toObjectKey(dir);
        final Path abs = dir.toAbsolutePath().normalize();
        return client.getObjectList(client.getDefaultBucketName(), prefix).stream()
                .map(S3Client::fromObjectKey)
                .filter(p -> p.startsWith(abs))
                .map(p -> abs.resolve(abs.relativize(p).subpath(0, 1))) // Return "sub directories" if they exist
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Check if a path represents a dir or not
     *
     * @param path the path to be checked
     * @return true if it's a dir
     */
    @Override
    public boolean isDir(final Path path) {
        final String prefix = S3Client.toObjectKey(path);
        final List<String> matches =  client.getObjectList(client.getDefaultBucketName(), prefix, 2);
        final boolean exactMatch = client.isObjectExist(client.getDefaultBucketName(), prefix);
        if (matches.size() > 1 && exactMatch) {
            log.error("Found exact match and prefix match for object key: {}", prefix);
        }
        return !exactMatch && !matches.isEmpty();
    }

    /**
     * Check if a path represents a file or not
     *
     * @param path the path to be checked
     * @return true if it's a file
     */
    @Override
    public boolean isFile(final Path path) {
        return exists(path) && !isDir(path);
    }

    @Override
    public long length(final Path path) {
        return client.getObjectSize(client.getDefaultBucketName(), S3Client.toObjectKey(path));
    }

    @Override
    public InputStream newInputStream(final Path path, final OpenOption... options) {
        return client.downloadObject(client.getDefaultBucketName(), S3Client.toObjectKey(path));
    }

    @Override
    public OutputStream newOutputStream(final Path path, final OpenOption... options)  throws IOException {
        return client.getOutputStream(S3Client.toObjectKey(path));
    }

    @Override
    public boolean mkdirs(final Path dir) {
        return true; // For object store, making a directory is a no-op, and directories only exist if they contain something
    }

    @Override
    public boolean mkdir(final Path dir) {
        return true; // For object store, making a directory is a no-op, and directories only exist if they contain something
    }

    @Override
    public void deleteDummyFile() throws IOException {
        // Not required yet
    }

    @Override
    public void createDummyFile(final int size) {
        // Not required yet
    }

    @Override
    public void setReservedSpace(final Path path) {
        // Not required yet
    }

    @Override
    public boolean copy(final Path src, final Path dst, final boolean replace) throws IOException {
        // TODO - this might bite us some day, but right now we don't copy anything large enough for this to be a problem
        // Max single request server-side copy size is 5GB: https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html
        if (length(src) > DataSize.ofGigabytes(5).toBytes()) {
            throw new NotImplementedException("Copies of files larger than 5GB are not supported");
        }
        boolean overwrote = false;
        if (exists(dst)) {
            if (!replace) {
                log.error("Failed to copy {} to {}, destination already exists and replace is false", src, dst);
                throw new AWSPersistenceException();
            }
            delete(dst);
            overwrote = true;
        }
        final CopyObjectRequest request = new CopyObjectRequest(
                client.getDefaultBucketName(),
                S3Client.toObjectKey(src),
                client.getDefaultBucketName(),
                S3Client.toObjectKey(dst));
        client.getS3Client().copyObject(request);
        return overwrote;
    }

}
