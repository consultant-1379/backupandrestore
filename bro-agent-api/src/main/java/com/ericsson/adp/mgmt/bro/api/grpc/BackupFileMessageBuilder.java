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

import java.util.function.Consumer;

import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.BackupFileChunk.Builder;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.google.protobuf.ByteString;

/**
 * Builds messages to send backup file through Data Channel.
 */
public class BackupFileMessageBuilder implements BackupMessageBuilder {

    @Override
    public BackupData getFileNameMessage(final String fileName) {
        return generateMessage(backupFileChunkBuilder -> backupFileChunkBuilder.setFileName(fileName));
    }

    @Override
    public BackupData getDataMessage(final ByteString data) {
        return generateMessage(backupFileChunkBuilder -> backupFileChunkBuilder.setContent(data));
    }

    @Override
    public BackupData getChecksumMessage(final String checksum) {
        return generateMessage(backupFileChunkBuilder -> backupFileChunkBuilder.setChecksum(checksum));
    }

    private BackupData generateMessage(final Consumer<Builder> setMessageInformationFunction) {
        final Builder messageBuilder = BackupFileChunk.newBuilder();
        setMessageInformationFunction.accept(messageBuilder);
        return wrapMessage(messageBuilder.build());
    }

    private BackupData wrapMessage(final BackupFileChunk chunk) {
        return BackupData
                .newBuilder()
                .setBackupFileChunk(chunk)
                .setDataMessageType(DataMessageType.BACKUP_FILE)
                .build();
    }

}
