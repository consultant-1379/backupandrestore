/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.HousekeepingFileService;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.DEFAULT_MAX_BACKUP;

import java.util.function.Consumer;

/**
 * Represents housekeeping information
 */
public class Housekeeping extends HousekeepingInformation {

    public static final String AUTO_DELETE = "auto-delete";
    public static final String HOUSE_KEEPING = "housekeeping";

    private final String backupManagerId;
    private final Consumer<Housekeeping> persistFunction;

    /**
     * Default housekeeping information when a backup manager is created
     * @param backupManagerId of the backupManager the housekeeping belongs to
     * @param persistFunction to persist housekeeping
     */
    public Housekeeping(final String backupManagerId, final Consumer<Housekeeping> persistFunction) {
        super(AUTO_DELETE_ENABLED, DEFAULT_MAX_BACKUP);
        this.backupManagerId = backupManagerId;
        this.persistFunction = persistFunction;
    }

    /**
     * Constructor based in an existing housekeeping
     * @param housekeeping existing housekeeping object to duplicate
     */
    public Housekeeping(final Housekeeping housekeeping) {
        autoDelete = housekeeping.getAutoDelete();
        backupManagerId = housekeeping.getBackupManagerId();
        maxNumberBackups = housekeeping.getMaxNumberBackups();
        persistFunction = housekeeping.getPersistFunction();
        setVersion(housekeeping.getVersion());
    }
    /**
     * @param maxNumberBackups allowed for a backup manager
     * @param autoDelete enable or disable autoDelete feature
     * @param backupManagerId of the backupManager the housekeeping belongs to
     * @param persistFunction to persist housekeeping
     */
    public Housekeeping(final int maxNumberBackups, final String autoDelete,
                        final String backupManagerId, final Consumer<Housekeeping> persistFunction) {
        super(autoDelete, maxNumberBackups);
        this.backupManagerId = backupManagerId;
        this.persistFunction = persistFunction;
    }

    public String getBackupManagerId() {
        return backupManagerId;
    }

    public Consumer<Housekeeping> getPersistFunction() {
        return persistFunction;
    }

    /**
     * Reload this housekeeping object from persistence layer, in place
     * @param fileService used to load data from persistence layer
     * */
    public void reload(final HousekeepingFileService fileService) {
        final HousekeepingInformation loaded = fileService.getPersistedHousekeepingInformation(backupManagerId);
        this.autoDelete = loaded.autoDelete;
        this.maxNumberBackups = loaded.maxNumberBackups;
        persist();
    }

    /**
     * Persist Housekeeping
     */
    public void persist() {
        persistFunction.accept(this);
    }
}
