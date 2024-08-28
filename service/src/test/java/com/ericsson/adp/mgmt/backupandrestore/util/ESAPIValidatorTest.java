/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidBackupNameException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidURIException;

public class ESAPIValidatorTest {

    private ESAPIValidator esapiValidator;
    private Random random;
    private static final int BACKUP_NAME_INVALID_LENGTH = 256;
    private static final int URI_INVALID_LENGTH = 2001;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        esapiValidator = new ESAPIValidator();
        random = new Random(1234567890123456789L);
    }

    @Test
    public void validateAlphaNumeric_valid() {
        for(final String name : Arrays.asList("A", "a", "Z", "z", "0", "9", "_", "-","SampleSftpServer")) {
            assertTrue(esapiValidator.isAlphaNumeric(name));
        }
    }

    @Test
    public void validateAlphaNumeric_invalid() {
        final String[] rejectChars = "+ . * ? [ ] % $ ( ) ; < > / ~ \\ , ' ! `".split(" ");
        for(final String name : rejectChars) {
            assertFalse(esapiValidator.isAlphaNumeric(name));
        }
    }

    @Test
    public void validateBackupName_validBackupName_validBackupName() {
        final String backupName = "my_backup-1234.tar";
        esapiValidator.validateBackupName(backupName);
        assertTrue(esapiValidator.isValidBackupName(backupName));
    }

    @Test(expected = InvalidBackupNameException.class)
    public void validateBackupName_invalidBackupName_throwsException() {
        final String backupName = "</mybackup100w45pz4p><script>alert(1);</script><mybackup100w45pz4p>";
        assertFalse(esapiValidator.isValidURI(backupName));
        esapiValidator.validateBackupName(backupName);
    }

    @Test(expected = InvalidBackupNameException.class)
    public void validateBackupName_invalidBackupNameExceedsMaxLength_throwsException() {
        final String backupName = generateString();
        assertFalse(esapiValidator.isValidBackupName(backupName));
        esapiValidator.validateBackupName(backupName);
    }

    @Test
    public void validateBackupName_verifyCertainCharsRejected_AllThrowException() {
        final String[] rejectChars = "* ? [ ] % $ ( ) ; < > / ~ \\ , ' ! `".split(" ");
        for( final String rejectChar : rejectChars ) {
            try {
                assertFalse(esapiValidator.isValidBackupName(rejectChar));
                esapiValidator.validateBackupName(rejectChar);
            } catch (final InvalidBackupNameException e) {
                continue;
            }
            fail(); //Exception wasn't thrown, fail the test.
        }
    }

    @Test
    public void validateURI_validSftpUri_valid() throws URISyntaxException {
        new ArrayList<String>();
        for (final String uri : Arrays.asList(
                "sftp://user@localhost:22/my/path",
                "sftp://user@localhost:22/home/user/backups",
                "sftp://user@somewhere.location.server:22/home/user/backups",
                "sftp://user@127.0.0.1:22/home/user/backups",
                "sftp://user@[0000:0000:0000:0000:0000:0000:0000:0001]:22/home/user/backups",
                "sftp://user@[::1]:22/home/user/backups",
                //From RFC 2732 IPv6 Literal Addresses in URL's December 1999
                "sftp://user@[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80/index.html",
                "sftp://user@[1080:0:0:0:8:800:200C:417A]/index.html",
                "sftp://user@[3ffe:2a00:100:7031::1]",
                "sftp://user@[1080::8:800:200C:417A]/foo",
                "sftp://user@[::192.9.5.5]/ipng",
                "sftp://user@[::FFFF:129.144.52.38]:80/index.html",
                "sftp://user@[2010:836B:4179::836B:4179]",
                "sftp://us-er@localhost:22/home/user/backup-name",
                "sftp://user@localhost:22/home-area/user-area/backup-name",
                "sftp://us_er@localhost:22/home-area/user-area/backup-name"

                )) {
            esapiValidator.validateURI(new URI(uri));
            assertTrue(esapiValidator.isValidURI(uri));
        }
    }

    @Test
    public void validateURI_validHttpUri_valid() throws URISyntaxException {
        for (final String uri : Arrays.asList(
                "http://domain/my/path",
                "http://domain/my/path?tag=config",
                "http://localhost:8080/backups/mybackup",
                "http://localhost:8080/backups/mybackup?tag=config&user=op",
                "http://somewhere.location.server:8080/home/user/backups",
                "http://somewhere.location.server:8080/home/user/backups?tag=user",
                "http://127.0.0.1:8080/home/user/backups",
                "http://127.0.0.1:8080/home/user/backups?user=operator",
                "http://[0000:0000:0000:0000:0000:0000:0000:0001]:8080/home/user/backups",
                "http://[::1]:8080/home/user/backup?tag=config",
                "http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80/index.html",
                "http://[1080::8:800:200C:417A]/foo",
                "http://[::192.9.5.5]/backup?tag=config",
                "http://[2010:836B:4179::836B:4179]"
                )) {
            esapiValidator.validateURI(new URI(uri));
            assertTrue(esapiValidator.isValidURI(uri));
        }
    }

    @Test(expected = InvalidURIException.class)
    public void validateURI_invalidUriExceedsMaxLength_throwsException() throws URISyntaxException {
        final URI invalidURI = new URI(generateBadUriString());
        assertFalse(esapiValidator.isValidURI(invalidURI.toString()));
        esapiValidator.validateURI(invalidURI);
    }

    @Test
    public void validatePrivateKey_validPEMFormat() throws NoSuchAlgorithmException, IOException {
        for(final String algorithm  : Arrays.asList("DSA", "RSA")) {
            final String rsaKey = getPemPrivateKey(algorithm, "OPENSSL");
            assertTrue(esapiValidator.isValidOpenSSLPrivateKeyFormat(rsaKey));
        }
    }

    @Test
    public void validatePrivateKey_invalidFormat() throws NoSuchAlgorithmException, IOException {
        for(final String format  : Arrays.asList("ENCRYPTED", "SSH2")) {
            final String rsaKey = getPemPrivateKey("RSA", format);
            assertFalse(esapiValidator.isValidOpenSSLPrivateKeyFormat(rsaKey));
        }
    }

    private String getPemPrivateKey(final String algorithm, String format) throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm);
        KeyPair pair = generator.generateKeyPair();
        PrivateKey privateKey = pair.getPrivate();

        if(format.equals("OPENSSL")) {
            format = algorithm;
        }

        PemObject pem = new PemObject(format + " PRIVATE KEY", privateKey.getEncoded());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        PemWriter writer = new PemWriter(new OutputStreamWriter(out));
        writer.writeObject(pem);
        writer.close();
        return new String(out.toByteArray());
    }

    private String generateString() {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < BACKUP_NAME_INVALID_LENGTH; i++) {
            stringBuilder.append(getRandomChar());
        }
        return stringBuilder.toString();
    }

    private String generateBadUriString() throws URISyntaxException {
        final String path = "/path";

        final StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("sftp://user@host:22");

        while (strBuilder.length() < URI_INVALID_LENGTH) {
            strBuilder.append(path);
        }

        return strBuilder.toString();
    }

    private char getRandomChar() {
        return (char) ('a' + random.nextInt(26));
    }

}
