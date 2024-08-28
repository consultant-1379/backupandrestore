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
package com.ericsson.adp.mgmt.backupandrestore.aws.service;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.Bucket;
import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.exception.AWSException;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;
import com.ericsson.adp.mgmt.backupandrestore.util.OSUtils;
import io.findify.s3mock.S3Mock;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class S3MultipartClientTest {
    private S3Mock api;

    private static S3MultipartClient s3MultipartClient;
    private static String defaultBucketName;
    private static S3Config s3Config;
    
    @Before
    public void setup() {
        //set up the mock server
        api = new S3Mock.Builder().withPort(28001).withInMemoryBackend().build();
        api.start();
        defaultBucketName = "bro";
        s3Config = new S3Config();
        s3Config.setEnabled(true);
        s3Config.setDefaultBucketName(defaultBucketName);
        s3Config.setHost("http://0.0.0.0");
        s3Config.setPort("28001");
        s3Config.setAccessKeyName("ak");
        s3Config.setSecretKeyName("sk");
        s3Config.setConnectionTimeout(1000);
        s3Config.setRetriesStartUp(0);
        s3Config.setRetriesOperation(1);

        final KeyStoreService service = EasyMock.createMock(KeyStoreService.class);
        expect(service.getKeyStore(KeyStoreAlias.OSMN)).andReturn(null).anyTimes();
        replay(service);
        s3Config.setKeyStoreService(service);

        s3MultipartClient = new S3MultipartClient(s3Config);
        s3MultipartClient.createBucket();
    }

    @After
    public void tearDown() {
        api.shutdown();
    }

    @Test
    public void createBucket_bucketName_created() {
        final String bucketName = "bro-first-bucket";
        s3MultipartClient.createBucket(bucketName);
        assertTrue(s3MultipartClient.getS3Client().doesBucketExistV2(bucketName));
    }

    @Test
    public void createBucket_noInput_created() {
        assertTrue(s3MultipartClient.getS3Client().doesBucketExistV2(defaultBucketName));
    }

    @Test(expected = AWSException.class)
    public void createBucket_operationRequest_s3_stop() {
        final String bucketName = "bro-no-bucket";
        s3MultipartClient.invalidateCachedClient();
        api.stop();
        // api service stops but not shutdown so S3 service returns 404 here and raises AWSException
        s3MultipartClient.createBucket(bucketName);
    }

    @Test
    public void getBucket_bucketName_bucket() {
        final String bucketName = "bro-second-bucket";
        s3MultipartClient.createBucket(bucketName);
        Bucket bucket = s3MultipartClient.getBucket(bucketName).get();
        assertEquals(bucketName, bucket.getName());

    }

    @Test
    public void removeBucket_bucketName_removed() {
        final String bucketName = "bro-third-bucket";
        s3MultipartClient.createBucket(bucketName);
        assertTrue(s3MultipartClient.isBucketEmpty(bucketName));
        s3MultipartClient.removeBucket(bucketName);
        assertFalse(s3MultipartClient.getS3Client().doesBucketExistV2(bucketName));
    }

    @Test(expected = AmazonServiceException.class)
    public void removeBucket_operationRequest_invalidName() {
        final String bucketName = "bro-no-bucket";
        s3MultipartClient.invalidateCachedClient();
        s3MultipartClient.removeBucket(bucketName);
    }

    @Test
    public void getBucketLocation_bucketName_location() {
        final String bucketName = "bro-fourth-bucket";
        s3MultipartClient.createBucket(bucketName);
        String location = s3MultipartClient.getBucketLocation(bucketName);
        assertEquals("US", location);
    }


    @Test
    public void uploadObject_content_saved() throws IOException, InterruptedException {
        final String bucketName = defaultBucketName;
        final String objectName = "object1";
        final File tempFile = File.createTempFile("bro", ".test");
        s3MultipartClient.uploadObject(bucketName, objectName, tempFile);
        assertTrue(s3MultipartClient.getObjectList(bucketName,"").contains(objectName));

        InputStream inputStream = new FileInputStream(tempFile);
        final String objectName2 = "object2";
        s3MultipartClient.uploadObject(bucketName, objectName2, inputStream);
        assertTrue(s3MultipartClient.getObjectList(bucketName,"").contains(objectName2));

        byte[] bytes = new byte[2];
        final String objectName3 = "object3";
        s3MultipartClient.uploadObject(bucketName, objectName3, bytes);
        assertTrue(s3MultipartClient.getObjectList(bucketName, "").contains(objectName3));

        tempFile.deleteOnExit();
    }


    @Test
    public void getObjectList_bucketName_list() throws InterruptedException{
        final String objectKey = "home/file/text";
        byte[] bytes = new byte[2];
        s3MultipartClient.uploadObject(defaultBucketName, objectKey, bytes);

        List<String> list = s3MultipartClient.getObjectList(defaultBucketName, "");
        assertTrue(list.contains(objectKey));
    }

    @Test
    public void getObjectList_bucketName_orderby_list() throws InterruptedException{
        final String objectKey = "home/file/textz";
        final String objectKey2 = "home/file/atext";
        final String objectKey3 = "home/file/1text";
        byte[] bytes = new byte[2];
        s3MultipartClient.uploadObject(defaultBucketName, objectKey2, bytes);
        s3MultipartClient.uploadObject(defaultBucketName, objectKey, bytes);
        s3MultipartClient.uploadObject(defaultBucketName, objectKey3, bytes);
         List<String> list = s3MultipartClient.getObjectListOrder(defaultBucketName, "");
        assertTrue(list.get(0).equals(objectKey2));
        assertTrue(list.get(1).equals(objectKey));
        assertTrue(list.get(2).equals(objectKey3));
    }

    @Test
    public void getObjectList_prefix_valid() throws InterruptedException{
        final String objectKey = "home/file/text";
        byte[] bytes = new byte[2];
        s3MultipartClient.uploadObject(defaultBucketName, objectKey, bytes);

        List<String> list = s3MultipartClient.getObjectList(defaultBucketName, objectKey);
        assertTrue(list.contains(objectKey));

        List<String> list2 = s3MultipartClient.getObjectList(defaultBucketName, "home/");
        assertTrue(list2.contains(objectKey));
    }

    @Test
    public void emptyBucket_bucketName_valid() throws InterruptedException{
        final String objectKey = "home/file/text";
        byte[] bytes = new byte[2];
        s3MultipartClient.uploadObject(defaultBucketName, objectKey, bytes);
        assertFalse(s3MultipartClient.isBucketEmpty(defaultBucketName));

        s3MultipartClient.emptyBucket(defaultBucketName);
        assertTrue(s3MultipartClient.isBucketEmpty(defaultBucketName));
    }

    @Test
    public void downloadObject_objectKey_file() throws IOException, InterruptedException {
        final String objectKey = "home/file/text";
        TemporaryFolder temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();

        byte[] bytes = new byte[2];
        s3MultipartClient.uploadObject(defaultBucketName, objectKey, bytes);
        Path path = s3MultipartClient.downloadObject(objectKey, temporaryFolder.getRoot().toPath());

        assertEquals(temporaryFolder.getRoot().toPath().resolve(objectKey).toString(), path.toString());
        temporaryFolder.delete();
    }
    
    @Test
    public void removeObject_objectKey_valid() throws InterruptedException {
        final String objectKey = "home/file/text";
        byte[] bytes = new byte[]{1, 2};
        s3MultipartClient.uploadObject(defaultBucketName, objectKey, bytes);
        assertTrue(s3MultipartClient.isObjectExist(defaultBucketName, objectKey));

        s3MultipartClient.removeObject(defaultBucketName, objectKey);
        assertFalse(s3MultipartClient.isObjectExist(defaultBucketName, objectKey));
    }

    @Test
    public void getDefaultBucketName_noInput_validName(){
        assertEquals(defaultBucketName, s3MultipartClient.getDefaultBucketName());
    }

    @Test
    public void getObejctSize_objectKey_valid() throws InterruptedException{
        final String objectKey = "home/file/text";
        byte[] bytes = new byte[]{1, 2};
        s3MultipartClient.uploadObject(defaultBucketName, objectKey, bytes);
        assertEquals(2, s3MultipartClient.getObjectSize(defaultBucketName, objectKey));
    }

    @Test
    public void downloadObject_objectKey_path() throws IOException, InterruptedException {
        final String objectKey = "home/file/text";
        TemporaryFolder temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();

        byte[] bytes = new byte[2];
        s3MultipartClient.uploadObject(defaultBucketName, objectKey, bytes);
        Path path = s3MultipartClient.downloadObject(defaultBucketName, objectKey, temporaryFolder.getRoot().toPath());

        assertEquals(temporaryFolder.getRoot().toPath().resolve(objectKey).toString(), path.toString());
    }

    @Test
    public void adjustMinPartSize_fileSize_valid(){
        long fileSize = 40L * 1024 * 1024 * 1024;
        long minPartSize = 5 * 1024 * 1024;
        assertEquals(minPartSize, S3MultipartClient.getChunkSize(fileSize));
        assertEquals(minPartSize * 10, S3MultipartClient.getChunkSize(fileSize * 10));
        assertEquals(minPartSize * 100, S3MultipartClient.getChunkSize(fileSize * 100));
    }

    @Test(expected = AWSException.class)
    public void adjustMinPartSize_bigfileSize_exception() throws IOException {
        long fileSize = 6L * 1024 * 1024 * 1024 * 1024;
        s3MultipartClient = new S3MultipartClient(s3Config);
        // This causes a BufferedAWSOutputStream to be constructed, causing an exception to be thrown based on file size
        final OutputStream stream = s3MultipartClient.getOutputStream("test", fileSize);
    }

    @Test
    public void initMultipartUpload_normalSize_valid() throws IOException {
        final String objectKey = "object4";
        byte[] bytes = new byte[6 * 1024 * 1024];
        final OutputStream stream = s3MultipartClient.getOutputStream(objectKey);
        stream.write(bytes);
        stream.close();
        assertTrue(s3MultipartClient.getObjectList(defaultBucketName, "").contains(objectKey));
    }

    @Test
    public void initMultipartUpload_smallSize_valid() throws IOException {
        final String objectKey = "object4";
        byte[] bytes = new byte[4 * 1024 * 1024];
        final OutputStream stream = s3MultipartClient.getOutputStream(objectKey);
        stream.write(bytes);
        stream.close();
        assertTrue(s3MultipartClient.getObjectList(defaultBucketName, "").contains(objectKey));
    }

    @Test
    public void toObjectKey_path_valid() {
        assumeFalse("Skipping the test on Windows OS.", OSUtils.isWindows());
        Path path = Paths.get("/a/b/c").toAbsolutePath().normalize();
        assertEquals("a/b/c", S3Client.toObjectKey(path));
        assertEquals(path, S3Client.fromObjectKey(S3Client.toObjectKey(path)));
    }

    @Test
    public void downloadObject_objectKey_steam() throws InterruptedException, IOException {
        final String objectKey = "home/file/text";
        byte[] bytes = new byte[1024 * 1024 * 50 + 1234];
        bytes[0] = 1;
        bytes[bytes.length / 2] = 5;
        bytes[bytes.length -1] = 7;
        s3MultipartClient.uploadObject(defaultBucketName, objectKey, bytes);

        InputStream inputStream = s3MultipartClient.downloadObject(defaultBucketName, objectKey);
        byte[] bytes1 = inputStream.readAllBytes();

        assertEquals(bytes1[0], bytes[0]);
        assertEquals(bytes1[bytes1.length / 2], bytes[bytes.length / 2]);
        assertEquals(bytes1[bytes1.length -1], bytes[bytes.length -1]);
    }
}
