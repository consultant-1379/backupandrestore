/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.test;

public enum ApiUrl {
    V1_BASE_URL("http://localhost:7001/v1/"),
    V2_BASE_URL("http://localhost:7001/v2/"),
    V3_BASE_URL("http://localhost:7001/v3/"),
    V4_BASE_URL("http://localhost:7001/backup-restore/v4/"),
    V1_DEFAULT_BACKUP_MANAGER(V1_BASE_URL + "backup-manager/DEFAULT/"),
    V3_DEFAULT_BACKUP_MANAGER(V3_BASE_URL + "backup-managers/DEFAULT/"),
    V4_DEFAULT_BACKUP_MANAGER(V4_BASE_URL + "backup-managers/DEFAULT/"),
    V1_ACTION(V1_DEFAULT_BACKUP_MANAGER + "action"),
    V3_ACTION(V3_DEFAULT_BACKUP_MANAGER + "actions"),
    V4_ACTION(V4_DEFAULT_BACKUP_MANAGER + "actions"),
    V1_DEFAULT_BACKUP_MANAGER_BACKUP(V1_DEFAULT_BACKUP_MANAGER + "backup/"),
    V3_DEFAULT_BACKUP_MANAGER_BACKUP(V3_DEFAULT_BACKUP_MANAGER + "backups/"),
    V4_DEFAULT_BACKUP_MANAGER_BACKUP(V4_DEFAULT_BACKUP_MANAGER + "backups/"),
    V4_DEFAULT_BACKUP_MANAGER_AGENTS(V4_DEFAULT_BACKUP_MANAGER + "agents/"),
    V4_IMPORTS(V4_DEFAULT_BACKUP_MANAGER + "imports"),
    V4_EXPORTS(V4_DEFAULT_BACKUP_MANAGER + "exports/"),
    V4_SCHEDULER_CALENDAR(V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules/"),
    HOUSEKEEPING_BACKUP_MANAGER("HOUSEKEEPING_TEST"),
    HOUSEKEEPING_BACKUP_MANAGER_URL(V1_BASE_URL + "backup-manager/" + HOUSEKEEPING_BACKUP_MANAGER),
    HOUSEKEEPING_BACKUP_MANAGER_URL_V3(V3_BASE_URL + "backup-managers/" + HOUSEKEEPING_BACKUP_MANAGER),
    HOUSEKEEPING_BACKUP_MANAGER_URL_V4(V4_BASE_URL + "backup-managers/" + HOUSEKEEPING_BACKUP_MANAGER),
    HOUSEKEEPING_BACKUP_MANAGER_ACTION(HOUSEKEEPING_BACKUP_MANAGER_URL + "/action"),
    SFTP_SERVER_BACKUP_MANAGER("SFTP_SERVER_TEST"),
    CONFIGURATION_RESOURCE ("configurations"),
    CONFIGURATION_NAME ("ericsson-brm"),
    CONFIGURATION_URL (CONFIGURATION_RESOURCE + "/" + CONFIGURATION_NAME),
    METRICS_URL("http://localhost:7001/actuator/metrics/");

    private final String url;

    ApiUrl(final String url) {
        this.url = url;
    }

    public String toString() {
        return url;
    }
}
