/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.action;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_DISABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.ACTION;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.ACTION_ID;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.ADDITIONAL_INFO;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.BACKUP_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.BACKUP_TYPE;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.STATUS;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_OPERATIONS_TOTAL;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_OPERATION_INFO;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_OPERATION_ENDTIME;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_SCHEDULED_OPERATION_ERROR_INFO;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;
import com.ericsson.adp.mgmt.backupandrestore.job.ResetConfigJob;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

/**
 * Represents an action with measurable progress.
 */
@SuppressWarnings({"PMD.CyclomaticComplexity"})
public class Action extends ActionInformation implements Versioned<PersistedAction> {
    private static final Logger log = LogManager.getLogger(Action.class);

    protected List<String> messages = new ArrayList<>();
    private double progressPercentage;
    private final OffsetDateTime startTime;
    private OffsetDateTime completionTime;
    private OffsetDateTime lastUpdateTime;
    private Version<PersistedAction> version;
    private final String backupManagerId;
    private final Consumer<Action> persistFunction;
    private final boolean executedAsTask;
    private final String autoDelete;
    private final int maximumManualBackupsStored;
    private final boolean isScheduledEvent;

    /**
     * Creates an action with an id, name and payload.
     * @param request information necessary to create an action.
     * @param persistFunction how to persist an action.
     */
    protected Action(final ActionRequest request, final Consumer<Action> persistFunction) {
        actionId = request.getActionId();
        name = request.getAction();
        payload = request.getPayload();
        startTime = OffsetDateTime.now(ZoneId.systemDefault());
        result = ResultType.NOT_AVAILABLE;
        state = ActionStateType.RUNNING;
        lastUpdateTime = startTime;
        backupManagerId = request.getBackupManagerId();
        this.persistFunction = persistFunction;
        executedAsTask = request.isExecutedAsTask();
        autoDelete = request.getAutoDelete();
        maximumManualBackupsStored = request.getMaximumManualBackupsStored();
        isScheduledEvent = request.isScheduledEvent();

        if (isScheduledEvent) {
            SpringContext.getBean(MeterRegistry.class).ifPresent(this::buildScheduledActionMetrics);
        }
    }

    /**
     * Creates action based on PersistedAction.
     * @param action that was persisted.
     * @param backupManagerId owner of action.
     * @param persistFunction how to persist an action.
     */
    protected Action(final PersistedAction action, final String backupManagerId, final Consumer<Action> persistFunction) {
        actionId = action.getActionId();
        name = action.getName();
        payload = action.getPayload();
        additionalInfo = action.getAdditionalInfo();
        startTime = DateTimeUtils.parseToOffsetDateTime(action.getStartTime());
        lastUpdateTime = DateTimeUtils.parseToOffsetDateTime(action.getLastUpdateTime());
        if (action.getCompletionTime() != null) {
            completionTime = DateTimeUtils.parseToOffsetDateTime(action.getCompletionTime());
        }
        progressInfo = action.getProgressInfo();
        if (action.getProgressPercentage() != null) {
            progressPercentage = action.getProgressPercentage();
        }
        result = action.getResult();
        resultInfo = action.getResultInfo();
        state = action.getState();
        this.backupManagerId = backupManagerId;
        this.persistFunction = persistFunction;
        executedAsTask = false;
        autoDelete = AUTO_DELETE_DISABLED;
        maximumManualBackupsStored = 1;
        isScheduledEvent = action.isScheduledEvent();
        version = action.getVersion();
    }

    /**
     * Creates a copy of action from another action
     * @param other the other action
     */
    private Action(final Action other) {
        actionId = other.actionId;
        name = other.name;
        payload = other.payload;
        additionalInfo = other.additionalInfo;
        startTime = other.startTime;
        lastUpdateTime = other.lastUpdateTime;
        if (other.completionTime != null) {
            completionTime = other.completionTime;
        }
        progressInfo = other.progressInfo;
        progressPercentage = other.progressPercentage;
        result = other.result;
        resultInfo = other.resultInfo;
        state = other.state;
        this.backupManagerId = other.backupManagerId;
        this.persistFunction = other.persistFunction;
        executedAsTask = other.executedAsTask;
        autoDelete = other.autoDelete;
        maximumManualBackupsStored = other.maximumManualBackupsStored;
        isScheduledEvent = other.isScheduledEvent;
        version = other.version;
        messages.addAll(other.messages);
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(final double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(final OffsetDateTime completionTime) {
        this.completionTime = completionTime;
    }

    public OffsetDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(final OffsetDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getBackupManagerId() {
        return backupManagerId;
    }

    /**
     * Get json representation of Action.
     *
     * @return json object.
     */
    public ActionResponse toJson() {
        final ActionResponse response = toSimplifiedJson();
        response.setPayload(getPayload());
        response.setAdditionalInfo(getAdditionalInfo());
        response.setProgressInfo(getProgressInfo());
        response.setProgressPercentage(getProgressPercentage());
        response.setResultInfo(getResultInfo());
        response.setState(getState());
        if (getCompletionTime() != null) {
            response.setCompletionTime(DateTimeUtils.convertToString(getCompletionTime()));
        }
        if (hasMessages()) {
            response.setResultInfo(getAllMessagesAsSingleString());
        }
        response.setLastUpdateTime(DateTimeUtils.convertToString(getLastUpdateTime()));
        return response;
    }

    /**
     * get all agent messages in the form
     * "{Agent: agent, Stage: stage, success: true, message: message }, {...}, ..., {...}"
     *
     * @return a single string of formatted messages
     */
    public String getAllMessagesAsSingleString() {
        final StringJoiner allAgentMessages = new StringJoiner(", ");
        for (final String message : getCopyOfMessages()) {
            allAgentMessages.add("{" + message.trim().replaceAll("\nm", ", m").replaceAll("\"", "").replaceAll("\n", "") + "}");
        }
        return allAgentMessages.toString();
    }

    /**
     * "Get json representation of minimal Action information.
     *
     * @return json object.
     */
    public ActionResponse toSimplifiedJson() {
        final ActionResponse response = new ActionResponse();
        response.setActionId(getActionId());
        response.setName(getName());
        response.setResult(getResult());
        response.setStartTime(DateTimeUtils.convertToString(getStartTime()));
        return response;
    }

    /**
     * Persists action.
     */
    public void persist() {
        persistFunction.accept(this);
    }

    /**
     * Gets backupName from the payload.
     * @return backupName
     */
    public String getBackupName() {
        if (getPayload() instanceof BackupNamePayload) {
            final BackupNamePayload payload = (BackupNamePayload) getPayload();
            return payload.getBackupName();
        }
        if (getPayload() instanceof ExportPayload) {
            final ExportPayload payload = (ExportPayload) getPayload();
            return payload.getBackupName();
        }
        if (getPayload() instanceof ImportPayload) {
            final ImportPayload payload = (ImportPayload) getPayload();
            return getImportBackupName(payload.hasSftpServerName() ? payload.getBackupPath() : payload.getUri().toString());
        }
        log.error("Action <" + actionId + "> doesn't have a backupName");
        throw new UnprocessableEntityException(
                "Action <" + actionId + "> doesn't have a backupName");
    }

    private String getImportBackupName (final String path) {
        final String backupName = Stream.of(path.split("/")).reduce((first, last)->last).orElse("");
        if (!backupName.endsWith(".tar.gz")) {
            return backupName;
        } else {
            return backupName.split("-", 4)[0];
        }
    }
    /**
     * Gets backup creationTime from the payload.
     *
     * @return backupName
     */
    public Optional<OffsetDateTime> getBackupCreationTime() {
        if (getPayload() instanceof BackupNamePayload) {
            final BackupNamePayload payload = (BackupNamePayload) getPayload();
            return payload.getCreationTime();
        }
        log.error("Action <" + actionId + "> doesn't have a creationTime");
        throw new UnprocessableEntityException("Action <" + actionId + "> doesn't have a creationTime");
    }

    /**
     * Indicates if action belongs to backup.
     * @return true if it does.
     */
    public boolean isRestoreOrExport() {
        return isRestore() || isExport();
    }

    /**
     * Indicates if the action is a RESTORE action
     * @return true if the action is a RESTORE, false otherwise.
     */
    public boolean isRestore() {
        return ActionType.RESTORE.equals(getName());
    }

    /**
     * Indicates if action is housekeeping or housekeeping delete backup.
     * @return true if it does.
     */
    public boolean isPartOfHousekeeping() {
        return ActionType.HOUSEKEEPING.equals(getName()) || isHousekeepingDelete();
    }

    /**
     * Indicates if the action is a HOUSEKEEPING_DELETE action
     * @return true if the action is a HOUSEKEEPING_DELETE, false otherwise.
     */
    public boolean isHousekeepingDelete() {
        return ActionType.HOUSEKEEPING_DELETE_BACKUP.equals(getName());
    }

    /**
     * Indicates if action belongs to a specific backup.
     * @param backupId to be checked
     * @return true if it does.
     */
    public boolean belongsToBackup(final String backupId) {
        return isRestoreOrExport() && getBackupName().equals(backupId);
    }

    /**
     * Gets a copy of the action's messages.
     *
     * @return copy of the messages
     */
    public synchronized List<String> getCopyOfMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * Adds a message to messages.
     * @param message - the message to add.
     */
    public synchronized void addMessage(final String message) {
        messages.add(message);
    }

    /**
     * Checks if the action has messages.
     *
     * @return Whether the action has messages or not
     */
    public synchronized boolean hasMessages() {
        return !messages.isEmpty();
    }

    public boolean isExecutedAsTask() {
        return executedAsTask;
    }

    public String getAutoDelete() {
        return autoDelete;
    }

    public int getMaximumManualBackupsStored() {
        return maximumManualBackupsStored;
    }

    public boolean isScheduledEvent() {
        return isScheduledEvent;
    }

    /**
     * Updates bro.operations.total metric for the action.
     */
    public void updateOperationsTotalMetric() {
        METRIC_BRO_OPERATIONS_TOTAL.unRegister();
        Metrics.counter(METRIC_BRO_OPERATIONS_TOTAL.identification(),
                ACTION.identification(), this.getName().toString(),
                STATUS.identification(), this.getResult().toString(),
                BACKUP_TYPE.identification(), this.getBackupManagerId()).increment();
    }

    /**
     * Updates bro.operation.info and bro.operation.end.time metrics for the action.
     */
    public void updateLastOperationInfoMetric() {
        final Optional<MeterRegistry> meterRegistry = SpringContext.getBean(MeterRegistry.class);
        MeterRegistry registry = null;
        if (meterRegistry.isPresent()) {
            registry = meterRegistry.get();
        } else {
            return;
        }
        switch (this.getName()) {
            case CREATE_BACKUP:
            case DELETE_BACKUP:
            case RESTORE:
            case IMPORT:
            case EXPORT:
                // Clear and repopulate metric with current operation
                METRIC_BRO_OPERATION_INFO.unRegister();
                registry.find(METRIC_BRO_OPERATION_INFO.identification())
                        .tag(BACKUP_TYPE.identification(), this.getBackupManagerId())
                        .tag(ACTION.identification(), this.getName().name())
                        .gauges()
                        .forEach(registry::remove);

                METRIC_OPERATION_ENDTIME.unRegister();
                registry.find(METRIC_OPERATION_ENDTIME.identification())
                        .tag(BACKUP_TYPE.identification(), this.getBackupManagerId())
                        .tag(ACTION.identification(), this.getName().name())
                        .gauges()
                        .forEach(registry::remove);

                rebuildGauges(registry);
                break;
            default:
                break;
        }
    }

    private void rebuildGauges(final MeterRegistry registry) {
        // For some Action types (e.g. IMPORT), getBackupName()
        // throws an UnprocessableEntityException exception
        String backupName;
        try {
            backupName = this.getBackupName();
        } catch (UnprocessableEntityException ex) {
            backupName = "None";
        }

        METRIC_BRO_OPERATION_INFO.unRegister();
        Gauge.builder(METRIC_BRO_OPERATION_INFO.identification(), () -> 1)
                .description(METRIC_BRO_OPERATION_INFO.description())
                .tag(ACTION.identification(), this.getName().name())
                .tag(BACKUP_TYPE.identification(), this.getBackupManagerId())
                .tag(BACKUP_NAME.identification(), backupName)
                .tag(ACTION_ID.identification(), this.getActionId())
                .tag(STATUS.identification(), this.getResult().name())
                .tag(ADDITIONAL_INFO.identification(), this.getAdditionalInfo() == null ?
                        "None" : this.getAdditionalInfo())
                .register(registry);

        // For some Actions (e.g. failed IMPORT), getCompletionTime()
        // can be null so return 0 for completion time
        METRIC_OPERATION_ENDTIME.unRegister();
        final long gaugeCompletionTime = this.getCompletionTime() == null ? 0 : this.getCompletionTime().toEpochSecond();
        Gauge.builder(METRIC_OPERATION_ENDTIME.identification(), () -> gaugeCompletionTime)
                .description(METRIC_OPERATION_ENDTIME.description())
                .tag(ACTION_ID.identification(), this.getActionId())
                .tag(ACTION.identification(), this.getName().name())
                .tag(BACKUP_TYPE.identification(), this.getBackupManagerId())
                .tag(STATUS.identification(), this.getResult().name())
                .register(registry);
    }

    private void buildScheduledActionMetrics(final MeterRegistry registry) {
        final Gauge existingGauge = registry.find(METRIC_SCHEDULED_OPERATION_ERROR_INFO.identification())
                .tag(BACKUP_TYPE.identification(), this.backupManagerId)
                .tag(ACTION.identification(), this.name.name())
                .gauge();

        if (existingGauge == null) {
            METRIC_SCHEDULED_OPERATION_ERROR_INFO.unRegister();
            Gauge.builder(METRIC_SCHEDULED_OPERATION_ERROR_INFO.identification(), () -> getLastScheduledActionResult(this.backupManagerId, this.name))
                .description(METRIC_SCHEDULED_OPERATION_ERROR_INFO.description())
                .tag(BACKUP_TYPE.identification(), this.backupManagerId)
                .tag(ACTION.identification(), this.name.name())
                .register(registry);
        }
    }

    /**
     * This method queries the status of the last scheduled and completed operation of a backupmanager with a given actionType.
     * @param backupManagerId
     * @param actionType
     * @return returns 1 if the action failed, 0 if the action is successful. The value of the previous scheduled
     * operation is returned if the current operation is still on-going.
     */
    private int getLastScheduledActionResult(final String backupManagerId, final ActionType actionType) {
        final Optional<BackupManagerRepository> backupManagerRepo = SpringContext.getBean(BackupManagerRepository.class);

        if (backupManagerRepo.isEmpty()) {
            return 0;
        }

        final BackupManager backupManager = backupManagerRepo.get().getBackupManager(backupManagerId);
        final List<Action> actions = backupManager.getActions();

        return actions.stream().sorted( Comparator.comparing(Action::getStartTime).reversed())
            .filter(action -> action.isScheduledEvent() && action.getName().equals(actionType))
            .filter(action -> !action.getResult().equals(ResultType.NOT_AVAILABLE))
            .findFirst()
            .map(action -> action.getResult().equals(ResultType.FAILURE) ? 1 : 0)
            .orElse(0);
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder("Action{");
        stringBuilder.append("actionId='").append('\'');
        stringBuilder.append(", name=").append(name).append('\'');
        stringBuilder.append(", result=").append(result).append('\'');
        stringBuilder.append(", state=").append(state).append('\'');
        stringBuilder.append(", progressPercentage=").append(progressPercentage).append('\'');

        if (getAdditionalInfo() != null) {
            stringBuilder.append(", additionalInfo='").append(getAdditionalInfo()).append('\'');
        }
        if (getProgressInfo() != null) {
            stringBuilder.append(", progressInfo='").append(progressInfo).append('\'');
        }
        if (getResultInfo() != null) {
            stringBuilder.append(", resultInfo='").append(resultInfo).append('\'');
        }

        if (getCompletionTime() != null) {
            stringBuilder.append(", completionTime='").append(completionTime).append('\'');
        }
        stringBuilder.append(", startTime='").append(startTime).append('\'');
        stringBuilder.append(", lastUpdateTime='").append(lastUpdateTime);
        stringBuilder.append("'}");
        return stringBuilder.toString();
    }

    @Override
    @JsonIgnore
    public Version<PersistedAction> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<PersistedAction> version) {
        this.version = version;
    }

    /**
     * Checks if an action is of a specified ActionType
     * @param type the action type
     * @return true if the action is a specified ActionType, false otherwise.
     */
    public boolean isActionType(final ActionType type) {
        return getName().equals(type);
    }

    public boolean isCreateBackup() {
        return isActionType(ActionType.CREATE_BACKUP);
    }

    public boolean isImport() {
        return isActionType(ActionType.IMPORT);
    }

    public boolean isExport() {
        return isActionType(ActionType.EXPORT);
    }

    /**
     * Checks if the action is an import or an export action.
     * @return true if the action is an import or an export action, false otherwise.
     */
    public boolean isImportOrExport() {
        return isExport() || isImport();
    }

    /**
     * Compares this action to another action and checks if the other action is targeted to the same BRM Id.
     * @param other the other action
     * @return true if the other action is targeted to the same BRM ID, false otherwise.
     */
    public boolean hasSameBRMId(final Action other) {
        return other.getBackupManagerId().equals(getBackupManagerId());
    }

    /**
     * Compares this action to another action and checks if the BRM held by one of the actions
     * is the parent of the other
     * @param other the other action
     * @return true If the BRM held by one of the actions is the parent of the other, false otherwise.
     */
    public boolean hasKinBRM(final Action other) {
        final Optional<BackupManagerRepository> backupManagerRepo = SpringContext.getBean(BackupManagerRepository.class);
        if (backupManagerRepo.isEmpty()) {
            return false;
        }
        final BackupManager otherManager = backupManagerRepo.get().getBackupManager(other.getBackupManagerId());
        final BackupManager thisManager = backupManagerRepo.get().getBackupManager(getBackupManagerId());
        return otherManager.getAgentVisibleBRMId().equals(thisManager.getAgentVisibleBRMId());
    }

    /**
     * Compares this action to another action and checks if the BRM held by one of the actions
     * is the configuration vBRM of the other
     * @param other the other action
     * @return true If the BRM held by one of the actions is the configuration vBRM of the other, false otherwise.
     */
    public boolean isConfigBRMOf(final Action other) {
        return getBackupManagerId().endsWith(ResetConfigJob.RESET_BRM_SUFFIX) &&
                getBackupManagerId()
                        .substring(0, getBackupManagerId().length() - ResetConfigJob.RESET_BRM_SUFFIX.length())
                        .equals(other.getBackupManagerId());
    }

    /**
     * Compares this action to another action and checks if the other action is for the same backup name
     * @param other the other action
     * @return true if the other action is for the same backup name, false otherwise.
     */
    public boolean hasSameBackupName(final Action other) {
        return other.getBackupName().equals(getBackupName());
    }

    @Override
    public int hashCode() {
        return backupManagerId.hashCode() + name.hashCode() + actionId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Action other = (Action) obj;
        return backupManagerId.equals(other.backupManagerId)
                && name.equals(other.name)
                && actionId.equals(other.actionId);
    }

    /**
     * Get a deep clone of the action
     * @return an Optional object wrapping the copy of the action
     */
    public Optional<Action> getCopy() {
        return Optional.of( new Action(this));
    }
}
