/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.action.yang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;

public class YangSftpServerActionRequestTest {

    private JsonService jsonService;

    @Before
    public void setUp() {
        jsonService = new JsonService();
    }

    @Test
    public void testDeserialization_YangURIInput() {
        final String uriiInput = formatJson("{'input':"
                + "{'ericsson-brm:uri':'testURI', 'ericsson-brm:password':'password'},"
                + "'context':'/ericsson-brm:brm/backup-manager/3'}");
        YangSftpServerActionRequest request = jsonService.parseJsonString(uriiInput, YangSftpServerActionRequest.class).get();
        final YangURIInput input = (YangURIInput) request.getInput();
        assertEquals("testURI", input.getUri().toString());
        assertEquals("password", input.getPassword());
    }

    @Test
    public void testDeserialization_YangSftpServerNameExportInput() {
        final String serverName = formatJson("{'input':"
                + "{'ericsson-brm:sftp-server-name':'testServer'},"
                + "'context':'/ericsson-brm:brm/backup-manager/3'}");
        YangSftpServerActionRequest request = jsonService.parseJsonString(serverName, YangSftpServerActionRequest.class).get();
        final YangSftpServerNameInput input = (YangSftpServerNameInput) request.getInput();
        assertEquals("testServer", input.getSftpServerName());
        assertNull(input.getBackupPath());
    }

    @Test
    public void testDeserialization_YangSftpServerNameImportInput() {
        final String serverNameInput = formatJson("{'input':"
                + "{'ericsson-brm:sftp-server-name':'testServer',"
                + "'ericsson-brm:backup-path':'myBackup'},"
                + "'context':'/ericsson-brm:brm/backup-manager/3'}");
        YangSftpServerActionRequest request = jsonService.parseJsonString(serverNameInput, YangSftpServerActionRequest.class).get();
        final YangSftpServerNameInput input = (YangSftpServerNameInput) request.getInput();
        assertEquals("testServer", input.getSftpServerName());
        assertEquals("myBackup", input.getBackupPath());
   }

    private String formatJson(String input) {
        return input.replaceAll("'", "\"");
    }
}
