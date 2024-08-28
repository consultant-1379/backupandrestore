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
package com.ericsson.adp.mgmt.backupandrestore.rest;

import com.ericsson.adp.mgmt.backupandrestore.action.yang.YangActionService;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangBackupNameActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangSftpServerActionRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for Yang actions.
 */
@RestController
public class YangActionsController extends V2Controller {
    private static final Logger log = LogManager.getLogger(YangActionsController.class);

    @Autowired
    private YangActionService yangActionService;

    /**
     * Creates an action to create a backup.
     * @param request action input.
     * @return id of action.
     */
    @PutMapping("ericsson-brm:brm::backup-manager::create-backup")
    public YangActionResponse createBackup(@RequestBody final YangBackupNameActionRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("Yang request create-backup: <{}>", request);
        }
        return new YangActionResponse(yangActionService.createBackup(request).getActionId());
    }

    /**
     * Creates an action to delete a backup.
     * @param request action input.
     * @return id of action.
     */
    @PutMapping("ericsson-brm:brm::backup-manager::delete-backup")
    public YangActionResponse deleteBackup(@RequestBody final YangBackupNameActionRequest request) {
        return new YangActionResponse(yangActionService.deleteBackup(request).getActionId());
    }

    /**
     * Creates an action to import a backup.
     * @param request action input.
     * @return id of action.
     */
    @PutMapping("ericsson-brm:brm::backup-manager::import-backup")
    public YangActionResponse importBackup(@RequestBody final YangSftpServerActionRequest request) {
        return new YangActionResponse(yangActionService.importBackup(request).getActionId());
    }

    /**
     * Creates an action to restore a backup.
     * @param request action input.
     * @return id of action.
     */
    @PutMapping("ericsson-brm:brm::backup-manager::backup::restore")
    public YangActionResponse restore(@RequestBody final YangActionRequest request) {
        return new YangActionResponse(yangActionService.restore(request).getActionId());
    }

    /**
     * Creates an action to export a backup.
     * @param request action input.
     * @return id of action.
     */
    @PutMapping("ericsson-brm:brm::backup-manager::backup::export")
    public YangActionResponse export(@RequestBody final YangSftpServerActionRequest request) {
        return new YangActionResponse(yangActionService.export(request).getActionId());
    }

}
