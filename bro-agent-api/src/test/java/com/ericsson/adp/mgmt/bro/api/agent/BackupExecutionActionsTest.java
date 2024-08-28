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
package com.ericsson.adp.mgmt.bro.api.agent;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.data.BackupData;

import io.grpc.stub.StreamObserver;

public class BackupExecutionActionsTest {

    private BackupExecutionActions actions;
    private StreamStub streamStub;

    @Before
    public void setUp() {
        streamStub = new StreamStub();
        final Agent agent = createMock(Agent.class);

        expect(agent.getBackupStream()).andReturn(streamStub);
        expect(agent.getAgentId()).andReturn("id");

        expect(agent.getBackupStream()).andReturn(streamStub);
        expect(agent.getAgentId()).andReturn("id");

        final ActionInformation actionInformation = createMock(ActionInformation.class);
        expect(actionInformation.getBackupName()).andReturn("myBackup");
        expect(actionInformation.getBackupType()).andReturn("DEFAULT");

        replay(agent, actionInformation);

        actions = new BackupExecutionActions(agent, actionInformation);
    }

    @Test
    public void sendBackup_fragmentInformation_sendsBackupDataToBackupStream() throws Exception {
        actions.sendBackup(getFragmentInformation());
        assertEquals(7, streamStub.getMessages().size());
    }

    @Test
    public void getBackupName_backupName() {
        assertEquals("myBackup", actions.getBackupName());
    }

    @Test
    public void getBackupName_backupType() {
        assertEquals("DEFAULT", actions.getBackupType());
    }

    private BackupFragmentInformation getFragmentInformation() {
        final BackupFragmentInformation fragmentInformation = new BackupFragmentInformation();

        fragmentInformation.setBackupFilePath("src/test/resources/backup.txt");
        fragmentInformation.setCustomMetadataFilePath(Optional.ofNullable("src/test/resources/CustomMetadata.txt"));
        fragmentInformation.setFragmentId("1");
        fragmentInformation.setSizeInBytes("2");
        fragmentInformation.setVersion("3");

        return fragmentInformation;
    }

    private class StreamStub implements StreamObserver<BackupData> {

        private final List<BackupData> messages = new ArrayList<>();

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(final Throwable arg0) {

        }

        @Override
        public void onNext(final BackupData arg0) {
            this.messages.add(arg0);
        }

        public List<BackupData> getMessages() {
            return messages;
        }

    }

}
