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
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.util.function.Predicate;

import org.springframework.http.HttpMethod;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMMessage;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation;

/**
 * Global Constants
 */
public final class ApplicationConstantsUtils {
    /**
     * Name of the configuration handler for Mediator
     */
    public static final String URLMAPPING = "confighandler";
    public static final String SCHEMA_NAME = "ericsson-brm";
    public static final String CONFIGURATION_RESOURCE = "configurations";
    public static final String SUBSCRIPTION_RESOURCE = "subscriptions";
    public static final String SCHEMA_RESOURCE = "schemas";
    public static final String SCHEMA_VERSION = "eric-adp-version";
    public static final String BACKUP_RESOURCE = "backup";
    public static final String NACM_CONFIGURATION_NAME = "ietf-netconf-acm";
    public static final String NACM_RESOURCE = CONFIGURATION_RESOURCE + "/" + NACM_CONFIGURATION_NAME;
    public static final String CONFIGURATION_BRO_RESOURCE = CONFIGURATION_RESOURCE + "/" + SCHEMA_NAME;
    public static final String SEARCH_STRING_SYSTEM_ADMIN = "\"name\":\"ericsson-brm-1-system-admin\"";
    public static final String SEARCH_STRING_SYSTEM_READ_ONLY = "\"name\":\"ericsson-brm-2-system-read-only\"";
    public static final long DAY_IN_MILLISECONDS = 86400000;
    public static final String ADD_PARAMETER = "-";


    /**
     * Accepted values for housekeeping autoDelete
     */
    public static final String AUTO_DELETE_ENABLED = "enabled";
    public static final String AUTO_DELETE_DISABLED = "disabled";
    public static final int DEFAULT_MAX_BACKUP = 1;

    /**
     * ALIASES used in keystores for cert authorities
     */
    public static final String BRO_CA_ALIAS = "broCa";
    public static final String SIP_TLS_ROOT_CERT_ALIAS = "siptlsRootCert";
    public static final String PM_CLIENT_CA_ALIAS = "pmClientCa";
    public static final String CMYP_CLIENT_CA_ALIAS = "cmypClientCa";
    public static final String CMM_CLIENT_CA_ALIAS = "cmmClientCa";
    public static final String CMM_CLIENT_ACTION_CA_ALIAS = "cmmClientActionCa";
    public static final String CMM_CLIENT_STATE_CA_ALIAS = "cmmClientStateCa";
    public static final String CMM_CLIENT_VALIDATOR_CA_ALIAS = "cmmClientValidatorCa";

    public static final String BRO_HTTP_PORT = "7001";
    public static final String BRO_TLS_PORT = "7002";
    public static final String BRO_TLS_PM_PORT = "7003";
    public static final String BRO_TLS_CMM_NOTIF_PORT = "7004";

    /**
     * Settings used in resttemplate
     */
    public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 50;
    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 10;
    public static final int DEFAULT_KEEPALIVE_SECONDS = 20;

    /**
     * Settings used in import/export
     */
    public static final String BACKUP_DATA_FOLDER_NAME = "backupdata";
    public static final String BACKUP_FILE_FOLDER_NAME = "backupfile";
    public static final String BACKUP_MONITOR_TIMEOUT_NAME = "isTimeout";
    public static final String PROGRESS_MONITOR_CURRENT_PERCENTAGE = "pmCurrentPercentage";

    public static final String BACKUP_MANAGER_CONFIG_BACKUP_FOLDER = "backupManagers";

    /**
     * Settings used as Spring cache variables
     */
    public static final String BACKUP_FOLDER_SIZE = "backupFolderSize";
    public static final String BACKUP_USED_SIZE = "backupUsedSize";

    /**
     * Settinsg used in S3
     */
    public static final String S3_CREATION_TIME = "creation-time";
    public static final String S3_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    /**
     * Settings used in CMM Client
     */
    public static final String ERROR_CM_MAX_CONNECT_ATTEMPTS_REACHED = "Cannot establish contact with CM - max attempts reached.";
    public static final int DELAY_IN_SECONDS_BETWEEN_UPLOAD_ATTEMPTS = 6;
    public static final String URL_SEP = "/";

    /**
     * Settings used in EtagNotifId to identify the BM/backup position from CMM
     */
    public static final int BACKUP_MANAGER_POSITION_IN_CONTEXT = 3;
    public static final int BACKUP_POSITION_IN_CONTEXT = 5;

    public static final Predicate<CMMMessage> TO_TOP_POST_PUT_DELETE_PATCH = message -> (
            HttpMethod.POST.equals(((CMMMessage) message).getHttpMethod()) ||
            HttpMethod.PUT.equals(message.getHttpMethod()) ||
            HttpMethod.DELETE.equals(message.getHttpMethod()) ||
            ( message.getConfigurationPatch().getOperation().equals(PatchOperation.ADD) &&
            message.getConfigurationPatch().getPath().endsWith(ADD_PARAMETER)));

    private ApplicationConstantsUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String getBrmConfigurationResource() {
        return CONFIGURATION_RESOURCE + URL_SEP + SCHEMA_NAME;
    }
}
