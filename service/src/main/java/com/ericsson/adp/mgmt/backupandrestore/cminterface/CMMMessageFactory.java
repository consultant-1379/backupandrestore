/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2022
 * <p>
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.CONFIGURATION_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.NACM_CONFIGURATION_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SCHEMA_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SCHEMA_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SUBSCRIPTION_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.CONFIGURATION_BRO_RESOURCE;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.NewConfigurationRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.SchemaRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.nacm.NACMRoleType;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.BackupManagerPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.BackupPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ConfigurationPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.HousekeepingPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.NACMRolePatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PeriodicEventPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.SchedulerPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;

/**
 * The factory class is responsible to create CMM message which will be processed by CMM Client.
 */
@Service
public class CMMMessageFactory {
    private static final String DELIMITER = "/";

    private BackupPatchFactory backupPatchFactory;
    private BackupManagerPatchFactory backupManagerPatchFactory;
    private HousekeepingPatchFactory housekeepingPatchFactory;
    private SchedulerPatchFactory schedulerPatchFactory;
    private PeriodicEventPatchFactory periodicEventPatchFactory;

    private SchemaRequestFactory schemaRequestFactory;
    private JsonService jsonService;
    private CMMSubscriptionRequestFactory cmmSubscriptionRequestFactory;
    private BackupManagerRepository backupManagerRepository;

    /**
     * Create the general CMM message
     * @param resource the resource to be manipulated
     * @param httpEntity the input as httpEntity
     * @param patch the input as configuration patch
     * @param method the http method
     * @return the CMM message
     */
    public CMMMessage getMessage(final String resource,
                                 final HttpEntity httpEntity,
                                 final ConfigurationPatch patch,
                                 final HttpMethod method) {
        return new CMMMessage(resource, httpEntity, patch, method,
            (m, e) -> Optional.empty(), 0);
    }

    /**
     * Create the general CMM message with retry
     * @param resource the resource to be manipulated
     * @param httpEntity the input as httpEntity
     * @param patch the input as configuration patch
     * @param method the http method
     * @param retry the number of retries to be performed
     * @return the CMM message
     */
    public CMMMessage getMessage(final String resource,
                                 final HttpEntity httpEntity,
                                 final ConfigurationPatch patch,
                                 final HttpMethod method,
                                 final int retry) {
        return new CMMMessage(resource, httpEntity, patch, method, (m, e) -> Optional.empty(), retry);
    }

    /**
     * Create the general CMM message with retry
     * @param resource the resource to be manipulated
     * @param httpEntity the input as httpEntity
     * @param patch the input as configuration patch
     * @param method the http method
     * @param fallback fallback method when message fails
     * @param retry the number of retries to be performed
     * @return the CMM message
     */
    public CMMMessage getMessage(final String resource,
                                 final HttpEntity httpEntity,
                                 final ConfigurationPatch patch,
                                 final HttpMethod method,
                                 final BiFunction fallback,
                                 final int retry) {
        return new CMMMessage(resource, httpEntity, patch, method, fallback, retry);
    }

    /**
     * Creates a CMMMessage to delete configuration
     * @param retry no of times to retry
     * @return CMMMessage to delete configuration
     */
    public CMMMessage getMessageToDeleteConfiguration(final int retry) {
        return new CMMMessage(getBrmConfigurationResource(), null, null, DELETE,
            (m, e) -> Optional.empty(), retry);
    }

    /**
     * Creates a CMMMessage to delete schema
     * @param retry number of times to retry
     * @return CMMMessage to delete schema
     */
    public CMMMessage getMessageToDeleteSchema(final int retry) {
        final SchemaRequest request = schemaRequestFactory.getRequestToCreateSchema();
        return new CMMMessage(SCHEMA_RESOURCE + DELIMITER + request.getName(), null, null, DELETE,
            (m, e) -> Optional.empty(), retry);
    }

    /**
     * Creates a CMMMessage to delete schema with remedy included
     * @param remedy to execute
     * @param retry no of times to retry
     * @return CMMMessage to delete schema
     */
    public CMMMessage getMessageToDeleteSchema(final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy, final int retry) {
        final SchemaRequest request = schemaRequestFactory.getRequestToCreateSchema();
        return new CMMMessage(SCHEMA_RESOURCE + DELIMITER + request.getName(), null, null, DELETE,
            remedy, retry);
    }

    /**
     * Creates a CMMMessage to upload schema
     * @param retry no of times to retry
     * @return CMMMessage to upload schema
     */
    public CMMMessage getMessageToUploadSchema(final int retry) {
        final SchemaRequest request = schemaRequestFactory.getRequestToCreateSchema();
        return new CMMMessage(SCHEMA_RESOURCE, request.getHttpEntity(), null, POST,
            (m, e) -> Optional.empty(), retry);
    }

    /**
     * Creates a CMMMessage to upload schema
     * @param remedy to execute
     * @param retry no of times to retry
     * @return CMMMessage to upload schema
     */
    public CMMMessage getMessageToUploadSchema(final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy, final int retry) {
        final SchemaRequest request = schemaRequestFactory.getRequestToCreateSchema();
        return new CMMMessage(SCHEMA_RESOURCE, request.getHttpEntity(), null, POST,
            remedy, retry);
    }

    /**
     * Creates a CMMMessage to create configuration
     * @param retry no of times to retry
     * @param backupManagers List of backupManagers to add
     * @return CMMMessage to create configuration
     */
    protected CMMMessage getMessageToCreateConfiguration(final int retry, final List<BackupManager> backupManagers) {
        final NewConfigurationRequest newConfigurationRequest =
                new NewConfigurationRequest(SCHEMA_NAME,
                        new BRMConfiguration(backupManagers));
        newConfigurationRequest.setName(null);
        return new CMMMessage(CONFIGURATION_BRO_RESOURCE,
                new HttpEntity<>(newConfigurationRequest), null, PUT,
            (m, e) -> Optional.empty(), retry);
    }

    /**
     * Creates a CMMMessage to create configuration
     * @param retry no of times to retry
     * @return CMMMessage to create configuration
     */
    public CMMMessage getMessageToCreateConfiguration(final int retry) {
        final NewConfigurationRequest newConfigurationRequest =
                new NewConfigurationRequest(SCHEMA_NAME,
                        new BRMConfiguration(backupManagerRepository.getBackupManagers()));
        newConfigurationRequest.setName(null);
        return new CMMMessage(CONFIGURATION_BRO_RESOURCE,
                new HttpEntity<>(newConfigurationRequest), null, PUT,
            (m, e) -> Optional.empty(), retry);
    }

    /**
     * Creates a CMMMessage to create configuration
     * @param remedy to execute
     * @param retry no of times to retry
     * @return CMMMessage to create configuration
     */
    public CMMMessage getMessageToCreateConfiguration(final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy, final int retry) {
        final NewConfigurationRequest newConfigurationRequest =
                new NewConfigurationRequest(SCHEMA_NAME,
                        new BRMConfiguration(backupManagerRepository.getBackupManagers()));
        newConfigurationRequest.setName(null);
        return new CMMMessage(CONFIGURATION_BRO_RESOURCE,
                new HttpEntity<>(newConfigurationRequest), null, PUT,
            remedy, retry);
    }

    /**
     * Creates a CMMMessage to create an empty configuration
     * @param retry no of times to retry
     * @return CMMMessage to create empty configuration
     */
    public CMMMessage getMessageToCreateEmptyConfiguration(final int retry) {
        final BRMConfiguration emptyConfiguration = new BRMConfiguration();
        emptyConfiguration.setBrm(null);
        final NewConfigurationRequest newConfigurationRequest =
                new NewConfigurationRequest(SCHEMA_NAME, emptyConfiguration);
        return new CMMMessage(CONFIGURATION_RESOURCE,
                new HttpEntity<>(newConfigurationRequest), null, POST,
            (m, e) -> Optional.empty(), retry);
    }

    /**
     * Creates a CMMMessage to create an empty configuration
     * @param remedy to execute
     * @param retry no of times to retry
     * @return CMMMessage to create empty configuration
     */
    public CMMMessage getMessageToCreateEmptyConfiguration(final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy, final int retry) {
        final BRMConfiguration emptyConfiguration = new BRMConfiguration();
        emptyConfiguration.setBrm(null);
        final NewConfigurationRequest newConfigurationRequest =
                new NewConfigurationRequest(SCHEMA_NAME, emptyConfiguration);
        return new CMMMessage(CONFIGURATION_RESOURCE,
                new HttpEntity<>(newConfigurationRequest), null, POST,
            remedy, retry);
    }

    /**
     * Creates a CMMMessage to add NACM roles
     * @param roleType to add
     * @param position to add the role to
     * @param retry no of times to retry
     * @return CMMMessage to add NACM roles
     */
    public CMMMessage getMessageToAddNACMRoles(final NACMRoleType roleType, final int position, final int retry) {
        return new CMMMessage(getNACMResource(), null, getPatch(roleType, position), PATCH,
            (m, e) -> Optional.empty(), retry);
    }

    /**
     * Creates a CMMMessage to delete subscription
     * @param retry no of times to retry
     * @return CMMMessage to delete subscription
     */
    public CMMMessage getMessageToDeleteSubscription(final int retry) {
        return new CMMMessage(getBrmSubscriptionResource(), null, null, DELETE,
            (m, e) -> Optional.empty(), retry);
    }

    /**
     * Creates a CMMMessage to delete subscription
     * @param remedy to execute
     * @param retry no of times to retry
     * @return CMMMessage to delete subscription
     */
    public CMMMessage getMessageToDeleteSubscription(final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy, final int retry) {
        return new CMMMessage(getBrmSubscriptionResource(), null, null, DELETE,
            remedy, retry);
    }

    /**
     * Creates a CMMMessage to create subscription
     * @param retry no of times to retry
     * @return CMMMessage to create subscription
     */
    public CMMMessage getMessageToSubscribeToCMM(final int retry) {
        return getMessageToSubscribeToCMM ((m, e) -> Optional.empty(), retry);
    }

    /**
     * Creates a CMMMessage to create subscription
     * @param remedy to execute
     * @param retry no of times to retry
     * @return CMMMessage to create subscription
     */
    public CMMMessage getMessageToSubscribeToCMM(final BiFunction remedy, final int retry) {
        final SchemaRequest subscriptionRequest = cmmSubscriptionRequestFactory.getRequestToCreateSchema();
        return new CMMMessage(SUBSCRIPTION_RESOURCE, subscriptionRequest.getHttpEntity(), null, POST,
            remedy, retry);
    }

    /**
     * Creates a CMMMessage to update the subscription
     * @param retry no of times to retry
     * @return CMMMessage to update the subscription
     */
    public CMMMessage getMessageToUpdateToSubscription(final int retry) {
        return getMessageToUpdateToSubscription ((m, e) -> Optional.empty(), retry);
    }

    /**
     * Creates a CMMMessage to update the subscription
     * @param remedy to execute
     * @param retry no of times to retry
     * @return CMMMessage to update the subscription
     */
    public CMMMessage getMessageToUpdateToSubscription(final BiFunction remedy, final int retry) {
        final SchemaRequest subscriptionRequest = cmmSubscriptionRequestFactory.getRequestToUpdateSchema();
        return new CMMMessage(SUBSCRIPTION_RESOURCE, subscriptionRequest.getHttpEntity(), null, PUT,
            remedy, retry);
    }

    /**
     * Create a CMM message to add the backup in CM
     * @param manager backup manager the backup belongs to
     * @param backup the backup to be manipulated
     * @param retry number of times to retry
     * @return the CMM message
     */
    public CMMMessage getMessageToAddBackup(final BackupManager manager, final Backup backup, final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                backupPatchFactory.getPatchToAddBackup(manager, backup), PATCH, retry);
    }

    /**
     * Create a CMM message to add the backup in CM
     * @param manager backup manager the backup belongs to
     * @param backup the backup to be manipulated
     * @param retry number of times to retry
     * @return the CMM message
     */
    public CMMMessage getMessageToAddInitialBackup(final BackupManager manager, final Backup backup, final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                backupPatchFactory.getPatchToAddInitialBackup(manager, backup), PATCH, retry);
    }

    /**
     * Create a CMM message to add the backup base in CM
     * @param retry number of times to retry
     * @return the CMM message
     */
    public CMMMessage getMessageToUploadBackupManager(final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                backupPatchFactory.getPathToAddBackupManager(), PATCH, retry);
    }

    /**
     * Create a CMM message to update the backup in CM
     * @param manager backup manager the backup belongs to
     * @param backup the backup to be manipulated
     * @param remedy to execute on failure
     * @param retry no of times to retry
     * @return the CMM message to update.
     */
    public CMMMessage getMessageToUpdateBackup(final BackupManager manager, final Backup backup,
                                               final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy, final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                backupPatchFactory.getPatchToUpdateBackup(manager , backup), PATCH, remedy, retry);
    }


    /**
     * This method is used to delete backup.
     * Because the backup index needs to be calculated in BackupRepository.java
     *
     * @param backupManagerId the backup manager
     * @param backupIndex the index of backup
     * @param retry number of times to retry
     * @return the CMM message
     */
    public CMMMessage getMessageToDeleteBackup(final String backupManagerId, final int backupIndex, final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                backupPatchFactory.getPatchToDeleteBackup(backupManagerId, backupIndex), PATCH, retry);
    }

    /**
     * Create a CMM message to add the scheduler in CM
     * @param scheduler the scheduler to be manipulated
     * @param retry number of times to retry
     * @return the CMM message
     */
    public CMMMessage getMessageToAddScheduler(final Scheduler scheduler, final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                schedulerPatchFactory.getPatchToAddScheduler(scheduler), PATCH, retry);
    }

    /**
     * Create a CMM message to update the scheduler in CM
     * @param scheduler the scheduler to be manipulated
     * @param remedy to execute on failure
     * @param retry no of times to retry
     * @return the CMM message to update.
     */
    public CMMMessage getMessageToUpdateScheduler(final Scheduler scheduler,
                                                  final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy,
                                                  final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                schedulerPatchFactory.getPatchToUpdateScheduler(scheduler), PATCH, remedy, retry);
    }

    /**
     * Create a CMM message to add the housekeeping in CM
     * @param housekeeping the housekeeping to be manipulated
     * @param retry number of times to retry
     * @return the CMM message
     */
    public CMMMessage getMessageToAddHousekeeping(final Housekeeping housekeeping, final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                housekeepingPatchFactory.getPatchToAddHousekeeping(housekeeping), PATCH, retry);
    }

    /**
     * Create a CMM message to update the housekeeping in CM
     * @param housekeeping the housekeeping to be manipulated
     * @param remedy to execute on failure
     * @param retry no of times to retry on failure
     * @return the CMM message to update
     */
    public CMMMessage getMessageToUpdateHousekeeping(final Housekeeping housekeeping,
                                                     final BiFunction remedy,
                                                     final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                housekeepingPatchFactory.getPatchToUpdateHousekeeping(housekeeping), PATCH, remedy, retry);
    }

    /**
     * Create a CMM message to add the periodic event in CM
     * @param periodicEvent the periodic event to be manipulated
     * @param retry number of times to retry
     * @return the CMM message
     */
    public CMMMessage getMessageToAddPeriodicEvent(final PeriodicEvent periodicEvent, final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                periodicEventPatchFactory.getPatchToAddPeriodicEvent(periodicEvent), PATCH, retry);
    }

    /**
     * Create a CMM message to add the periodic event in CM
     * @param periodicEvent the periodic event to be manipulated
     * @param remedy to execute on failure
     * @param retry retries on failure
     * @return CMMMesage to be updated
     */
    public CMMMessage getMessageToUpdatePeriodicEvent(final PeriodicEvent periodicEvent,
                                                      final BiFunction remedy,
                                                      final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                periodicEventPatchFactory.getPatchToUpdatePeriodicEvent(periodicEvent), PATCH,
                remedy, retry);
    }

    /**
     * Create a CMM message to delete the periodic event in CM
     * @param periodicEvent the periodic event to be manipulated
     * @param retry number of times to retry
     * @return the CMM message
     */
    public CMMMessage getMessageToDeletePeriodicEvent(final PeriodicEvent periodicEvent, final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                periodicEventPatchFactory.getPatchToDeletePeriodicEvent(periodicEvent), PATCH, retry);
    }

    /**
     * Create a cmm Message to add the backup manager in CM
     *
     * @param backupManager the backup manager to be manipulated.
     * @param retry number of times to retry
     * @return the CMM message
     */
    public CMMMessage getPatchToAddBackupManager(final BackupManager backupManager, final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME,
                null, backupManagerPatchFactory.getPatchToAddBackupManager(backupManager), PATCH, retry);
    }

    /**
     * Create a cmmMessage update the backup manager in CM
     *
     * @param backupManager the backup manager to be manipulated.
     * @param retry number of times to retry
     * @return the CMM message
     */
    public CMMMessage getMessageToUpdateBackupManager(final BackupManager backupManager, final int retry) {
        return getMessageToUpdateBackupManager(backupManager, (m, e) -> Optional.empty(), retry);
    }

    /**
     * Create a cmmMessage update the backup manager in CM
     *
     * @param backupManager the backup manager to be manipulated.
     * @param remedy to be executed on rest fail
     * @param retry no of times to retry
     * @return the CMM message
     */
    public CMMMessage getMessageToUpdateBackupManager(final BackupManager backupManager,
                                                      final BiFunction remedy,
                                                      final int retry) {
        return new CMMMessage(CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME, null,
                backupManagerPatchFactory.getPatchToUpdateBackupManager(backupManager), PATCH, remedy, retry);
    }

    private String getBrmConfigurationResource() {
        return CONFIGURATION_RESOURCE + DELIMITER + SCHEMA_NAME;
    }

    private String getNACMResource() {
        return CONFIGURATION_RESOURCE + DELIMITER + NACM_CONFIGURATION_NAME;
    }

    private String getBrmSubscriptionResource() {
        return SUBSCRIPTION_RESOURCE + DELIMITER + SCHEMA_NAME;
    }

    private NACMRolePatch getPatch(final NACMRoleType roleType, final int position) {
        return new NACMRolePatch(PatchOperation.ADD, roleType, position, jsonService);
    }

    /**
     * Get the service Cmm Subscription Request Factory
     * @return CmmSubscriptionRequestFactory
     */
    public CMMSubscriptionRequestFactory getCmmSubscriptionRequestFactory() {
        return cmmSubscriptionRequestFactory;
    }

    public BackupManagerRepository getBackupManagerRepository() {
        return backupManagerRepository;
    }

    @Autowired
    public void setBackupPatchFactory(final BackupPatchFactory backupPatchFactory) {
        this.backupPatchFactory = backupPatchFactory;
    }

    @Autowired
    public void setBackupManagerPatchFactory(final BackupManagerPatchFactory backupManagerPatchFactory) {
        this.backupManagerPatchFactory = backupManagerPatchFactory;
    }

    @Autowired
    public void setHousekeepingPatchFactory(final HousekeepingPatchFactory housekeepingPatchFactory) {
        this.housekeepingPatchFactory = housekeepingPatchFactory;
    }

    @Autowired
    public void setSchedulerPatchFactory(final SchedulerPatchFactory schedulerPatchFactory) {
        this.schedulerPatchFactory = schedulerPatchFactory;
    }

    @Autowired
    public void setPeriodicEventPatchFactory(final PeriodicEventPatchFactory periodicEventPatchFactory) {
        this.periodicEventPatchFactory = periodicEventPatchFactory;
    }

    @Autowired
    public void setSchemaRequestFactory(final SchemaRequestFactory schemaRequestFactory) {
        this.schemaRequestFactory = schemaRequestFactory;
    }

    @Autowired
    public void setJsonService(final JsonService jsonService) {
        this.jsonService = jsonService;
    }

    @Autowired
    protected void setCMMSubscriptionRequestFactory(final CMMSubscriptionRequestFactory cmmSubscriptionRequestFactory) {
        this.cmmSubscriptionRequestFactory = cmmSubscriptionRequestFactory;
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

}

