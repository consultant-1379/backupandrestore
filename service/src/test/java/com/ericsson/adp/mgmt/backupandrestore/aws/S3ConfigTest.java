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
package com.ericsson.adp.mgmt.backupandrestore.aws;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static com.amazonaws.services.s3.internal.SkipMd5CheckStrategy.DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY;
import static com.amazonaws.services.s3.internal.SkipMd5CheckStrategy.DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.ericsson.adp.mgmt.backupandrestore.exception.AWSException;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumHash64;
import io.findify.s3mock.S3Mock;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;


public class S3ConfigTest {
    private S3Mock api;

    private static S3Config s3Config;

    @Before
    public void setup() {
        //set up the mock server
        api = new S3Mock.Builder().withPort(28000).withInMemoryBackend().build();
        api.start();
        final KeyStoreService service = EasyMock.createMock(KeyStoreService.class);
        expect(service.getKeyStore(KeyStoreAlias.OSMN)).andReturn(null).anyTimes();
        replay(service);
        s3Config = new S3Config();
        s3Config.setConnectionReadTimeOut(2000);
        s3Config.setConnectionTimeout(1000);
        s3Config.setRetriesOperation(2);
        s3Config.setRetriesStartUp(2);
        s3Config.setKeyStoreService(service);
    }

    @After
    public void tearDown() {
        api.shutdown();
    }

    @Test
    public void getAWSClient_valid() {
        s3Config.setEnabled(true);
        s3Config.setEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(
                        "http://localhost:28000",
                        "eu-west-3"
                )
        );
        s3Config.setCredentials(new BasicAWSCredentials("ak", "sk"));
        AmazonS3 awsClient = s3Config.getAWSClient();
        assertEquals("eu-west-3", awsClient.getRegion().toString());
    }

    @Test(expected = AWSException.class)
    public void get_AWSClient_invalid() {
        s3Config.setEnabled(false);
        s3Config.getAWSClient();
    }

    @Test
    public void setSkipMd5Checksum_true () {
        s3Config.setSkipMd5Checksum(true);
        assertNotNull(System.getProperty(DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY));
        assertNotNull(System.getProperty(DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY));
    }

    @Test
    public void setSkipMd5Checksum_false () {
        s3Config.setSkipMd5Checksum(false);
        assertNull(System.getProperty(DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY), null);
        assertNull(System.getProperty(DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY), null);
    }

    @Test
    public void set_credentials_valid(){
        s3Config.setCredentials(new BasicAWSCredentials("ak","sk"));
        assertEquals("ak", s3Config.getAccessKey());
    }

    @Test(expected = AWSException.class)
    public void set_credentials_with_null() {
        s3Config.setCredentials(null);
    }

    @Test
    public void get_credentials_null() {
        s3Config.setEnabled(true);
        s3Config.setHost("http://localhost");
        s3Config.setPort("28000");
        s3Config.setRegion("eu-west-3");
        s3Config.setSecretKeyName("sk");
        s3Config.setAccessKeyName("ak");
        AmazonS3 awsClient = s3Config.getAWSClient();
        assertEquals("eu-west-3", awsClient.getRegion().toString());
    }

    @Test
    public void set_endpoint_configuration_valid() {
        s3Config.setEndpointConfiguration(new EndpointConfiguration(
                "localhost:28000","eu-west-3"
        ));
        assertEquals("eu-west-3", s3Config.getRegion());
        assertEquals("localhost", s3Config.getHost());
        assertEquals("28000", s3Config.getPort());
    }

    @Test(expected = AWSException.class)
    public void set_endpoint_configuration_null() {
        s3Config.setEndpointConfiguration(null);
    }

    @Test
    public void setEnabled_true_valid() {
        s3Config.setEnabled(true);
        assertTrue(s3Config.isEnabled());
    }

    @Test
    public void setRegion_isBlank_default() {
        s3Config.setRegion("");
        assertEquals(Regions.DEFAULT_REGION.getName(), s3Config.getRegion());
    }

    @Test
    public void setRegion_nonBlank_valid() {
        s3Config.setRegion("eu-2");
        assertEquals("eu-2", s3Config.getRegion());
    }


    @Test
    public void setPort_port_valid() {
        s3Config.setPort("28000");
        assertEquals("28000", s3Config.getPort());
    }

    @Test
    public void setHost_host_valid() {
        s3Config.setHost("localhost");
        assertEquals("localhost", s3Config.getHost());
    }

    @Test
    public void setDefaultBucketName_name_valid() {
        s3Config.setDefaultBucketName("bro");
        assertEquals("bro", s3Config.getDefaultBucketName());
    }

    @Test
    public void getEndPoint_endpoint_valid() {
        s3Config.setHost("localhost");
        s3Config.setPort("36000");
        String endpoint = s3Config.getEndPoint();
        assertEquals("localhost:36000", endpoint);
    }

    @Test
    public void loadCredentialsFromFiles() throws IOException {
        final Path credDir = setupSecretFiles("accessKey", "accessKeyContents", "secretKey", "secretKeyContents", true, true);
        s3Config = new S3Config();
        s3Config.setCredentialsDir(credDir.toString());
        s3Config.setAccessKeyName("accessKey");
        s3Config.setSecretKeyName("secretKey");
        assertEquals(s3Config.getAccessKey(), "accessKeyContents");
        assertEquals(s3Config.getSecretKey(), "secretKeyContents");
        cleanup(credDir);
    }

    @Test
    public void loadCredentialsFromFiles_badAccess() throws IOException {
        final Path credDir = setupSecretFiles("accessKey", "accessKeyContents", "secretKey", "secretKeyContents", false, true);
        s3Config = new S3Config();
        s3Config.setCredentialsDir(credDir.toString());
        s3Config.setAccessKeyName("accessKey");
        s3Config.setSecretKeyName("secretKey");
        // If we fail to read the file, we should fall back on using the name directly to support backwards compatibility
        assertEquals(s3Config.getAccessKey(), "accessKey");
        assertEquals(s3Config.getSecretKey(), "secretKeyContents");
        cleanup(credDir);
    }

    @Test
    public void loadCredentialsFromFiles_badSecret() throws IOException {
        final Path credDir = setupSecretFiles("accessKey", "accessKeyContents", "secretKey", "secretKeyContents", true, false);
        s3Config = new S3Config();
        s3Config.setCredentialsDir(credDir.toString());
        s3Config.setAccessKeyName("accessKey");
        s3Config.setSecretKeyName("secretKey");
        // If we fail to read the file, we should fall back on using the name directly to support backwards compatibility
        assertEquals(s3Config.getAccessKey(), "accessKeyContents");
        assertEquals(s3Config.getSecretKey(), "secretKey");
        cleanup(credDir);
    }

    @Test
    public void loadCredentialsFromFiles_badBoth() throws IOException {
        final Path credDir = setupSecretFiles("accessKey", "accessKeyContents", "secretKey", "secretKeyContents", false, false);
        s3Config = new S3Config();
        s3Config.setCredentialsDir(credDir.toString());
        s3Config.setAccessKeyName("accessKey");
        s3Config.setSecretKeyName("secretKey");
        // If we fail to read the file, we should fall back on using the name directly to support backwards compatibility
        assertEquals(s3Config.getAccessKey(), "accessKey");
        assertEquals(s3Config.getSecretKey(), "secretKey");
        cleanup(credDir);
    }

    private Path setupSecretFiles(final String accessKeyName,
                                  final String accessKeyContents,
                                  final String secretKeyName,
                                  final String secretKeyContents,
                                  final boolean makeAccessKey,
                                  final boolean makeSecretKey) throws IOException {
        final ChecksumHash64 hasher = new ChecksumHash64();
        final String timestamp = OffsetDateTime.now().toString();
        final byte[] bytes = timestamp.getBytes(StandardCharsets.UTF_8);
        hasher.updateHash64(bytes, bytes.length);
        final Path credDir = Path.of(System.getProperty("java.io.tmpdir")).resolve(hasher.getStringValue());
        System.out.println("Creating temp directory: " + credDir);
        assertTrue(credDir.toFile().mkdirs());
        if (makeAccessKey) {
            Files.write(credDir.resolve(accessKeyName), accessKeyContents.getBytes(StandardCharsets.UTF_8));
        }
        if (makeSecretKey) {
            Files.write(credDir.resolve(secretKeyName), secretKeyContents.getBytes(StandardCharsets.UTF_8));
        }
        return credDir;
    }

    private void cleanup(final Path toClean) throws IOException {
        System.out.println("Cleaning up directory: " + toClean);
        Files.walk(toClean)
                .map(Path::toFile)
                .sorted((o1, o2) -> -o1.compareTo(o2))
                .forEach(f -> { assertTrue(f.delete()); });
    }
}
