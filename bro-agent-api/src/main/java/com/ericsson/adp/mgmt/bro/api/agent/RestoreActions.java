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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.bro.api.exception.FailedToDownloadException;
import com.ericsson.adp.mgmt.bro.api.fragment.FragmentInformation;
import com.ericsson.adp.mgmt.bro.api.registration.SoftwareVersion;
import com.ericsson.adp.mgmt.bro.api.service.RestoreService;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

/**
 * Provides means to perform various restore actions
 * Provides a function to indicate that a restore stage is complete
 * Provides methods to access restore information
 */
public abstract class RestoreActions {

    private static final Logger log = LogManager.getLogger(RestoreActions.class);
    protected final Agent agent;
    protected final RestoreInformation restoreInformation;

    /**
     * @param agent Agent participating in restore
     * @param restoreInformation required for restore
     */
    public RestoreActions(final Agent agent, final RestoreInformation restoreInformation) {
        this.agent = agent;
        this.restoreInformation = restoreInformation;
    }

    /**
     * Once all the actions in the stage is completed call this method to inform the Orchestrator that the stage has completed
     *
     * @param success Inform the Orchestrator if the stage was successful or not
     * @param message Inform the Orchestrator why something went wrong or just that all is well
     */
    public void sendStageComplete(final boolean success, final String message) {
        this.agent.sendStageCompleteMessage(success, message, Action.RESTORE);
    }

    /**
     * Method to download a fragment to be restored
     * @param fragment is instance of FragmentData containing information needed to download a fragment
     * @param restoreLocation The location to download a fragment to.
     * @throws FailedToDownloadException when there occurs an issue while downloading fragment
     */
    public void downloadFragment(final FragmentInformation fragment, final String restoreLocation) throws FailedToDownloadException {
        final RestoreService restoreService = new RestoreService(restoreLocation);
        final Iterator<RestoreData> restoreDataIterator = this.agent.getRestoreDataIterator(metadataBuilder(fragment));
        try {
            restoreService.download(restoreDataIterator);
        } catch (final Exception e) {
            log.error("An exception occured when trying to download fragment <{}> of backup <{}>",
                    fragment.getFragmentId(), restoreInformation.getBackupName());
            throw e;
        }
    }

    /**
     * Provides the name of the backup that is being restored.
     * @return backupName
     */
    public String getBackupName() {
        return this.restoreInformation.getBackupName();
    }

    /**
     * Provides the backup Type of the backup that is being restored.
     * @return backupType
     */
    public String getBackupType() {
        return restoreInformation.getBackupType();
    }

    /**
     * Provides list of fragments available to restore
     * @return list of partial fragment information
     */
    public List<FragmentInformation> getFragmentList() {
        final List<FragmentInformation> fragmentInformations = new ArrayList<>();
        this.restoreInformation.getFragmentList().stream().forEach(fragment -> fragmentInformations.add(setFragment(fragment)));
        return fragmentInformations;
    }

    /**
     * Provides software version of the backup to be restored. This should be used for validation. Call restore complete with success set to false if
     * this version is incompatible
     * @return Software Version details
     */
    public SoftwareVersion getSoftwareVersion() {
        final SoftwareVersionInfo softwareVersionInfo = this.restoreInformation.getSoftwareVersionInfo();

        final SoftwareVersion softwareVersion = new SoftwareVersion();

        softwareVersion.setProductName(softwareVersionInfo.getProductName());
        softwareVersion.setProductNumber(softwareVersionInfo.getProductNumber());
        softwareVersion.setRevision(softwareVersionInfo.getRevision());
        softwareVersion.setDescription(softwareVersionInfo.getDescription());
        softwareVersion.setType(softwareVersionInfo.getType());
        softwareVersion.setProductionDate(softwareVersionInfo.getProductionDate());

        return softwareVersion;
    }

    private Metadata metadataBuilder(final FragmentInformation fragment) {
        return Metadata.newBuilder()
                .setAgentId(this.agent.getAgentId())
                .setBackupName(this.restoreInformation.getBackupName())
                .setFragment(getFragment(fragment))
                .build();
    }

    private Fragment getFragment(final FragmentInformation fragment) {
        return Fragment
                .newBuilder()
                .setFragmentId(fragment.getFragmentId())
                .setSizeInBytes(fragment.getSizeInBytes())
                .setVersion(fragment.getVersion())
                .putAllCustomInformation(fragment.getCustomInformation())
                .build();
    }

    /**
     * Converting Fragment obtained from Orchestrator into PartialFragmentInformation to hide grpc details
     * @param fragment obtained from Orchestrator
     * @return PartialFragmentInformation instance containing fragment details obtained from orchestrator
     */
    private FragmentInformation setFragment(final Fragment fragment) {
        final FragmentInformation fragmentInformation = new FragmentInformation();
        fragmentInformation.setFragmentId(fragment.getFragmentId());
        fragmentInformation.setSizeInBytes(fragment.getSizeInBytes());
        fragmentInformation.setVersion(fragment.getVersion());
        fragmentInformation.setCustomInformation(fragment.getCustomInformationMap());
        return fragmentInformation;
    }

}
