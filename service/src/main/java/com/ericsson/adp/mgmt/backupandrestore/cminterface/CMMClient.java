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
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.CONFIGURATION_BRO_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.NACM_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SCHEMA_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SCHEMA_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.TO_TOP_POST_PUT_DELETE_PATCH;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.getBrmConfigurationResource;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_MANAGER_POSITION_IN_CONTEXT;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_POSITION_IN_CONTEXT;
import static org.springframework.http.HttpMethod.PATCH;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ConfigurationPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation;
import com.ericsson.adp.mgmt.backupandrestore.exception.CMMediatorException;
import com.ericsson.adp.mgmt.backupandrestore.util.ManagedQueueingWorker;
import com.ericsson.adp.mgmt.backupandrestore.util.ProcessorEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * A service for interacting with the CMM REST API.
 */
@Service
public class CMMClient {
    protected static final int RETRY_INDEFINITELY = -1;
    private static final long INITIAL_DELAY_MS = 100;
    private static final String ETAG_VALUE_NOT_CURRENT = "ETag value not current";
    private static final Logger log = LogManager.getLogger(CMMClient.class);
    private boolean flagEnabled;
    // Is initialized or not
    private final AtomicBoolean initialized = new AtomicBoolean();
    private ManagedQueueingWorker<CMMMessage> blockingQueueService;
    private CMMRestClient cmmRestClient;
    private EtagNotifIdBase etagNotifidBase;
    private int maxDelay;
    private int maxAttempts;

    /**
     * Push the request to CMM server
     *
     * @param cmmMessage the message to be processed
     */
    public void processMessage(final CMMMessage cmmMessage) {
        blockingQueueService.add(cmmMessage);
    }

    /**
     * Stop processing messages
     */
    public void stopProcessing() {
        if (blockingQueueService != null) {
            blockingQueueService.stopProcessing();
        }
    }

    /**
     * Process a CMM Message and wait for the result
     * @param cmmMessage the message to be processed
     */
    public void processMessageAndWait(final CMMMessage cmmMessage) {
        blockingQueueService.addAndWait(cmmMessage);
    }
    /**
     * This is used with the Configuration Resource to determine if the CMM service is available or not.
     *
     * @param resource  to be verified.
     * @param maxRetries     Number of times to be retried.
     * @param isStartup defines whether it is startup or not.
     * @return true if it exists.
     */
    public boolean cmmExists(final String resource, final int maxRetries, final boolean isStartup) {
        if (isStartup) {
            return checkResourceUntilItExists(resource);
        } else {
            return checkResourceWithRetry(resource, maxRetries);
        }
    }

    private boolean checkResourceWithRetry(final String resource, final int maxRetries) {
        int retryCount = 0;
        boolean found = false;
        long delay = INITIAL_DELAY_MS;
        while (shouldRetry(maxRetries, retryCount) && !found) {
            try {
                found = exists(resource);
            } catch (final Exception e) {
                log.info("Exception while checking for resource {} in CMM", resource);
            }
            if (!found) {
                retryCount++;
                if (shouldRetry(maxRetries, retryCount)) {
                    final String loggedMaxRetries = maxRetries == RETRY_INDEFINITELY ? "infinity" : maxRetries + "";
                    log.warn("Resource {} not found. Retrying in {} ms (retry {} of {})", resource, delay, retryCount, loggedMaxRetries);
                    sleep(delay, MessageFormat.format("Validation for resource {0} is interrupted", resource));
                    delay = Math.min(delay * 2, maxDelay);
                } else {
                    log.warn("Failed to validate resource {} availability after {} retries", resource, retryCount);
                }
            }
        }
        return found;
    }

    private boolean shouldRetry(final int maxRetries, final int retryCount) {
        return maxRetries == RETRY_INDEFINITELY || retryCount <= maxRetries;
    }

    private boolean checkResourceUntilItExists(final String resource) {
        boolean found = false;
        long delay = INITIAL_DELAY_MS;
        while (!found) {
            try {
                found = exists(resource);
            } catch (final Exception e) {
                log.info("Exception while checking for resource {} in CMM", resource);
            }
            if (!found) {
                sleep(delay, MessageFormat.format("Validation for resource {0} is interrupted", resource));
                delay = Math.min(delay * 2, maxDelay);
            }
        }
        return true;
    }

    /**
     * Check if the schema exists indefinitely
     * @return if the schema exists
     */
    protected boolean checkExistingSchemaIndefinitely() {
        long delay = INITIAL_DELAY_MS;
        final String resource = SCHEMA_RESOURCE + "/" + SCHEMA_NAME;
        while (true) {
            try {
                return exists(resource);
            } catch (final Exception e) {
                log.info("Exception while checking for resource {} in CMM", resource);
                sleep(delay, MessageFormat.format("Validation for resource {0} is interrupted", resource));
                delay = Math.min(delay * 2, maxDelay);
            }
        }
    }

    /**
     * Validates if a resource exists in CM.
     *
     * @param resource to be verified.
     * @return true if it exists.
     */
    protected boolean exists(final String resource) {
        final String logString = resource.replaceFirst("/", " ");
        log.debug("Attempting to validate resource {}", logString);
        try {
            log.debug("Checking for resource {} in CM", logString);
            cmmRestClient.get(resource);
            log.debug("Resource {} exists in CM", logString);
            return true;
        } catch (final HttpClientErrorException e) {
            // Validates exclusively if the 404 is received
            // Configurations not found indicates CMM not ready - retry
            if (e.getStatusCode() == HttpStatus.NOT_FOUND && !resource.equals(CONFIGURATION_RESOURCE)) {
                log.info("Resource {} not found - {}", logString, e.getMessage());
                return false;
            } else {
                log.warn("HttpClientErrorException calling {} - {} with HttpStatus Code {}",
                        logString, e.getResponseBodyAsString(), e.getStatusCode());
                throw e;
            }
        } catch (final ResourceAccessException e) {
            log.warn("Resource {} access exception - {}", logString, e.getMessage());
            throw e;
        } catch (final Exception e) {
            log.warn("Exception calling {} - {}", logString, e.getMessage());
            throw e;
        }
    }

    /**
     * Performs a thread sleep in a specified time delay
     * @param delay the sleep duration
     * @param threadInterruptErrorMessage the error message when the thread is interrupted while sleeping
     */
    public void sleep(final long delay, final String threadInterruptErrorMessage) {
        try {
            log.info("Waiting {} milli seconds.", delay);
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CMMediatorException(MessageFormat.format("{0}: {1}", threadInterruptErrorMessage, e.getMessage()));
        }
    }

    public boolean isFlagEnabled() {
        return flagEnabled;
    }

    /**
     * Specify if it was initialized, used mainly in test
     * @param initialize true it was initialized
     */
    public void setInitialized(final boolean initialize) {
        this.initialized.set(initialize);
    }

    protected boolean isInitialized() {
        return this.initialized.get();
    }

    /**
     * Flag to identify if the CMM Client is enabled or not.
     *
     * @param flagEnabled true if CMM Client will be used
     */
    @Value("${flag.enable.cm:false}")
    public void setFlagEnabled(final boolean flagEnabled) {
        this.flagEnabled = flagEnabled;
        if (this.flagEnabled) {
            blockingQueueService = new ManagedQueueingWorker<>(new CMMClientEngine());
        }
    }

    /**
     * Validates if the CMM is enabled and it was initialized
     * @return true if it was enabled and initialized false otherwise
     */
    public boolean isReady() {
        return isFlagEnabled() && isInitialized();
    }

    @Value("${cmm.retry.maxDelay:3000}")
    public void setMaxDelay(final String maxDelay) {
        this.maxDelay = Integer.parseInt(maxDelay);
    }

    @Value("${cmm.retry.maxAttempts:10}")
    public void setMaxAttempts(final String maxAttempts) {
        this.maxAttempts = Integer.parseInt(maxAttempts);
    }

    @Autowired
    public void setCmmRestClient(final CMMRestClient cmmRestClient) {
        this.cmmRestClient = cmmRestClient;
    }

    @Autowired
    public void setEtagNotifidBase(final EtagNotifIdBase etagNotifIdBase) {
        this.etagNotifidBase = etagNotifIdBase;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    /**
     * This get method need to return a String result immediately which can't be done in our blocking queue.
     * to put the method into our queue we may need to implement an Async Future mechanism.
     * so it's a separate method.
     * @param resource the resource to be check.
     * @return the json result.
     */
    public String get(final String resource) {
        return cmmRestClient.get(resource);
    }

    /**
     * This get method need to return a String result immediately which can't be done in our blocking queue.
     * to put the method into our queue we may need to implement an Async Future mechanism.
     * so it's a separate method.
     * @param resource the resource to be checked.
     * @return the json result including the etag header.
     */
    public String getResourceEtag(final String resource) {
        return cmmRestClient.exchange(resource);
    }
    /**
     * Retrieve the eTag from URL Mediator resource
     * @param configResource URL Mediator resource
     * @return last eTag registered in configuration
     */
    protected Optional<String> getLastEtag(final String configResource) {
        try {
            return cmmRestClient.retrieveConfigETag(configResource);
        } catch (final Exception e) {
            log.warn("Exception while retrieving CMM etag resource from {} : {}", configResource, e);
        }
        return Optional.empty();
    }

    private class CMMClientEngine implements ProcessorEngine<CMMMessage> {
        /**
         * Push the request to a element processor
         *
         * @param cmmMessage the message to be processed
         * @return ServiceState service state after the processed message
         */
        @Override
        public CMMMessage transferMessage(final CMMMessage cmmMessage) { // NOPMD CyclomaticComplexity
            int retryCount = 0;
            Optional<String> cfgPath_original = Optional.empty();
            final Optional<ConfigurationPatch> configurationPath = Optional.ofNullable(cmmMessage.getConfigurationPatch());
            if (configurationPath.isPresent() &&
                    configurationPath.get().getPath() != null &&
                    ! configurationPath.get().getPath().isEmpty()) {
                cfgPath_original = Optional.of(configurationPath.get().getPath());
            }
            while (true) {
                try {
                    if (!isFlagEnabled()) {
                        return null;
                    }
                    if (requireEtag(cmmMessage)) {
                        return executeRestActionEtag(cmmMessage);
                    }
                    executeRestActions(cmmMessage);
                    return null;
                } catch (final ResourceAccessException exception) {
                    if (retryAfterprocessRAE(exception, cmmMessage)) {
                        log.info("Retrying to send the message <{}> - attempt {}", cmmMessage, retryCount + 1);
                        retryCount++;
                    } else {
                        log.info("Executing the remedy for <{}>", cmmMessage);
                        return remedy(cmmMessage, exception);
                    }
                } catch (final HttpClientErrorException exception) {
                    sleepAfterException(exception, cmmMessage, INITIAL_DELAY_MS);
                    if (isMessageMovedToTop(exception, cmmMessage)) {
                        return null;
                    }
                    if (requireEtag(cmmMessage)) {
                        if (isMessageContainsEtag (exception, cmmMessage)) {
                            log.info("Retry to send the message <{}> - attempt {}", cmmMessage, retryCount + 1);
                            retryCount++;
                            continue;
                        } else {
                            // give preference to PATCH commands used to create elements in the configuration.
                            moveCreateMessageToTopInQueue(cmmMessage);
                            return null;
                        }
                    }
                    // on a different error, it will run the remedy instead
                    log.info("Executing the remedy for <{}>", cmmMessage);
                    return remedy(cmmMessage, exception);
                } catch (final Exception exception) {
                    sleepAfterException(exception, cmmMessage, INITIAL_DELAY_MS);
                    log.info("Executing the remedy for <{}>", cmmMessage);
                    return remedy(cmmMessage, exception);
                } finally {
                    // On any call, the cmmMessage reset to their original path
                    processFinally (cfgPath_original, cmmMessage);
                }
            }
        }

        private boolean retryAfterprocessRAE(final ResourceAccessException exception, final CMMMessage cmmMessage) {
            sleepAfterException(exception, cmmMessage, INITIAL_DELAY_MS);
            return (cmmMessage.getRetry() == RETRY_INDEFINITELY || cmmMessage.getRetryAndDecrement() > 0);
        }

        private void processFinally(final Optional<String> cfgPath_original, final CMMMessage cmmMessage) {
            if (cfgPath_original.isPresent()) {
                cmmMessage.getConfigurationPatch().setPath(cfgPath_original.get());
            }
        }

        private boolean isMessageMovedToTop(final HttpClientErrorException exception, final CMMMessage cmmMessage) {
            if (exception.getStatusCode() == HttpStatus.BAD_REQUEST &&
                    cmmMessage.getConfigurationPatch() != null &&
                    !isMissingNodeInCMM(cmmMessage)) {
                // If the path is not missing, means is an invalid request
                log.warn("{} - Ignoring the "
                        + "execution of message <{}>", exception.getMessage(), cmmMessage);
                moveCreateMessageToTopInQueue(cmmMessage);
                return true;
            }
            return false;
        }

        private boolean isMessageContainsEtag(final HttpClientErrorException exception, final CMMMessage cmmMessage) {
            final Optional<String> messageOpt = Optional.ofNullable(exception.getMessage());
            // on 409 Error and if it's a patch command or put command it requests an ETag.
            // If the path does not longer exists anymore it will jump the message to avoid
            // retrying the execution forever.
            // This happens mainly, after the restore, when the elements are added to the configuration
            // when it is regenerated, so the patches will no longer be needed.
            return ((messageOpt.isPresent() &&
                    messageOpt.get().contains(ETAG_VALUE_NOT_CURRENT)) &&
                    (cmmMessage.getRetry() == RETRY_INDEFINITELY || cmmMessage.getRetryAndDecrement() > 0));
        }

        private boolean isMissingNodeInCMM(final CMMMessage cmmMessage) {
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            try {
                final JsonNode rootNode = objectMapper.readTree(get(cmmMessage.getResource()));
                final JsonNode nameNode = rootNode.at(cmmMessage.getConfigurationPatch().getPath());
                return nameNode.isMissingNode();
            } catch (Exception e) {
                return false;
            }
        }

        private long sleepAfterException(final Exception exception, final CMMMessage cmmMessage, final long delay) {
            final String threadInterruptionErrorMessage = "The CMM REST Request execution is interrupted";
            log.warn("Failed to transfer the message <{}> due to {}", cmmMessage, exception.getMessage());
            sleep(delay, threadInterruptionErrorMessage);
            return Math.min(delay * 2, maxDelay);
        }

        /**
         * In REST exceptions 503 or 409, it is moved to the top of the processing queue
         * those messages used to create elements such as POST/PUT and PATCH with ADD operation.
         * @param ioException exception to validate
         * @param CMMMessage initial cmmMessage is set back to the queue
         * @return true if queue adjusted, false otherwise
         */
        private boolean moveCreateMessageToTopInQueue (final CMMMessage cmmMessage) {
            blockingQueueService.offerFirst(cmmMessage); // return the current message to top queue
            // uplift any POST or PATCH message used to create an element to the TOP of the queue
            return blockingQueueService.ToTop(TO_TOP_POST_PUT_DELETE_PATCH);
        }

        private boolean requireEtag(final CMMMessage cmmMessage) {
            log.debug("eTag cmmMessage resource:{} Method:{} configurationPath:{}",
                    cmmMessage.getResource(), cmmMessage.getHttpMethod(), cmmMessage.getConfigurationPatch());
            return (cmmMessage.getHttpMethod() == HttpMethod.PUT || cmmMessage.getHttpMethod() == HttpMethod.PATCH) &&
                   (cmmMessage.getResource().equalsIgnoreCase(getBrmConfigurationResource()) ||
                           cmmMessage.getResource().equalsIgnoreCase(NACM_RESOURCE)) &&
                    cmmMessage.getConfigurationPatch() != null;
        }

        private CMMMessage executeRestActionEtag(final CMMMessage cmmMessage) {
            // if it fails to get the etag it will be retrying returning the message to the dequeue
            // after that, it execute the message updated
            if (isReady()) {
                log.info("Retrieving etag for <{}>", cmmMessage);
                sleep(500, "Interrupting delay to get the last ETag");
                final Optional<String> lastEtag = getLastEtag(cmmMessage.getResource());
                if (lastEtag.isPresent()) {
                    cmmMessage.getConfigurationPatch().setEtag(lastEtag.get());
                    executeRestActions(cmmMessage);
                    return null;
                } else { // If can't get the last Etag
                    // return the current message to the queue
                    sleep(1000, "Validation for etag is interrupted");
                    // If reorder to top and the message in top is not the same
                    if (moveCreateMessageToTopInQueue(cmmMessage)
                            && blockingQueueService.getTop() != cmmMessage) {
                        log.warn("Switch a POST request to top and then execute <{}>", blockingQueueService.getTop());
                        return null;
                    }
                    return cmmMessage;
                }
            } else {
                sleep(500, "Waiting for CMMMClient is ready is interrupted");
                log.info("CMMClient is not ready");
            }
            return cmmMessage;
        }

        private String updatePath(final String path, final int bmIndex, final int backupIndex) {
            if (bmIndex == -1 && backupIndex == -1) {
                return path;
            }

            final String[] parts = path.split("/");
            if (bmIndex >= 0) {
                parts[BACKUP_MANAGER_POSITION_IN_CONTEXT] = String.valueOf(bmIndex);
            }
            if (backupIndex >= 0) {
                parts[BACKUP_POSITION_IN_CONTEXT] = String.valueOf(backupIndex);
            }
            final String result = Arrays.stream(parts)
                    .filter(part -> !part.isEmpty())
                    .collect(Collectors.joining("/"));
            return "/" + result + (path.endsWith("/") ? "/" : "");
        }

        /**
         * execute the action.
         *
         * @param cmmMessage the cmmMessage to be executed.
         */
        private void executeRestActions(final CMMMessage cmmMessage) {
            final HttpMethod httpMethod = cmmMessage.getHttpMethod();
            final Optional<ConfigurationPatch> configurationPath = Optional.ofNullable(cmmMessage.getConfigurationPatch());
            final Optional<String> pathUpdated = getPathUpdated(configurationPath, cmmMessage);
            if (pathUpdated.isPresent()) {
                configurationPath.get().setPath(pathUpdated.get());
            }
            if (HttpMethod.PATCH.equals(httpMethod)) {
                if (cmmMessage.getHttpEntity() != null ) {
                    cmmRestClient.exchange(cmmMessage.getResource(),
                            cmmMessage.getConfigurationPatch(),
                            cmmMessage.getHttpEntity());
                } else {
                    cmmRestClient.patch(cmmMessage.getResource(), cmmMessage.getConfigurationPatch());
                }
            } else if (HttpMethod.POST.equals(httpMethod)) {
                cmmRestClient.post(cmmMessage.getResource(), cmmMessage.getHttpEntity());
            } else if (HttpMethod.PUT.equals(httpMethod)) {
                cmmRestClient.put(cmmMessage.getResource(), cmmMessage.getHttpEntity());
            } else if (HttpMethod.DELETE.equals(httpMethod)) {
                cmmRestClient.delete(cmmMessage.getResource());
            } else {
                log.warn("HTTP method <{}> not supported", httpMethod);
            }
        }

        private Optional<String> getPathUpdated(final Optional<ConfigurationPatch> configurationPath, final CMMMessage cmmMessage) {
            // filtered to be used only in configuration/BRO
            Optional<String> pathUpdated = Optional.empty();

            if (configurationPath.isPresent() &&
                    ! configurationPath.get().getPath().isEmpty() &&
                    cmmMessage.getResource().equalsIgnoreCase(CONFIGURATION_BRO_RESOURCE) ) {
                final String path = configurationPath.get().getPath();
                final String newPath;
                final int bmIndex = etagNotifidBase.
                        getCMMIndexBackupManager(path);
                if (bmIndex == -1 && cmmMessage.getHttpMethod() == PATCH
                        && cmmMessage.getConfigurationPatch().getOperation( ) != PatchOperation.ADD) {
                    throw new HttpClientErrorException(HttpStatus.CONFLICT, "Backup Manager is missing in CMM:" + cmmMessage.getConfigurationPatch());
                }
                final int backupIndex = etagNotifidBase.
                        getCMMIndexBackupManagerBackup(path, bmIndex);
                newPath = updatePath(path, bmIndex, backupIndex);
                if (newPath.equals(path)) {
                    // if not change required, returned an empty value
                    return Optional.empty();
                }
                pathUpdated = Optional.of(newPath);
                log.debug("Updating Path in <{}> - from <{} to {}>",
                        cmmMessage.getResource(),
                        path,
                        pathUpdated);
            }
            return pathUpdated;
        }

        private CMMMessage remedy(final CMMMessage cmmMessage, final Exception exception) {
            return cmmMessage.getFallback().apply(cmmMessage, exception).orElse(null);
        }
    }
}
