/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.CLIENT_LOCAL_PRIVATE_KEY;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.CLIENT_LOCAL_PUBLIC_KEY;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.SERVER_LOCAL_HOST_KEY;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ClientIdentity;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerInformation;
import com.ericsson.adp.mgmt.backupandrestore.kms.CMKeyPassphraseService;
import com.ericsson.adp.mgmt.backupandrestore.rest.SftpServerTestConstant;

public class SftpServerValidatorTest {
    private static final int HOST_ADDRESS_MAX_NODE_LENGTH = 63;
    private SftpServerValidator sftpServerValidator;
    private CMKeyPassphraseService cmKeyPassphraseService;
    private Random random;

    @Before
    public void setUp() {
        sftpServerValidator = new SftpServerValidator();
        sftpServerValidator.setEsapiValidator(new ESAPIValidator());
        sftpServerValidator.setIdValidator(new IdValidator());
        cmKeyPassphraseService = EasyMock.createMock(CMKeyPassphraseService.class);
        sftpServerValidator.setCmKeyPassphraseService(cmKeyPassphraseService);
        random = new Random();
    }

    @Test
    public void validateSftpServer_valid() {
        final SftpServerInformation sftpServer = SftpServerTestConstant.createSftpServerInformation();
        assertTrue(sftpServerValidator.isValid(sftpServer));
    }

    @Test
    public void validateSftpServer_invalidUserName() {
        final SftpServerInformation sftpServer = SftpServerTestConstant.createSftpServerInformation();
        final ClientIdentity clientIdentity = sftpServer.getEndpoints().getEndpoint()[0].getClientIdentity();
        for (final String username : Arrays.asList("", null, "foo/bar", "sample@name", "$foo", "myName!")) {
            clientIdentity.setUsername(username);
            final String errorMessage = String.format("SFTP Server validation failed for username %s", username);
            assertFalse(errorMessage, sftpServerValidator.isValid(sftpServer));
        }
    }

    @Test
    public void validateAlphaNumeric_valid() {
        for(final String name : Arrays.asList("A", "a", "Z", "z", "0", "9", "_", "-", "SampleSftpServer")) {
            final String errorMessage = String.format("Alphanumeric validation failed for %s", name);
            assertTrue(errorMessage, sftpServerValidator.isAlphaNumericField("testField", name));
        }
    }

    @Test
    public void validateAlphaNumeric_invalid() {
        final String[] rejectChars = "+ . * ? [ ] % $ ( ) : ; < > / ~ \\ , ' ! ` @".split(" ");
        for(final String name : rejectChars) {
            final String errorMessage = String.format("Alphanumeric validation failed for %s", name);
            assertFalse(errorMessage, sftpServerValidator.isAlphaNumericField("testField", name));
        }
    }

    @Test
    public void testHostAddress_validHostNames() {
        for (final String hostAddress : Arrays.asList(
                "localhost",
                "somewhere.location.server",
                "somewhere.location9.server",
                "somewhere.location.server8",
                "1969-where.loca-tion.ser-ver",
                "somewhere.location.server.com")) {
            final String errorMessage = String.format("Hostname validation failed for %s", hostAddress);
            assertTrue(errorMessage, sftpServerValidator.isHostAddress(hostAddress));
        }
    }

    @Test
    public void testHostAddress_invalidHostNames() {
        for (final String hostAddress : Arrays.asList(
                "",
                " ",
                "localhost-",
                "somewhere-.location.server",
                "somewhere.location-.server",
                "somewhere.location.server-")) {
            final String errorMessage = String.format("Hostname validation failed for %s", hostAddress);
            assertFalse(errorMessage, sftpServerValidator.isHostAddress(hostAddress));
        }
    }

    @Test
    public void testHostAddress_validHostNameLength() {
        for (final String hostAddress : Arrays.asList(
                generateHostNameNode(HOST_ADDRESS_MAX_NODE_LENGTH),
                generate253CharLongHostName())) {
            final String errorMessage = String.format("Hostname length validation failed for %s", hostAddress);
            assertTrue(errorMessage, sftpServerValidator.isHostAddress(hostAddress));
        }
    }

    @Test
    public void testHostAddress_invalidHostNameLength() {
        for (final String hostAddress : Arrays.asList(
                generateHostNameNode(HOST_ADDRESS_MAX_NODE_LENGTH + 1),
                generate254CharLongHostName())) {
            final String errorMessage = String.format("Hostname length validation failed for %s", hostAddress);
            assertFalse(errorMessage, sftpServerValidator.isHostAddress(hostAddress));
        }
    }

    @Test
    public void testHostAddress_validIPv4Addresses() {
        for (final String hostAddress : Arrays.asList(
                "127.0.0.1",
                "0.0.0.0",
                "192.168.5.6",
                "255.255.255.255")) {
            final String errorMessage = String.format("IPv4 Address validation failed for %s", hostAddress);
            assertTrue(errorMessage, sftpServerValidator.isHostAddress(hostAddress));
        }
    }

    @Test
    public void testHostAddress_validIPv6Addresses() {
        for (final String hostAddress : Arrays.asList(
                "0000:0000:0000:0000:0000:0000:0000:0001",
                "fedc:ba98:7654:3210:fedc:ba98:7654:3210",
                "::1",
                //From RFC 2732 Format for Literal IPv6 Addresses in URL's
                "FEDC:BA98:7654:3210:FEDC:BA98:7654:3210",
                "1080:0:0:0:8:800:200C:417A",
                "3ffe:2a00:100:7031::1",
                "1080::8:800:200C:417A",
                "::192.9.5.5",
                "::FFFF:129.144.52.38",
                "2010:836B:4179::836B:4179")) {
            final String errorMessage = String.format("IPv6 Address validation failed for %s", hostAddress);
            assertTrue(errorMessage, sftpServerValidator.isHostAddress(hostAddress));
        }
    }

    @Test
    public void testHostAddress_invalidIPv4Addresses() {
        for (final String hostAddress : Arrays.asList(
                "127,0.0.1",
                "192:168.0.1",
                "192.168.0.1/8",
                "192.168.0.1:22")) {
            final String errorMessage = String.format("IPv4 Address validation failed for %s", hostAddress);
            assertFalse(errorMessage, sftpServerValidator.isHostAddress(hostAddress));
        }
    }

    @Test
    public void testHostAddress_invalidIPv6Addresses() {
        for (final String hostAddress : Arrays.asList(
                "0000/0000:0000:0000:0000:0000:0000:0001",
                ";:1",
                "FEDCBA98:7654:3210:FEDC:BA98:7654:3210:FEDC",
                "[FEDCBA98:7654:3210:FEDC:BA98:7654:3210]",
                "1080:0&0:0:8:800:200C:417A",
                ":192.9.5.5",
                ":FFFF:129.144.52.38")) {
            final String errorMessage = String.format("IPv6 Address validation failed for %s", hostAddress);
            assertFalse(errorMessage, sftpServerValidator.isHostAddress(hostAddress));
        }
    }

    @Test
    public void testHostAddress_invalidAsIPAddress_butValidAsHostName() {
        for (final String hostAddress : Arrays.asList(
                "127.0.0",
                "256.256.256.256",
               "FEDCBA98.7654.3210.FEDC.BA98.7654.3210.FEDC")) {
            final String errorMessage = String.format("Host name validation failed for %s", hostAddress);
            assertTrue(errorMessage, sftpServerValidator.isHostAddress(hostAddress));
        }
    }

    @Test
    public void testHostAddress_invalidCharacters() {
        final String[] rejectChars = "- + . * ? [ ] % $ ( ) ; < > / ~ \\ , ' ! `".split(" ");
        for(final String name : rejectChars) {
            final String errorMessage = String.format("Host address validation failed for %s", name);
            assertFalse(errorMessage, sftpServerValidator.isHostAddress(name));
        }
    }

    @Test
    public void testRemotePath_validRemotePath() {
        for (final String path : Arrays.asList("upload",
                                               "1969",
                                               "my/path",
                                               "my/foo/uploads",
                                               "my/foo-bar/uploads",
                                               "home_area/foo/")) {
            final String errorMessage = String.format("Remote path validation failed for %s", path);
            assertTrue(errorMessage, sftpServerValidator.isRemotePath(path));
        }
    }

    @Test
    public void testRemotePath_validRemotePathCharacters() {
        final String[] acceptedChars = "- . ? : ' / + = & ; % $ # _ , ".split(" ");
        for (final String path : acceptedChars) {
            final String errorMessage = String.format("Remote path validation failed for %s", path);
            assertTrue(errorMessage, sftpServerValidator.isRemotePath(path));
        }
    }

    @Test
    public void testRemotePath_invalidRemotePathCharacters() {
        final String[] rejectedChars = "£  ^ * ~ @ | \\ \\\\ \" [ ] { } ( )".split(" ");
        for (final String path : rejectedChars) {
            final String errorMessage = String.format("Remote path validation failed for %s", path);
            assertFalse(errorMessage, sftpServerValidator.isRemotePath(path));
        }
    }

    @Test
    public void testRemotePath_invalidRemotePathNullandEmptyValue() {
       final String[] rejectedChars = {"", null};
        for (final String path : rejectedChars) {
            final String errorMessage = String.format("Remote path validation failed for %s", path);
            assertFalse(errorMessage, sftpServerValidator.isRemotePath(path));
        }
    }

    @Test
    public void testPort_validRemotePort() {
        final String[] acceptedPorts = "0 1 65534 65535".split(" ");
        for (final String port : acceptedPorts) {
            final String errorMessage = String.format("Remote port validation failed for %s", port);
            assertTrue(errorMessage, sftpServerValidator.isRemotePort(Integer.parseInt(port)));
        }
    }

    @Test
    public void testPort_invalidRemotePort() {
        final String[] rejectedPorts = "-1 65536".split(" ");
        for (final String port : rejectedPorts) {
            final String errorMessage = String.format("Remote port validation failed for %s", port);
            assertFalse(errorMessage, sftpServerValidator.isRemotePort(Integer.parseInt(port)));
        }
    }

    @Test
    public void testPrivateKey_invalidFormat() {
      expect(cmKeyPassphraseService.isEnabled()).andReturn(true);
      final String encryptedPrivateKey = "encryptedPrivateKey";
      final String keyWithNonOpenSSHFormat = SftpServerTestConstant.encodeBase64("-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
              + "aDummyPrivateKeyContent\n"
              + "-----END ENCRYPTED PRIVATE KEY-----\r\n");
      expect(cmKeyPassphraseService.getPassphrase(encryptedPrivateKey)).andReturn(keyWithNonOpenSSHFormat);
      replay(cmKeyPassphraseService);
      assertFalse(sftpServerValidator.isValidKeyCredentials(CLIENT_LOCAL_PRIVATE_KEY.toString(), encryptedPrivateKey));
    }

    @Test
    public void testPublicKey_invalidKeyStructure() {
        final List<String> invalidPublicKeys = List.of("ssh-rsa", "ssh-rsa base64encodedcontent optionalcomment invalidextracomment");
        for(final String key : invalidPublicKeys) {
            final String base64EncodedKey = SftpServerTestConstant.encodeBase64(key);
            final String errorMessage = String.format("Public key validation failed for %s", key);
            assertFalse(errorMessage, sftpServerValidator.isValidKeyCredentials(CLIENT_LOCAL_PUBLIC_KEY.toString(), base64EncodedKey));
        }

    }

    @Test
    public void testPublicKey_unSupportedAlgorithms() {
        final List<String> invalidPublicKeys = List.of("x25519 AAAAB3NzaC1yc2EAAAADAQ",
                                                       "x448 AAAAB3NzaC1yc2EAAAADAQ",
                                                       "ssh-dsa AAAAB3NzaC1yc2EAAAADAQ",
                                                       "ssh-rsaa AAAAB3NzaC1yc2EAAAADAQ",
                                                       "ecdsa AAAAB3NzaC1yc2EAAAADAQ",
                                                       "ed25519 AAAAB3NzaC1yc2EAAAADAQ");
        for(final String key : invalidPublicKeys) {
            final String base64EncodedKey = SftpServerTestConstant.encodeBase64(key);
            final String errorMessage = String.format("SSH Host key algorithm validation failed for %s", key);
            assertFalse(errorMessage, sftpServerValidator.isValidKeyCredentials(SERVER_LOCAL_HOST_KEY.toString(), base64EncodedKey));
        }

    }

    @Test
    public void testPublicKey_validBase64EncodedContent() {
        final List<String> validPublicKeys = List.of("ssh-dss AAAAB3NzaC1kc3MAAACBAKJfEF3EYxzk08vkyjuxv0S/ dummykey",
                                                     "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAHgQDY9kEQ5O1Mg dummykey",
                                                     "ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyN dummykey",
                                                     "ecdsa-sha2-nistp384 AAAAE2VjZHNhLXNoYTItbmlzdHAzO dummykey",
                                                     "ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHA1M dummykey",
                                                     "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIPtiK0nn dummykey");
        for(final String key : validPublicKeys) {
            final String base64EncodedKey = SftpServerTestConstant.encodeBase64(key);
            final String errorMessage = String.format("SSH Host key base64 encoded content validation failed for %s", key);
            assertTrue(errorMessage, sftpServerValidator.isValidKeyCredentials(SERVER_LOCAL_HOST_KEY.toString(), base64EncodedKey));
        }
    }

    @Test
    public void testPublicKey_invalidBase64EncodedContent() {
        final String aKeyDeclaringRSAButContainingECDSA = "ssh-rsa AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEA";
        final List<String> invalidPublicKeys = List.of("ssh-rsa =", "ssh-rsa 123456", "ssh-rsa [B@522bbb7", "ssh-rsa ?", "ssh-rsa !", aKeyDeclaringRSAButContainingECDSA);
        for(final String key : invalidPublicKeys) {
            final String base64EncodedKey = SftpServerTestConstant.encodeBase64(key);
            final String errorMessage = String.format("SSH Host key base64 encoded content validation failed for %s", key);
            assertFalse(errorMessage, sftpServerValidator.isValidKeyCredentials(SERVER_LOCAL_HOST_KEY.toString(), base64EncodedKey));
        }

    }

    private String generate254CharLongHostName() {
        return generate253CharLongHostName() + "a";
    }

    private String generate253CharLongHostName() {
        return new StringBuilder()
                .append(generateHostNameNode(HOST_ADDRESS_MAX_NODE_LENGTH))
                .append(".")
                .append(generateHostNameNode(HOST_ADDRESS_MAX_NODE_LENGTH))
                .append(".")
                .append(generateHostNameNode(HOST_ADDRESS_MAX_NODE_LENGTH))
                .append(".")
                .append(generateHostNameNode(HOST_ADDRESS_MAX_NODE_LENGTH-2))
                .toString();
    }

    private String generateHostNameNode(final int length) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if(length >= 3 && i == length/2) {
                stringBuilder.append("-");
            } else {
                stringBuilder.append(getRandomChar());
            }
        }
        return stringBuilder.toString();
    }

    private char getRandomChar() {
        return (char) ('a' + random.nextInt(26));
    }

}
