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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.nacm.NACMRole;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.nacm.NACMRoleType;
import com.ericsson.adp.mgmt.backupandrestore.exception.CMMediatorException;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;

/**
 * Patch to add NACM Roles in ietf-netconf-acm configuration.
 */
public class NACMRolePatch extends ConfigurationPatch {

    private static final String NACM_PREFIX = "/ietf-netconf-acm:nacm/rule-list/";

    private final String nacmRoleType;
    private final JsonService jsonService;

    /**
     * Creates Patch.
     * @param operation to be performed.
     * @param roleType NACM role
     * @param position where the patch needs to be added
     * @param jsonService instance
     */
    public NACMRolePatch(final PatchOperation operation, final NACMRoleType roleType, final int position, final JsonService jsonService) {
        super(operation, NACM_PREFIX + position);
        this.nacmRoleType = roleType.getCmRepresentation();
        this.jsonService = jsonService;
    }

    @Override
    protected List<PatchOperationJson> getJsonOfOperations() {
        final Optional<NACMRole> patch = jsonService.parseJsonFromClassPathResource(nacmRoleType + ".json", NACMRole.class);

        if (!patch.isPresent()) {
            throw new CMMediatorException("Failed to generate NACM patch");
        }

        return Arrays.asList(createOperationJson(path, patch.get()));
    }

}
