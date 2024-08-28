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
package com.ericsson.adp.mgmt.bro.api.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.bro.api.fragment.FragmentInformation;
import com.ericsson.adp.mgmt.bro.api.grpc.GrpcServiceIntegrationTest;
import com.ericsson.adp.mgmt.bro.api.registration.SoftwareVersion;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.bro.api.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.DataInterfaceGrpc.DataInterfaceImplBase;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;
import com.google.protobuf.ByteString;

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;

public class RestoreActionsTest extends GrpcServiceIntegrationTest {

    private static final String RESTORED_FILE_NAME = "restoredFile.txt";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Agent agent;
    private DataService dataService;
    private ControlService controlService;
    private RestoreActions restoreActions;
    private RestoreInformation restoreInformation;

    private final List<Fragment> fragmentList = Arrays.asList(getFragment());

    @Before
    public void setUp() {
        final OrchestratorGrpcChannel orchGrpcChannel = new OrchestratorGrpcChannel(channel);

        this.agent = new Agent(new TestAgentBehavior(), orchGrpcChannel);

        orchGrpcChannel.establishControlChannel(new TestOrchestratorStreamObserver(agent));

        restoreInformation = new RestoreInformation("Backup", getSoftwareInfo(), fragmentList, "DEFAULT");
        restoreActions = new RestorePreparationActions(agent, restoreInformation);
    }

    @Test
    public void getBackupName_backupName() {
        assertEquals("Backup", restoreActions.getBackupName());
    }

    @Test
    public void getBackupName_backupType() {
        assertEquals("DEFAULT", restoreActions.getBackupType());
    }

    @Test
    public void getFragmentList_fragmentList() {
        final List<FragmentInformation> fragmentInformations = new ArrayList<>(restoreActions.getFragmentList());
        assertEquals("id", fragmentInformations.get(0).getFragmentId());
        assertEquals("size", fragmentInformations.get(0).getSizeInBytes());
        assertEquals("version", fragmentInformations.get(0).getVersion());
    }

    @Test
    public void getSoftwareVersion_softwareVersion() {
        final SoftwareVersion softwareVersion = restoreActions.getSoftwareVersion();
        assertEquals("Name", softwareVersion.getProductName());
        assertEquals("Number", softwareVersion.getProductNumber());
        assertEquals("Description", softwareVersion.getDescription());
    }

    @Test
    public void downloadFragment_restoreLocation_fragmentDownloaded() throws Exception {
        final List<FragmentInformation> fragmentInformations = new ArrayList<>(restoreActions.getFragmentList());
        restoreActions.downloadFragment(fragmentInformations.get(0), getRestoreLocation().toString());

        assertEquals(Arrays.asList("bytes"), Files.readAllLines(getRestoreLocation().resolve(RESTORED_FILE_NAME)));
    }

    @Test
    public void sendStageComplete_success_stageCompleteTrue() {
        restoreActions.sendStageComplete(true, "success");
        assertTrue(controlService.getMessage().getStageComplete().getSuccess());
    }

    @Override
    protected List<BindableService> getServices() {
        dataService = new DataService();
        controlService = new ControlService();
        return Arrays.asList(dataService, controlService);
    }

    private class DataService extends DataInterfaceImplBase {

        @Override
        public void restore(final Metadata request, final StreamObserver<RestoreData> responseObserver) {
            sendValidBackupFile(responseObserver);
        }

    }

    private void sendValidBackupFile(final StreamObserver<RestoreData> responseObserver) {
        sendBackupFileName(RESTORED_FILE_NAME, responseObserver);
        sendBackupFileContent(ByteString.copyFrom("bytes".getBytes()), responseObserver);
        sendBackupChecksum(getChecksum(), responseObserver);
        responseObserver.onCompleted();
    }

    private void sendBackupFileName(final String fileName, final StreamObserver<RestoreData> responseObserver) {
        responseObserver.onNext(RestoreData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setFileName(fileName).build()).build());
    }

    private void sendBackupFileContent(final ByteString bytes, final StreamObserver<RestoreData> responseObserver) {
        responseObserver.onNext(RestoreData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setContent(bytes).build()).build());
    }

    private void sendBackupChecksum(final String checksum, final StreamObserver<RestoreData> responseObserver) {
        responseObserver.onNext(RestoreData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setChecksum(checksum).build()).build());
    }

    private String getChecksum() {
        final ChecksumCalculator calculator = new ChecksumCalculator();
        calculator.addBytes("bytes".getBytes());
        return calculator.getChecksum();
    }

    private Path getRestoreLocation() {
        return this.folder.getRoot().toPath();
    }

    private SoftwareVersionInfo getSoftwareInfo() {
        return SoftwareVersionInfo.newBuilder()
                .setProductName("Name")
                .setProductNumber("Number")
                .setDescription("Description")
                .setRevision("Revision")
                .setProductionDate("Date")
                .setType("Type")
                .build();
    }

    private Fragment getFragment() {
        return Fragment.newBuilder()
                .setFragmentId("id")
                .setSizeInBytes("size")
                .setVersion("version")
                .build();
    }

}