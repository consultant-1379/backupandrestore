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
package com.ericsson.adp.mgmt.brotestagent.test;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.adp.mgmt.bro.api.agent.RestoreExecutionActions;
import com.ericsson.adp.mgmt.bro.api.exception.FailedToDownloadException;
import com.ericsson.adp.mgmt.bro.api.fragment.FragmentInformation;
import com.ericsson.adp.mgmt.bro.api.registration.SoftwareVersion;
import com.ericsson.adp.mgmt.brotestagent.agent.SoftwareVersionInformationUtils;

public class RestoreExecutionActionsStub extends RestoreExecutionActions {

    private final String backupName;
    private final List<FragmentInformation> downloadedFragments = new ArrayList<>();
    private boolean shouldThrowException;
    private boolean successful;
    private String message;
    private String restoreLocationString;
    private final String backupType;

    public RestoreExecutionActionsStub(final String backupName, final String backupType) {
        super(null, null);
        this.backupName = backupName;
        this.backupType = backupType;
    }

    public RestoreExecutionActionsStub(final String backupName, final boolean shouldThrowException, final String backupType) {
        this(backupName, backupType);
        this.shouldThrowException = shouldThrowException;
    }

    @Override
    public SoftwareVersion getSoftwareVersion() {
        return SoftwareVersionInformationUtils.getSoftwareVersion();
    }

    @Override
    public void downloadFragment(final FragmentInformation fragment, final String restoreLocation) throws FailedToDownloadException {
        if (shouldThrowException) {
            throw new FailedToDownloadException("oops");
        }
        this.downloadedFragments.add(fragment);
        this.restoreLocationString = restoreLocation;
    }

    @Override
    public List<FragmentInformation> getFragmentList() {
        FragmentInformation fragment;
        final List<FragmentInformation> fragmentsForRestore = new ArrayList<FragmentInformation>();

        for(int i=0; i<3; i++) {
            fragment = new FragmentInformation();
            fragment.setFragmentId("X_" + i);
            fragment.setSizeInBytes("sizeInBytes");
            fragment.setVersion("0.0.0");
            fragmentsForRestore.add(fragment);
        }

        return fragmentsForRestore;
    }

    @Override
    public String getBackupName() {
        return this.backupName;
    }

    @Override
    public String getBackupType() {
        return this.backupType;
    }

    @Override
    public void sendStageComplete(final boolean success, final String message) {
        this.successful = success;
        this.message = message;
    }

    public List<FragmentInformation> getDownloadedFragments() {
        return this.downloadedFragments;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public String getRestoreLocationString() {
        return this.restoreLocationString;
    }
}
