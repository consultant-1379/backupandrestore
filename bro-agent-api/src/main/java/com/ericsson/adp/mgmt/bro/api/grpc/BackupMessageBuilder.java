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

import com.ericsson.adp.mgmt.data.BackupData;
import com.google.protobuf.ByteString;

/**
 * Builds messages to be sent on Data Channel.
 */
public interface BackupMessageBuilder {

    /**
     * Builds a message holding a file name.
     * @param fileName to be sent.
     * @return message.
     */
    BackupData getFileNameMessage(String fileName);

    /**
     * Builds a message holding bytes.
     * @param data to be sent.
     * @return message.
     */
    BackupData getDataMessage(ByteString data);

    /**
     * Builds a message holding checksum.
     * @param checksum to be sent.
     * @return message.
     */
    BackupData getChecksumMessage(String checksum);

}
