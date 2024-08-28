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
package com.ericsson.adp.mgmt.backupandrestore.persist;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3Client;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class S3PersistProviderTest {

    private static final String BUCKET_NAME = "persisttestbucket";
    private static final Path ROOT = Path.of("/irrelevant/test/path").toAbsolutePath();
    private static final byte[] DATA = "somedata".getBytes(StandardCharsets.UTF_8);

    private static final Supplier<Integer> MAX_DEPTH = () -> 2;
    private static final Predicate<Path> FILTER = p -> p.getFileName().toString().endsWith(".json");
    private static final Function<String, Optional<String>> PARSE = Optional::of;

    private S3MultipartClient client;
    private Bucket bucket;
    private PersistProvider persistProvider;

    @Before
    public void setup() {
        client = createMock(S3MultipartClient.class);
        bucket = createMock(Bucket.class);
        persistProvider = new S3PersistProvider(client);
    }

    @Test
    public void write_simple() throws InterruptedException {
        expectGetBucketName(1);
        client.uploadObject(eq(BUCKET_NAME), anyString(), eq(DATA));
        expectLastCall().once();

        replay(client);
        replay(bucket);

        persistProvider.write(ROOT, ROOT.resolve("test"), DATA);

        verify(client);
        verify(bucket);
    }

    @Test
    public void write_noBucket_createsBucket() throws InterruptedException {
        expectGetBucketName(1);
        client.uploadObject(eq(BUCKET_NAME), anyString(), eq(DATA));
        expectLastCall().once();

        replay(client);
        replay(bucket);

        persistProvider.write(ROOT, ROOT.resolve("test"), DATA);

        verify(client);
        verify(bucket);
    }

    @Test
    public void readTest() throws IOException {
        final List<Path> shouldRead = List.of(ROOT.resolve("1.json"), ROOT.resolve("2/2.json"), ROOT.resolve("indirect/../indirect.json"));
        final List<Path> toDeep = List.of(ROOT.resolve("3/3/3.json"), ROOT.resolve("4/4/4/4.json"));
        final List<Path> wrongType = List.of(ROOT.resolve("5.xml"));
        final List<Path> allPaths = new ArrayList<>(shouldRead);
        allPaths.addAll(toDeep);
        allPaths.addAll(wrongType);

        expectGetBucketName();
        expectGetBucket();

        expect(client.getObjectList(BUCKET_NAME, S3Client.toObjectKey(ROOT))).andReturn(allPaths
                .stream()
                .map(p -> p.getRoot().relativize(p).toString())
                .collect(Collectors.toList())
        ).once();

        for (int i = 0; i < shouldRead.size(); i++) {
            expect(client.downloadObject(BUCKET_NAME, shouldRead.get(i).getRoot().relativize(shouldRead.get(i)).toString()))
                .andReturn(getInputStream("" + i)).once();
        }

        replay(client);
        replay(bucket);


        final List<Path> walkResult = persistProvider.walk(ROOT, MAX_DEPTH.get()).collect(Collectors.toList());
        //Mimic what the fileService does with the result of a walk
        final List<String> returned = walkResult.stream()
                .filter(FILTER)
                .map(p -> persistProvider.read(p)).collect(Collectors.toList());

        final List<String> expected = List.of("0", "1", "2");
        assertEquals(expected.size(), returned.size());
        for (final String e: expected) {
            assertTrue(returned.contains(e));
        }
        verify(client);
        verify(bucket);
    }

    @Test
    public void existsTest() {
        final Path existingFile = ROOT.resolve("1.json");
        final List<Path> paths = List.of(ROOT.resolve("1.json"), ROOT.resolve("2/2.json"), ROOT.resolve("indirect/../indirect.json"));

        expectGetBucketName();
        expectGetBucket();
        expect(client.getObjectList(BUCKET_NAME, S3Client.toObjectKey(existingFile), 1)).andReturn(paths
                .stream()
                .map(p -> p.getRoot().relativize(p).toString())
                .collect(Collectors.toList())
        ).anyTimes();

        replay(client);
        replay(bucket);

        assertTrue(persistProvider.exists(existingFile));

        verify(client);
        verify(bucket);
    }

    private S3ObjectInputStream getInputStream(final String data) {
        return new S3ObjectInputStream(new ByteArrayInputStream(data.getBytes()), null);
    }

    private void expectGetBucketName(final int times) {
        expect(client.getDefaultBucketName()).andReturn(BUCKET_NAME).times(times);
    }

    private void expectGetBucketName() {
        expect(client.getDefaultBucketName()).andReturn(BUCKET_NAME).anyTimes();
    }

    private void expectGetBucket(final int times) {
        expect(client.getBucket(BUCKET_NAME)).andReturn(Optional.of(bucket)).times(times);
    }

    private void expectGetBucket() {
        expect(client.getBucket(BUCKET_NAME)).andReturn(Optional.of(bucket)).anyTimes();
    }
}
