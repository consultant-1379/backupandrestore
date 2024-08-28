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
package com.ericsson.adp.mgmt.backupandrestore.restore;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.control.Preparation;
import com.ericsson.adp.mgmt.metadata.Fragment;

public class RestoreInformationTest {

    private Backup backup;
    private BackupManager backupManager;
    private FragmentFileService fragmentFileService;
    private Agent agent;

    @Before
    public void setup() {
        backup = createMock(Backup.class);
        fragmentFileService = createMock(FragmentFileService.class);
        backupManager = createMock(BackupManager.class);
        agent = createMock(Agent.class);

        expect(backup.getBackupId()).andReturn("backupTest").anyTimes();
        expect(backup.getSoftwareVersions()).andReturn(getSoftwareVersions()).anyTimes();
        expect(backupManager.getBackupManagerId()).andReturn("brm").anyTimes();
        expect(backupManager.getAgentVisibleBRMId()).andReturn("brm").anyTimes();
        replay(backup, backupManager);
    }

    @Test
    public void buildPreparationMessage_returnRestorePreparationMessage() throws Exception {
        final RestoreInformation restoreInformation = new RestoreInformation(backup, backupManager, "agentId2", fragmentFileService, Optional.of(agent));
        expect(fragmentFileService.getFragments("brm", "backupTest", "agentId2")).andReturn(getFragments());
        expect(agent.getApiVersion()).andReturn(ApiVersion.API_V4_0).times(2);
        replay(agent, fragmentFileService);
        final Preparation message = restoreInformation.buildPreparationMessage();

        assertEquals("backupTest", message.getBackupName());
        assertEquals("productName2", message.getSoftwareVersionInfo().getProductName());
        assertEquals("description2", message.getSoftwareVersionInfo().getDescription());
        assertEquals("productNumber2", message.getSoftwareVersionInfo().getProductNumber());
        assertEquals("productRevision2", message.getSoftwareVersionInfo().getRevision());
        assertEquals("type2", message.getSoftwareVersionInfo().getType());
        assertEquals("date2", message.getSoftwareVersionInfo().getProductionDate());
        assertEquals("brm", message.getBackupType());
        assertEquals("CommercialVersion", message.getSoftwareVersionInfo().getCommercialVersion());
        assertEquals("SemanticVersion", message.getSoftwareVersionInfo().getSemanticVersion());
        assertEquals(1, message.getFragmentCount());
    }

    @Test
    public void buildPreparationMessage_returnRestorePreparationMessage_forV3CreatedBackup() throws Exception {
        final RestoreInformation restoreInformation = new RestoreInformation(backup, backupManager, "agentId", fragmentFileService, Optional.of(agent));
        expect(fragmentFileService.getFragments("brm", "backupTest", "agentId")).andReturn(getFragments());
        expect(agent.getApiVersion()).andReturn(ApiVersion.API_V4_0).times(2);
        replay(agent, fragmentFileService);
        final Preparation message = restoreInformation.buildPreparationMessage();

        assertEquals("backupTest", message.getBackupName());
        assertEquals("productName1", message.getSoftwareVersionInfo().getProductName());
        assertEquals("description1", message.getSoftwareVersionInfo().getDescription());
        assertEquals("productNumber1", message.getSoftwareVersionInfo().getProductNumber());
        assertEquals("productRevision1", message.getSoftwareVersionInfo().getRevision());
        assertEquals("type1", message.getSoftwareVersionInfo().getType());
        assertEquals("date1", message.getSoftwareVersionInfo().getProductionDate());
        assertEquals("brm", message.getBackupType());
        assertEquals("", message.getSoftwareVersionInfo().getCommercialVersion());
        assertEquals("", message.getSoftwareVersionInfo().getSemanticVersion());
        assertEquals(1, message.getFragmentCount());
    }

    @Test
    public void buildPreparationMessage_returnRestorePreparationMessagev3() throws Exception {
        final RestoreInformation restoreInformation = new RestoreInformation(backup, backupManager, "agentId", fragmentFileService, Optional.of(agent));
        expect(fragmentFileService.getFragments("brm", "backupTest", "agentId")).andReturn(getFragments());
        expect(agent.getApiVersion()).andReturn(ApiVersion.API_V3_0).times(2);

        replay(agent, fragmentFileService);
        final Preparation message = restoreInformation.buildPreparationMessage();

        assertEquals("backupTest", message.getBackupName());
        assertEquals("productName1", message.getSoftwareVersionInfo().getProductName());
        assertEquals("description1", message.getSoftwareVersionInfo().getDescription());
        assertEquals("productNumber1", message.getSoftwareVersionInfo().getProductNumber());
        assertEquals("productRevision1", message.getSoftwareVersionInfo().getRevision());
        assertEquals("type1", message.getSoftwareVersionInfo().getType());
        assertEquals("date1", message.getSoftwareVersionInfo().getProductionDate());
        assertEquals("brm", message.getBackupType());
        assertEquals("", message.getSoftwareVersionInfo().getCommercialVersion());
        assertEquals("", message.getSoftwareVersionInfo().getSemanticVersion());
        assertEquals(2, message.getFragmentCount());
    }

    @Test
    public void buildPreparationMessage_returnRestorePreparation() throws Exception {
        final RestoreInformation restoreInformation = new RestoreInformation(backup, backupManager, "agentId", fragmentFileService, Optional.of(agent));
        expect(fragmentFileService.getFragments("brm", "backupTest", "agentId")).andReturn(getFragments());
        expect(agent.getApiVersion()).andReturn(ApiVersion.API_V3_0).times(2);

        replay(agent, fragmentFileService);
        final Preparation message = restoreInformation.buildPreparationMessage();

        assertEquals("backupTest", message.getBackupName());
        assertEquals("productName1", message.getSoftwareVersionInfo().getProductName());
        assertEquals("description1", message.getSoftwareVersionInfo().getDescription());
        assertEquals("productNumber1", message.getSoftwareVersionInfo().getProductNumber());
        assertEquals("productRevision1", message.getSoftwareVersionInfo().getRevision());
        assertEquals("type1", message.getSoftwareVersionInfo().getType());
        assertEquals("date1", message.getSoftwareVersionInfo().getProductionDate());
        assertEquals("brm", message.getBackupType());
        assertEquals(2, message.getFragmentCount());
    }

    private List<Fragment> getFragments() {
        final List<Fragment> fragments = new ArrayList<>();
        fragments.add(Fragment.newBuilder().setFragmentId("F1").setSizeInBytes("123").setVersion("V1").build());
        fragments.add(Fragment.newBuilder().setFragmentId("F2").setSizeInBytes("123").setVersion("V2").build());
        return fragments;
    }

    private List<SoftwareVersion> getSoftwareVersions() {
        final List<SoftwareVersion> softwareVersions = new ArrayList<>();
        final SoftwareVersion softwareVersion1 = new SoftwareVersion();
        softwareVersion1.setAgentId("agentId");
        softwareVersion1.setDate("date1");
        softwareVersion1.setDescription("description1");
        softwareVersion1.setProductName("productName1");
        softwareVersion1.setProductNumber("productNumber1");
        softwareVersion1.setProductRevision("productRevision1");
        softwareVersion1.setType("type1");
        softwareVersions.add(softwareVersion1);
        final SoftwareVersion softwareVersion2 = new SoftwareVersion();
        softwareVersion2.setAgentId("agentId2");
        softwareVersion2.setDate("date2");
        softwareVersion2.setDescription("description2");
        softwareVersion2.setProductName("productName2");
        softwareVersion2.setProductNumber("productNumber2");
        softwareVersion2.setProductRevision("productRevision2");
        softwareVersion2.setType("type2");
        softwareVersion2.setCommercialVersion("CommercialVersion");
        softwareVersion2.setSemanticVersion("SemanticVersion");
        softwareVersions.add(softwareVersion2);
        return softwareVersions;
    }
}
