/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import static com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMClient.RETRY_INDEFINITELY;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.nacm.NACMRoleType.SYSTEM_ADMIN;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.nacm.NACMRoleType.SYSTEM_READ_ONLY;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.CONFIGURATION_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.DAY_IN_MILLISECONDS;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.DELAY_IN_SECONDS_BETWEEN_UPLOAD_ATTEMPTS;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.ERROR_CM_MAX_CONNECT_ATTEMPTS_REACHED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.NACM_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SCHEMA_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SCHEMA_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SCHEMA_VERSION;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SEARCH_STRING_SYSTEM_ADMIN;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SEARCH_STRING_SYSTEM_READ_ONLY;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.URL_SEP;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.getBrmConfigurationResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.HttpClientErrorException;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMEricssonbrmJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddProgressReportInitialPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ConfigurationPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ProgressReportPatch;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;

/**
 * Manages BRM Configuration in CM Mediator.
 */
@EnableScheduling
public class BRMConfigurationUtil {

    private static final Logger log = LogManager.getLogger(BRMConfigurationUtil.class);
    private static final String SUBSCRIPTION_RESOURCE = "subscriptions";
    private static final String JSON_NODE_PATH = "jsonSchema";

    private final JsonService jsonService;
    private final CMMClient cmmClient;
    private final CMMMessageFactory cmmMessageFactory;
    private final AtomicBoolean enableSubscriptionUpdate = new AtomicBoolean(true);
    private final EtagNotifIdBase etagBase;

    /**
     * Constructor receiving the service CMMClient and CMMMessageFactory
     * @param cmmClient Service CMM Client
     * @param cmmMessageFactory Service CMM Message
     * @param etagNotifIdBase Object used to keep the last baseEtag and notifId from Mediator notifications
     */
    public BRMConfigurationUtil(final CMMClient cmmClient,
            final CMMMessageFactory cmmMessageFactory,
            final EtagNotifIdBase etagNotifIdBase) {
        super();
        this.cmmClient = cmmClient;
        this.cmmMessageFactory = cmmMessageFactory;
        this.etagBase = etagNotifIdBase;
        jsonService = new JsonService();
    }

    /**
     * Push schema and configuration to CMM, creates a configuration with all the backupManagers from startup
     * Add NACM roles
     * Subscribe to CMM
     * @param retryIndefinitelyFlag defines no.of attempts to perform CMM operations
     * @param isStartup defines whether it is startup or not
     */
    protected void pushSchemaAndConfiguration(final boolean retryIndefinitelyFlag, final boolean isStartup) {
        // On array List empty, it read ll the BM from persistence filesystem.
        pushSchemaAndConfiguration (retryIndefinitelyFlag, isStartup, new ArrayList());
    }

    /**
     * Push schema and configuration to CMM.
     * Add NACM roles
     * Subscribe to CMM
     * @param retryIndefinitelyFlag defines no.of attempts to perform CMM operations
     * @param isStartup defines whether it is startup or not
     * @param backupManagers Array of Backup managers to be added
     */
    protected void pushSchemaAndConfiguration(final boolean retryIndefinitelyFlag,
                                              final boolean isStartup,
                                              final List<BackupManager> backupManagers) {
        handleSchemaAndConfiguration(backupManagers);
        handleNACMRoles(retryIndefinitelyFlag ? RETRY_INDEFINITELY : 1, isStartup);
        handleSubscribeToCMMediator(retryIndefinitelyFlag ? RETRY_INDEFINITELY : 1, isStartup);
    }

    /**
     * Push schema and configuration to CMM.
     * Add NACM roles
     * Subscribe to CMM
     * @param backupManagers Array of Backup managers to be added
     */
    protected void handleSchemaAndConfiguration(final List<BackupManager> backupManagers) {
        initializeCMMStatus(false);
        deleteConfiguration();
        createEmptyConfig();
        uploadSchema();
        if (backupManagers.isEmpty()) {
            // Creates configuration from persisted objects
            createConfiguration();
        } else {
            // Read the BM repository and add from there.
            addBackupManagersOneByOne(backupManagers);
        }
        initializeCMMStatus(true);
    }

    /**
     * Specifies whether the CMMService has been initialized.
     * The status is coordinated with BRMConfigurationService, since they share CMM access logic.
     * @param initialize true if it was initiated, false otherwise
     */
    protected void initializeCMMStatus(final boolean initialize) {
        getCMMClient().setInitialized(initialize);
        if (initialize) {
            // retrieve the last etag from Mediator and update the baseEtag for tracking
            final Optional<String> lastEtag = updateLastEtag();
            // If not able to retrieve the last etag, don't change it
            if (lastEtag.isPresent()) {
                etagBase.updateEtag(lastEtag.get());
            }
            log.debug("Initial baseEtag: {} - notifId: {} - configuration: {}", etagBase.getEtag(),
                    etagBase.getNotifId(), etagBase.getConfiguration());
        }
    }

    /**
     * Update to the last Etag from CMM
     * @return etag from CMM
     */
    public Optional<String> updateLastEtag() {
        return getCMMClient().getLastEtag(getBrmConfigurationResource());
    }

    /**
     * If the schema exists, it will be updated using the delete-create technique.
     */
    public void uploadSchema() {
        if (schemaExists()) {
            if (schemaVersionMismatch()) {
                log.info("schema version not match, reload the schema.");
                deleteSchema();
                getCMMClient().sleep(500, "Delete schema interrupted");
                createSchema();
            }
        } else {
            createSchema();
        }
    }

    /**
     * Creates configuration.
     */
    public void createConfiguration() {
        log.info("Attempting create configuration");
        final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy = (m, e) -> {
            deleteConfiguration();
            createEmptyConfig();
            uploadSchema();
            getCMMClient().processMessageAndWait(getCMMessageFactory().
                    getMessageToCreateConfiguration(RETRY_INDEFINITELY));
            return Optional.empty();
        };
        getCMMClient().processMessageAndWait(getCMMessageFactory().
                getMessageToCreateConfiguration(remedy, RETRY_INDEFINITELY));
    }

    /**
     * Read the current BM Container and add each one individually
     * @param backupManagers List of backup managers
     */
    public void addBackupManagersOneByOne(final List<BackupManager> backupManagers) {
        final AtomicBoolean isFirst = new AtomicBoolean(true); // First element
        log.info("Attempting create configuration by BM");
        backupManagers.stream().forEach(backupManager -> {
            if (isFirst.getAndSet(false)) {
                // add a new empty backup manager element
                getCMMClient().processMessageAndWait(getCMMessageFactory().
                        getMessageToCreateConfiguration(RETRY_INDEFINITELY,
                                Collections.singletonList(backupManager)));
            } else {
                cmmMessageFactory.getBackupManagerRepository().addBackupManager(backupManager);
            }
            sleep(100, "Interrupted adding " + backupManager.getBackupManagerId());
        });
    }

    /**
     * Creates an empty configuration.
     */
    public void createEmptyConfig() {
        final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy = (m, e) -> {
            deleteConfiguration();
            uploadSchema();
            getCMMClient().processMessageAndWait(getCMMessageFactory().
                    getMessageToCreateEmptyConfiguration(RETRY_INDEFINITELY));
            return Optional.empty();
        };
        getCMMClient().processMessageAndWait(getCMMessageFactory().
                getMessageToCreateEmptyConfiguration(remedy, RETRY_INDEFINITELY));
    }

    private void deleteSchema() {
        log.info("Attempting to delete schema");
        final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy = (m, e) -> {
            deleteConfiguration();
            getCMMClient().processMessageAndWait(getCMMessageFactory().getMessageToDeleteSchema(RETRY_INDEFINITELY));
            return Optional.empty();
        };
        if (schemaExists()) {
            getCMMClient().processMessageAndWait(getCMMessageFactory().getMessageToDeleteSchema(remedy, RETRY_INDEFINITELY));
        } else {
            log.info("Schema doesn't exist");
        }
    }

    private void createSchema() {
        log.info("Attempting to create schema");
        final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy = (m, e) -> {
            getCMMClient().processMessageAndWait(getCMMessageFactory().getMessageToUploadSchema(RETRY_INDEFINITELY));
            return Optional.empty();
        };
        getCMMClient().processMessageAndWait(getCMMessageFactory().getMessageToUploadSchema(remedy, RETRY_INDEFINITELY));
    }

    private boolean schemaVersionMismatch() {
        final Optional<String> currentVersion = getCurrentSchemaVersion();
        final Optional<String> newVersion = getSchemaVersion();
        if (currentVersion.isPresent() && newVersion.isPresent()) {
            log.info("current schema version: " + currentVersion.get());
            log.info("new schema version: " + newVersion.get());
            return !currentVersion.get().equals(newVersion.get());
        }
        log.info("no current or new schema version fetched, will re-create schema");
        return true;
    }

    private Optional<String> getSchemaVersion() {
        final InputStream  inputStream = getClass().getResourceAsStream("/ericsson-brm.json");
        if (inputStream != null) {
            try (InputStreamReader streamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(streamReader)) {
                final String schema = reader.lines()
                        .collect(Collectors.joining(System.lineSeparator()));
                return jsonService.parseJsonStringAndFetchValue(schema, null, SCHEMA_VERSION);
            } catch (final IOException e) {
                log.error("Error loading schema file {}", e.getMessage());
                return Optional.empty();
            }
        } else {
            log.debug("Could not load schema file");
            return Optional.empty();
        }
    }

    private Optional<String> getCurrentSchemaVersion() {
        final String schema = getCMMClient().get(SCHEMA_RESOURCE + URL_SEP + SCHEMA_NAME);
        return jsonService.parseJsonStringAndFetchValue(schema, JSON_NODE_PATH, SCHEMA_VERSION);
    }

    /**
     * Deletes configuration if it exists in CM.
     */
    public void deleteConfiguration() {
        if (getCMMClient().cmmExists(getBrmConfigurationResource(), 1, false)) {
            log.info("Attempting delete BRO configuration in Mediator");
            getCMMClient().processMessageAndWait(getCMMessageFactory().getMessageToDeleteConfiguration(RETRY_INDEFINITELY));
        }
    }

    private void handleNACMRoles(final int retry, final boolean isStartup) {
        if (getCMMClient().cmmExists(NACM_RESOURCE, retry, isStartup)) {
            log.info("ietf-netconf-acm configuration available in CM");
            addNACMRoles();
        }
    }

    private void addNACMRoles() {
        //Removing all whitespace from the returned String just in case.
        final String result = getCMMClient().get(NACM_RESOURCE).replaceAll("\\s+", "");
        if (!result.contains(SEARCH_STRING_SYSTEM_ADMIN)) {
            final CMMMessage message = getCMMessageFactory().getMessageToAddNACMRoles(SYSTEM_ADMIN, 1, RETRY_INDEFINITELY);
            getCMMClient().processMessage(message);
            log.info("{} role added successfully", SYSTEM_ADMIN);
        } else {
            log.info("{} role not added because it already exists.", SYSTEM_ADMIN);
        }
        if (!result.contains(SEARCH_STRING_SYSTEM_READ_ONLY)) {
            final CMMMessage message = getCMMessageFactory().getMessageToAddNACMRoles(SYSTEM_READ_ONLY, 2, RETRY_INDEFINITELY);
            getCMMClient().processMessage(message);
            log.info("{} role added successfully", SYSTEM_READ_ONLY);
        } else {
            log.info("{} role not added because it already exists.", SYSTEM_READ_ONLY);
        }
    }

    private void handleSubscribeToCMMediator(final int retry, final boolean isStartup) {
        deleteSubscription(retry, isStartup);
        createSubscription(retry, isStartup);
    }

    /**
     * Attempt to create a subscription
     */
    private void attemptCreateSubscription() {
        final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy = (m, e) -> {
            log.error(
                    "Error attempting to subscribe to CM from file. Error received {}.",
                    e.getMessage());
            return Optional.empty();
        };
        getCMMClient().processMessage(getCMMessageFactory().
                getMessageToSubscribeToCMM(remedy, RETRY_INDEFINITELY));
    }

    /**
     * Create subscription and POST to Mediator
     * @param retry Defines no.of attempts to perform CMM operations
     * @param isStartup Defines whether it is startup or not
     */
    protected void createSubscription(final int retry, final boolean isStartup) {
        log.info("Attempting to subscribe to Mediator:");
        getCMMClient().processMessage(getCMMessageFactory().
                getMessageToSubscribeToCMM(gethandleSubscribeToCMMediatorRemedy(retry, isStartup),
                        RETRY_INDEFINITELY));
    }

    private void deleteSubscription(final int retry, final boolean isStartup) {
        if (isSubscriptionExist()) {
            log.info("Attempting to delete the subscription in Mediator");
            getCMMClient().processMessage(getCMMessageFactory().
                    getMessageToDeleteSubscription(gethandleSubscribeToCMMediatorRemedy(retry, isStartup),
                            RETRY_INDEFINITELY));
        }
    }

    /**
     * On error it retries the handleSubscribeToCMMediator execution
     * @return
     */
    private BiFunction<CMMMessage, Exception, Optional<CMMMessage>> gethandleSubscribeToCMMediatorRemedy(final int retry, final boolean isStartup) {
        return (m, e) -> {
            log.error("Exception received while Subscribe to CMMediator with  message {}", e.getMessage());
            if (!isStartup && retry == 0) {
                log.error("Subscription to CMM Mediator failed: max attempts reached with  message {}", e.getMessage());
                return Optional.empty();
            }
            if ((retry > 0 || retry == RETRY_INDEFINITELY) && getCMMClient().cmmExists(CONFIGURATION_RESOURCE, retry, isStartup)) {
                handleSubscribeToCMMediator(retry > 0 ? retry - 1 : RETRY_INDEFINITELY, isStartup);
                sleep();
            } else {
                log.error(ERROR_CM_MAX_CONNECT_ATTEMPTS_REACHED, e.getMessage());
                return Optional.empty();
            }
            return Optional.empty();
        };
    }

    /**
     * Try to put a subscription request into Mediator
     * used the scheduler validating leased Time
     */
    protected void updateSubscriptionToMediator() {
        if (isConfigurationExist()) {
            updateSubscription();
        }
    }

    /**
     * Try to put a request into Mediator
     */
    public void updateSubscription() {
        log.info("Attempting to update subscription");
        final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy = (m, e) -> Optional.empty();
        getCMMClient().processMessage(getCMMessageFactory().getMessageToUpdateToSubscription(remedy, RETRY_INDEFINITELY));
    }

    private boolean schemaExists() {
        final boolean schemaAlreadyExists = getCMMClient().checkExistingSchemaIndefinitely();
        if (schemaAlreadyExists) {
            log.info("Schema {} exists.", SCHEMA_NAME);
        } else {
            log.info("Schema {} does not exist.", SCHEMA_NAME);
        }
        return schemaAlreadyExists;
    }

    /**
     * Performs a thread sleep
     */
    protected void sleep() {
        sleep(DELAY_IN_SECONDS_BETWEEN_UPLOAD_ATTEMPTS * 1_000L, "Upload interrupted");
    }

    /**
     * Performs a thread sleep
     * @param delayMilliseconds delay in milliseconds
     */
    private void sleep(final long delayMilliseconds, final String messageInterruption) {
        getCMMClient().sleep(delayMilliseconds, messageInterruption);
    }
    /**
     * Updates configuration.
     * @param patch to apply.
     */
    public void patch(final ConfigurationPatch patch) {
        log.debug("Updating configuration in CM with <{}>", patch);
        getCMMClient().processMessage(getMessageBrmConfiguration (patch,
                HttpMethod.PATCH,
                createRemedyPatch(patch)));
    }

    private BiFunction<CMMMessage, Exception, Optional<CMMMessage>> createRemedyPatch(final ConfigurationPatch patch) {
        final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy = (m, e) -> {
            if (((HttpClientErrorException) e).getStatusCode() == HttpStatus.CONFLICT) {
                // On conflict it retries with a REST switch operation instead to retry with
                // same patch operation
                switchOperation (patch);
                return Optional.of(getMessageBrmConfiguration (patch, HttpMethod.PATCH,
                    (messa, excp) -> Optional.empty()));
            }
            return Optional.empty();
        };
        return remedy;
    }

    private void switchOperation(final ConfigurationPatch patch) {
        if (patch.getOperation() == PatchOperation.ADD) {
            patch.setOperation(PatchOperation.REPLACE);
        } else if (patch.getOperation() == PatchOperation.REPLACE) {
            patch.setOperation(PatchOperation.ADD);
        }
    }

    private CMMMessage getMessageBrmConfiguration (final ConfigurationPatch patch,
            final HttpMethod method,
            final BiFunction remedy) {
        if (patch instanceof ProgressReportPatch) {
            final Action action = ((ProgressReportPatch) patch).getAction();
            final int retry = action.getProgressPercentage() == 1.0 ? RETRY_INDEFINITELY : getCMMClient().getMaxAttempts();
            final CMMMessage cmmMessage = getCMMessageFactory().getMessage(getBrmConfigurationResource(), null, patch, method, remedy, retry);
            if (patch instanceof AddProgressReportInitialPatch) {
                final HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json-patch+json");
                cmmMessage.setHttpEntity(new HttpEntity<>(headers));
            }
            return cmmMessage;
        } else {
            return getCMMessageFactory().getMessage(getBrmConfigurationResource(), null, patch, method);
        }
    }

    /**
     * Try to subscribe to the configuration updates on Mediator
     */
    protected void subscribeToMediator() {
        if (isConfigurationExist()) {
            attemptCreateSubscription();
        }
    }

    /**
     * Every 4 minutes, the system validates the Leased time and renew the subscription if it's required
     * If the leased time remaining in the subscription is lower than 2 days it renew the subscription.
     */
    @Scheduled(fixedRate = 4 * 60 * 1000)
    public void validateLeasedTime() {
        if (!getCMMClient().isReady() || ! enableSubscriptionUpdate.get()) {
            return;
        }
        if (isConfigurationExist()) {
            if (!isSubscriptionExist()) {
                log.warn("Subscription {} does not exist", getSubscriptionResource());
                subscribeToMediator();
            } else {
                final Optional<ConfigurationRequest> configurationRequest = getConfigurationRequest(getSubscriptionResource());
                if (configurationRequest.isPresent()) {
                    updateSubscriptionMediator (configurationRequest.get());
                } else {
                    log.error("Failed to parse configurationRequest, isEmpty is true");
                }
            }
        } else {
            log.warn("Configuration {} does not exist", CONFIGURATION_RESOURCE);
        }
    }

    private void updateSubscriptionMediator(final ConfigurationRequest configurationRequest) {
        log.info("CM Subscription {} leased time remaining {}", getSubscriptionResource(), configurationRequest.getLeaseSeconds());
        if (configurationRequest.getLeaseSeconds() < (DAY_IN_MILLISECONDS / 1000) * 2 ) {
            log.debug("Updating Subscription {} ", getSubscriptionResource());
            updateSubscriptionToMediator();
        } else {
            log.debug("Subscription {} update not required", getSubscriptionResource());
        }
    }

    protected Optional<BRMEricssonbrmJson> getEricssonBRMConfiguration () {
        return getBRMConfiguration(getCMMClient().getResourceEtag(getBrmConfigurationResource()));
    }

    /**
     * Creates an object BRMEricssonbrmJson using a configuration string from CMM
     * @param jsonString configuration string
     * @return object representing BRMEricssonbrmJson
     */
    protected Optional<BRMEricssonbrmJson> getBRMConfiguration (final String jsonString) {
        return cmmMessageFactory.getCmmSubscriptionRequestFactory()
                .parseJsonStringToBRMConfiguration(jsonString);
    }

    /**
     * Validates if the Configuration exists on Mediator
     * @return true if the Configuration is loaded in Mediator, otherwise false.
     */
    protected boolean isConfigurationExist() {
        return getCMMClient().exists(getBrmConfigurationResource());
    }

    private boolean isSubscriptionExist() {
        return getCMMClient().exists(getSubscriptionResource());
    }

    private String getSubscriptionResource() {
        return SUBSCRIPTION_RESOURCE + URL_SEP + SCHEMA_NAME;
    }

    private Optional<ConfigurationRequest> getConfigurationRequest (final String resource) {
        return cmmMessageFactory.getCmmSubscriptionRequestFactory()
                .parseJsonStringToSubscriptionRequest(getCMMClient().get(resource));
    }

    public CMMClient getCMMClient() {
        return cmmClient;
    }

    public CMMMessageFactory getCMMessageFactory() {
        return this.cmmMessageFactory;
    }

    /**
     * Used in Junit to disable the scheduled update subscriptor
     * @param enabled true if requires to updateSubscriptor by scheduler
     */
    public void enableSubscriptionUpdate(final boolean enabled) {
        enableSubscriptionUpdate.set(enabled);
    }

    /**
     * update the last baseEtag and lastNotifId
     * @param eTag last eTag to be tracked
     * @param notifId last notifId to be tracked
     */
    public void updateEtagNotifId(final String eTag, final Integer notifId) {
        updateEtag (eTag);
        etagBase.setNotifId(notifId);
    }

    /**
     * update the last baseEtag
     * @param eTag last eTag to be tracked
     */
    public void updateEtag(final String eTag) {
        etagBase.updateEtag (eTag);
    }

    /**
     * Validates the etag and notifId provided
     * @param etag to compare vs the base values
     * @param notifId to compare vs the base values
     * @return true if valid
     */
    public boolean isValidETag(final String etag, final Integer notifId) {
        return etagBase.isValidMediatorRequest(etag, notifId);
    }
}
