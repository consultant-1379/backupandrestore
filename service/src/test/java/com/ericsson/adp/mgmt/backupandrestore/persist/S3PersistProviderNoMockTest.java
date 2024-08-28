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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3Client;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;
import com.ericsson.adp.mgmt.backupandrestore.util.OSUtils;
import io.findify.s3mock.S3Mock;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class S3PersistProviderNoMockTest {
    private static final S3Mock api = new S3Mock.Builder().withPort(28001).withInMemoryBackend().build();

    private static S3MultipartClient client;
    private static String defaultBucketName;
    private static PersistProvider persistProvider;

    @BeforeClass
    public static void setup() {
        //set up the mock server
        api.start();
        defaultBucketName = "bro";
        S3Config s3Config = new S3Config();
        s3Config.setEnabled(true);
        s3Config.setDefaultBucketName(defaultBucketName);
        s3Config.setHost("http://localhost");
        s3Config.setPort("28001");
        s3Config.setAccessKeyName("ak");
        s3Config.setSecretKeyName("sk");
        final KeyStoreService service = EasyMock.createMock(KeyStoreService.class);
        expect(service.getKeyStore(KeyStoreAlias.OSMN)).andReturn(null).anyTimes();
        replay(service);
        s3Config.setKeyStoreService(service);


        client = new S3MultipartClient(s3Config);
        client.createBucket();
        persistProvider = new S3PersistProvider(client);
    }

    @AfterClass
    public static void tearDown() {
        api.shutdown();
    }

    @Test
    public void delete_path_valid() throws IOException, InterruptedException {
        final Path path = Path.of("home/test/file");
        byte[] bytes = new byte[2];

        client.uploadObject(defaultBucketName, S3Client.toObjectKey(path), bytes);

        persistProvider.delete(path);

        assertFalse(persistProvider.exists(path));
    }

    @Test
    public void list_path_valid() throws IOException, InterruptedException {
        assumeFalse("Skipping the test on Windows OS.", OSUtils.isWindows());
        final Path path = Path.of("home/test/file");
        byte[] bytes = new byte[2];

        client.uploadObject(defaultBucketName, S3Client.toObjectKey(path), bytes);

        List<Path> list = persistProvider.list(Path.of("home"));
        System.out.println(list.toString());
        assertTrue(list.contains(Path.of("home/test").toAbsolutePath()));
    }

    @Test
    public void copyTest() throws InterruptedException, IOException {
        assumeFalse("Skipping test on windows", OSUtils.isWindows());
        final Path src = Path.of("home/test/src");
        final Path dst = Path.of("home/test/dst");
        persistProvider.write(src.getParent(), src, src.toString().getBytes(StandardCharsets.UTF_8));
        assertEquals(persistProvider.read(src), src.toString());
        // Since we're not overwriting anything, return false
        assertFalse(persistProvider.copy(src, dst, true));
        assertEquals(persistProvider.read(dst), src.toString());
        // Since we're overwriting something, return true
        assertTrue(persistProvider.copy(src, dst, true));
    }

    @Test
    public void isDir_path_valid() throws InterruptedException {
        assumeFalse("Skipping the test on Windows OS.", OSUtils.isWindows());
        final Path path = Path.of("home/test/file");
        byte[] bytes = new byte[2];

        client.uploadObject(defaultBucketName, S3Client.toObjectKey(path), bytes);

        assertTrue(persistProvider.isDir(Path.of("home/test")));
        assertFalse(persistProvider.isDir(Path.of("home/test/file")));
    }

    @Test
    public void isFile_path_valid() throws InterruptedException {
        assumeFalse("Skipping the test on Windows OS.", OSUtils.isWindows());
        final Path path = Path.of("home/test/file");
        byte[] bytes = new byte[2];

        client.uploadObject(defaultBucketName, S3Client.toObjectKey(path), bytes);
        System.out.println(persistProvider.exists(Path.of("home/test/file")));
        System.out.println(persistProvider.isDir(Path.of("home/test/file")));
        assertTrue(persistProvider.isFile(Path.of("home/test/file")));
    }
}
