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
import java.util.List;

import com.ericsson.adp.mgmt.control.FragmentListEntry;
import com.ericsson.adp.mgmt.control.Preparation;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

/**
 * Holds restore information
 */
public class RestoreInformation extends ActionInformation {

    private final SoftwareVersionInfo softwareVersionInfo;
    private final List<Fragment> fragmentList;

    /**
     * @param backupName Name of backup to be restored
     * @param softwareVersionInfo software version info
     * @param fragmentList List of fragments to be restored
     * @param backupType the type of the backup to be restored
     */
    public RestoreInformation(final String backupName, final SoftwareVersionInfo softwareVersionInfo, final List<Fragment> fragmentList,
                              final String backupType) {
        super(backupName, backupType);
        this.softwareVersionInfo = softwareVersionInfo;
        this.fragmentList = fragmentList;
    }

    /**
     * @param preparation message.
     */
    public RestoreInformation(final Preparation preparation) {
        super(preparation);
        this.softwareVersionInfo = preparation.getSoftwareVersionInfo();
        final List<Fragment> fragmentList = preparation.getFragmentList();
        if (fragmentList.size() == 1 && fragmentList.get(0).getSerializedSize() == 0) {
            this.fragmentList = new ArrayList<>();
        } else {
            this.fragmentList = fragmentList;
        }
    }

    public SoftwareVersionInfo getSoftwareVersionInfo() {
        return softwareVersionInfo;
    }

    public List<Fragment> getFragmentList() {
        return fragmentList;
    }

    /**
     * Add fragment to list.
     * @param fragment fragment.
     */
    public void addFragment(final FragmentListEntry fragment) {
        fragmentList.add(fragment.getFragment());
    }
}
