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
package com.ericsson.adp.mgmt.bro.api.grpc;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.google.protobuf.ByteString;

public class CustomMetadataFileMessageBuilderTest {

    private BackupMessageBuilder builder;

    @Before
    public void setup() {
        builder = new CustomMetadataFileMessageBuilder();
    }

    @Test
    public void getFileNameMessage_fileName_backupDataMessageWithFileName() throws Exception {
        final BackupData expectedMessage = BackupData
                .newBuilder()
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setFileName("abc"))
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .build();

        assertEquals(expectedMessage, builder.getFileNameMessage("abc"));
    }

    @Test
    public void getDataMessage_byteString_backupDataMessageWithBytes() throws Exception {
        final BackupData expectedMessage = BackupData
                .newBuilder()
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setContent(ByteString.copyFromUtf8("aaaaaaaaaaaaaa")))
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .build();

        assertEquals(expectedMessage, builder.getDataMessage(ByteString.copyFromUtf8("aaaaaaaaaaaaaa")));
    }

    @Test
    public void getChecksumMessage_checksum_backupDataMessageWithChecksum() throws Exception {
        final BackupData expectedMessage = BackupData
                .newBuilder()
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setChecksum("qwe"))
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .build();

        assertEquals(expectedMessage, builder.getChecksumMessage("qwe"));
    }


}
