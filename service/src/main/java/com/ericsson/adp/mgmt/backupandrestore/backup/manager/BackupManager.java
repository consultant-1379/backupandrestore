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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager;

import static com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils.TIMESTAMP_PATTERN;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.ACTION;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.ACTION_ID;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.ADDITIONAL_INFO;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.BACKUP_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.BACKUP_TYPE;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.STATUS;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_DISK_USAGE_BYTES;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_OPERATION_INFO;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_STORED_BACKUPS;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_OPERATION_ENDTIME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.PersistedBackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.exception.ActionNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupIdAlreadyExistsException;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidActionException;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Represents the BackupManager entity.
 */
@SuppressWarnings({"PMD.CyclomaticComplexity"})
public class BackupManager extends FullBackupManagerInformation implements Versioned<PersistedBackupManager> {

    public static final String DEFAULT_BACKUP_MANAGER_ID = "DEFAULT";
    private static final String SCOPE_SEPARATOR = ";";
    private static final Pattern PATTERN = Pattern.compile("(.*)(?=" + TIMESTAMP_PATTERN + ")");
    private static final Logger log = LogManager.getLogger(BackupManager.class);

    private final List<Action> actions = new ArrayList<>();
    private final List<Backup> owned = new ArrayList<>();
    private final List<Backup> readable = new ArrayList<>();
    private final List<SftpServer> sftpServers = new ArrayList<>();
    private final Consumer<BackupManager> persistFunction;
    private final Housekeeping housekeeping;
    private final Scheduler scheduler;
    private final AtomicBoolean backupManagerLevelProgressReportCreated = new AtomicBoolean();
    private final BackupManagerFileService backupManagerfileService;

    private Version<PersistedBackupManager> version;
    private final BackupManagerRepository backupManagerRepository;
    private final VirtualInformation virtualInformation;

    /**
     * Creates a new backupManager with an id.
     *
     * @param backupManagerId
     *            backupManager's id.
     * @param housekeeping
     *            instance with default information
     * @param scheduler
     *            instance with default information
     * @param persistFunction
     *            to persist backup manager
     * @param backupManagerfileService
     *            instance of backup manager file service
     * @param backupManagerRepository
     *            ref to the backup manager repo, necessary as vBRMs use it to get their parents backup list
     * @param virtualInformation
     *            the virtual information of this BRM
     */
    protected BackupManager(final String backupManagerId, final Housekeeping housekeeping,
                            final Scheduler scheduler, final Consumer<BackupManager> persistFunction,
                            final BackupManagerFileService backupManagerfileService,
                            final BackupManagerRepository backupManagerRepository,
                            final VirtualInformation virtualInformation) {
        this.backupManagerId = backupManagerId;
        this.housekeeping = housekeeping;
        this.persistFunction = persistFunction;
        this.scheduler = scheduler;
        this.backupManagerfileService = backupManagerfileService;
        this.backupManagerRepository = backupManagerRepository;
        this.virtualInformation = virtualInformation;
        SpringContext.getBean(MeterRegistry.class).ifPresent(this::buildMetrics);
    }

    /**
     * Creates a backupManager that was previously persisted.
     *
     * @param persistedBackupManager
     *            used to create backupManager.
     * @param actions
     *            persisted actions.
     * @param sftpServers
     *            persisted SFTP servers
     * @param owned
     *            persisted backups.
     * @param housekeeping
     *            with persisted values
     * @param scheduler
     *            with persisted values
     * @param persistFunction
     *            to persist a backupManager.
     * @param backupManagerfileService
     *            instance of backup manager file service
     * @param backupManagerRepository
     *            ref to the backup manager repo, necessary as vBRMs use it to get their parents backup list
     * @param virtualInformation
     *            the virtual information of this BRM
     */
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public BackupManager(final PersistedBackupManager persistedBackupManager, final List<Action> actions, final List<Backup> owned,
                         final List<SftpServer> sftpServers,
                         final Housekeeping housekeeping, final Scheduler scheduler, final Consumer<BackupManager> persistFunction,
                         final BackupManagerFileService backupManagerfileService,
                         final BackupManagerRepository backupManagerRepository,
                         final VirtualInformation virtualInformation) {
        backupManagerId = persistedBackupManager.getBackupManagerId();
        backupDomain = persistedBackupManager.getBackupDomain();
        backupType = persistedBackupManager.getBackupType();
        this.actions.addAll(actions);
        this.owned.addAll(owned);
        this.readable.addAll(owned);
        this.sftpServers.addAll(new ArrayList<>(sftpServers));
        this.housekeeping = housekeeping;
        this.persistFunction = persistFunction;
        this.scheduler = scheduler;
        this.backupManagerfileService = backupManagerfileService;
        this.version = persistedBackupManager.getVersion();
        this.backupManagerRepository = backupManagerRepository;
        this.virtualInformation = virtualInformation;

        SpringContext.getBean(MeterRegistry.class).ifPresent(this::buildMetrics);
    }

    public List<Action> getActions() {
        return new ArrayList<>(actions);
    }

    public List<SftpServer> getSftpServers() {
        return new ArrayList<>(sftpServers);
    }

    /**
     * Get the SFTP Server based on index
     * @param index index of the SFTP Server
     * @return the SFTP server
     */
    public Optional<SftpServer> getSftpServer(final int index) {
        if (index >= 0 && index < sftpServers.size()) {
            return Optional.of(sftpServers.get(index));
        } else {
            log.debug("SFTP server with index {} is not found.", index);
            return Optional.empty();
        }
    }

    /**
     * Get the SFTP Server based on its name
     * @param sftpServerName name of the SFTP Server
     * @return the SFTP server
     */
    public Optional<SftpServer> getSftpServer(final String sftpServerName) {
        return getSftpServers().stream()
            .filter(sftpServer -> sftpServer.getName().equals(sftpServerName))
            .findFirst();
    }

    /**
     * Get the backups this BRM has references to, defined by ownership
     * @param ownership - the ownership context of the backups being retrieved
     * @return a list of backups available for write operations
     * */
    public List<Backup> getBackups(final Ownership ownership) {
        switch (ownership) {
            case OWNED:
                return new ArrayList<>(owned);
            case READABLE:
                return new ArrayList<>(readable);
            default:
                throw new NotImplementedException("Unknown ownership: " + ownership);
        }
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public boolean isDefault() {
        return DEFAULT_BACKUP_MANAGER_ID.equals(getBackupManagerId());
    }

    /**
     * Reloads backup manager config from disk
     * @param brmFileService the brm file service used for the reload
     * @throws IOException if the load from disk fails
     * */
    public void reload(final BackupManagerFileService brmFileService) throws IOException {
        final Optional<PersistedBackupManager> maybeLoaded = brmFileService.getPersistedBackupManager(getBackupManagerId());
        if (maybeLoaded.isEmpty()) {
            log.error("Failed to reload persisted backup manager for BRM {}", getBackupManagerId());
            persist();
            // If we're trying to reload this BRM, and we can't, throw
            throw new FileNotFoundException("Failed to find BRM persisted data during BRM reload");
        }
        final PersistedBackupManager loaded = maybeLoaded.get();
        this.backupDomain = loaded.getBackupDomain();
        this.backupType = loaded.getBackupType();
        this.backupManagerId = loaded.getBackupManagerId();
        log.info("Clearing existing SFTP Servers");
        this.sftpServers.clear();
        final List<SftpServer> persistedSftpServers = backupManagerRepository.getPersistedSftpServers(getBackupManagerId());
        log.info("Adding <{}> persisted SFTP Servers to BRM", persistedSftpServers.size());
        this.sftpServers.addAll(persistedSftpServers);
        final String sftpServerNames = persistedSftpServers.stream().map(SftpServer::getName).collect(Collectors.joining(", "));
        log.debug("The following SFTP Servers have been added to the BRM: [ {} ]", sftpServerNames);
        persist();
    }

    /**
     * Get specific action.
     *
     * @param actionId
     *            to look for.
     * @return action.
     */
    public Action getAction(final String actionId) {
        return getActions().stream().filter(action -> actionId.equals(action.getActionId())).findFirst()
                .orElseThrow(() -> new ActionNotFoundException(actionId));
    }

    /**
     * Get parent of backupManager.
     *
     * @return Optional BackupManager
     */
    public Optional<BackupManager> getParent() {
        return virtualInformation.getParentId().isEmpty() ? Optional.empty() :
                Optional.of(backupManagerRepository.getBackupManager(virtualInformation.getParentId()));
    }

    /**
     * Send this BRMs readable backup list with it's children's readable backup lists.
     * Note: this does not write backup data to any external resource e.g. persistence layer or CMM, it only synchronizes
     *       the internal lists.
     * Note: This does not operate recursively, which means if, for example, a list of BRMs is iterated over and
     *       syncChildren called on each of them, and the vBRM hierarchy is more that 1 level deep, the set of backups in
     *       each BRM depends on order of iteration i.e. it is indeterminate. Currently vBRMs are not nested, so this is
     *       not currently an issue.
     */
    public void syncChildren() {
        backupManagerRepository.getChildren(this.backupManagerId).forEach(child -> {
            readable.stream()
                    .filter(b -> !child.hasReadableBackupWithSameId(b))
                    .forEach(b -> child.addBackup(b, Ownership.READABLE));
        });
    }

    /**
     * Get specific backup.
     * @param backupId to look for.
     * @param ownership - the ownership context the backup is being looked for in
     * @return backup.
     */
    public Backup getBackup(final String backupId, final Ownership ownership) {
        return getBackups(ownership).stream().filter(backup -> backupId.equals(backup.getBackupId())).findFirst()
                .orElseThrow(() -> new BackupNotFoundException(backupId));
    }

    /**
     * Get specific backup by Name.
     * @param backupName to look for.
     * @param ownership - the ownership context the backup is being looked for in
     * @return backup.
     */
    public Backup getBackupByName(final String backupName, final Ownership ownership) {
        return getBackups(ownership).stream().filter(backup -> backupName.equals(backup.getName())).findFirst()
                .orElseThrow(() -> new BackupNotFoundException(backupName));
    }

    /**
     * Add action to backupManager.
     * @param action to be added.
     */
    public synchronized void addAction(final Action action) {
        if (actions.stream().anyMatch(existingAction -> existingAction.getActionId().equals(action.getActionId()))) {
            throw new InvalidActionException("Action with the same id already exists");
        }
        actions.add(action);
    }

    /**
     * Removes action.
     * @param action to be removed.
     */
    public synchronized void removeAction(final Action action) {
        actions.remove(action);
    }

    /**
     * Add backup to backupManager.
     * @param ownership the ownership context to add this backup under
     * @param backup to be added.
     */
    public synchronized void addBackup(final Backup backup, final Ownership ownership) {
        // Check if I /or any of my children/ have a readable backup with this name
        if (hasReadableBackupWithSameId(backup) || backupManagerRepository
                .getChildren(getBackupManagerId())
                .anyMatch(b -> b.hasReadableBackupWithSameId(backup))) {
            throw new BackupIdAlreadyExistsException(backup.getBackupId());
        }
        // Adding a backup to the owned list also adds it to the readable list, but not vice versa
        readable.add(backup);
        if (ownership == Ownership.OWNED) {
            owned.add(backup);
        }
    }

    /**
     * Removes backup. Ownership context irrelevant - a removal operation always removes the backup from every context
     * it exists under
     * @param backup to be removed.
     */
    public synchronized void removeBackup(final Backup backup) {
        owned.remove(backup);
        readable.remove(backup);
    }

    /**
     * Adds an SFTP Server to the backup manager
     * @param sftpServer the SFTP server
     */
    public synchronized void addSftpServer(final SftpServer sftpServer) {
        sftpServers.add(sftpServer);
    }

    /**
     * Removes sftpServer.
     * @param sftpServer the SFTP server
     */
    public synchronized void removeSftpServer(final SftpServer sftpServer) {
        sftpServers.remove(sftpServer);
    }

    /**
     * Get backup based in the position. This always relies on the BRMs readable list, so specifying ownership context isn't necessary
     * @param backupIndex to be recovered.
     * @return backup element based in the index
     */
    public Optional<Backup> getBackup(final int backupIndex) {
        return getBackups(Ownership.READABLE)
                .stream()
                .skip(backupIndex)  // Skip the first nth elements
                .findFirst();
    }

    /**
     * Gets index of a backup. This always relies on the BRMs readable list, so specifying ownership context isn't necessary
     * @param backupId to be checked.
     * @return index of backup on Repository or -1 if backupId doesn't exist
     */
    public int getBackupIndex(final String backupId) {
        return IntStream.range(0, getBackups(Ownership.READABLE).size())
                .filter(i -> getBackups(Ownership.READABLE).get(i).getBackupId().equals(backupId))
                .findFirst()
                .orElse(-1);
    }

    /**
     * Persist backupManager.
     */
    public void persist() {
        persistFunction.accept(this);
    }

    /**
     * Indicates if agent belongs to backupManager.
     * @param agentScope scope of agent to be verified.
     * @param agentId - the ID of the agent in question
     * @return true if agent falls under backupManager's scope.
     */
    public boolean ownsAgent(final String agentScope, final String agentId) {
        return isDefault() || agentScopeMatchesBackupManagerId(agentScope) || isVBRMAgent(agentId);
    }

    /**
     * Indicates if backup belongs to backupManager.
     * @param backup name of backup to be verified.
     * @return true if backup is owned by backupManager.
     */
    public boolean ownsBackup(final String backup) {
        return getBackups(Ownership.OWNED).stream()
                .anyMatch(ownedBackup -> ownedBackup.getName().equals(backup));
    }

    private boolean isVBRMAgent(final String agentId) {
        return isVirtual() && agentId != null && virtualInformation.getAgentIds().contains(agentId);
    }

    /**
     * Checks if backup exists in backupManager.
     * @param uri remote path
     */
    public void assertBackupIsNotPresent(final String uri) {
        final File backup = new File(uri);
        final Optional<String> backupID = filterBackupIDFromTarballFormat(backup.getName());
        if (backupID.isPresent() && hasReadableBackupWithSameId(backupID.get())) {
            throw new BackupIdAlreadyExistsException(backupID.get());
        }
    }

    /**
     * Identify the backupID from an URI
     * @param uri Address containing the backup reference
     * @return Optional String with the backupID
     */
    public Optional<String> getBackupID(final String uri) {
        final File backup = new File(uri);
        return filterBackupIDFromTarballFormat(backup.getName());
    }

    /**
     * Returns the backupID from a given string in the tarball format.
     * @param backupName
     *            backupName
     * @return backupID
     */
    public static Optional<String> filterBackupIDFromTarballFormat(final String backupName) {
        final Matcher matcher = PATTERN.matcher(backupName);
        Optional<String> backupID = Optional.empty();
        if (matcher.find()) {
            backupID = Optional.of(matcher.group(1));
        }
        return backupID;
    }

    public boolean isVirtual() {
        return !virtualInformation.getParentId().isEmpty();
    }

    public VirtualInformation getVirtualInformation() {
        return virtualInformation;
    }

    private boolean hasReadableBackupWithSameId(final Backup newBackup) {
        return getBackups(Ownership.READABLE).stream().anyMatch(backup -> backup.getBackupId().equals(newBackup.getBackupId()));
    }

    private boolean hasReadableBackupWithSameId(final String backupID) {
        return getBackups(Ownership.READABLE).stream().anyMatch(backup -> backup.getBackupId().equals(backupID));
    }

    private boolean agentScopeMatchesBackupManagerId(final String agentScope) {
        if (agentScope == null) {
            return false;
        }
        return Arrays.stream(agentScope.split(SCOPE_SEPARATOR)).anyMatch(scope -> backupManagerId.equals(scope));
    }

    private void buildMetrics(final MeterRegistry registry) {
        createMetricNumberOfBackups(registry);
        if (!isOSMNEnabled()) {
            createMetricTotalSizeOfBackups(registry);
        }

        // Metric for information of last executed action
        getLastAction().ifPresent(lastAction -> {
            createInformationMetricForLastExecutedAction(registry, lastAction);
            completionTimeForLastExecutedAction(registry, lastAction);
        });
    }

    private boolean isOSMNEnabled() {
        final Optional<S3Config> s3Config = SpringContext.getBean(S3Config.class);
        if (s3Config.isEmpty()) {
            return false;
        } else {
            return s3Config.get().isEnabled();
        }
    }

    private void completionTimeForLastExecutedAction(final MeterRegistry registry, final Action lastAction) {
        // For some Actions (e.g. failed IMPORT), getCompletionTime()
        // can be null so return 0 for completion time
        final var completionTime = lastAction.getCompletionTime() == null ? 0 : lastAction.getCompletionTime().toEpochSecond();
        METRIC_OPERATION_ENDTIME.unRegister();
        Gauge.builder(METRIC_OPERATION_ENDTIME.identification(), () -> completionTime)
                .description(METRIC_OPERATION_ENDTIME.description())
                .tag(ACTION_ID.identification(), lastAction.getActionId())
                .tag(ACTION.identification(), lastAction.getName().name())
                .tag(BACKUP_TYPE.identification(), lastAction.getBackupManagerId())
                .tag(STATUS.identification(), lastAction.getResult().name())
                .register(registry);
    }

    private void createInformationMetricForLastExecutedAction(final MeterRegistry registry, final Action lastAction) {
        // For some Action types (e.g. IMPORT), getBackupName()
        // throws an UnprocessableEntityException exception
        String backupName;
        try {
            backupName = lastAction.getBackupName();
        } catch (final UnprocessableEntityException ex) {
            backupName = "None";
        }
        METRIC_BRO_OPERATION_INFO.unRegister();
        Gauge.builder(METRIC_BRO_OPERATION_INFO.identification(), () -> 1)
                .description(METRIC_BRO_OPERATION_INFO.description())
                .tag(ACTION.identification(), lastAction.getName().name())
                .tag(BACKUP_TYPE.identification(), lastAction.getBackupManagerId())
                .tag(BACKUP_NAME.identification(), backupName)
                .tag(ACTION_ID.identification(), lastAction.getActionId())
                .tag(STATUS.identification(), lastAction.getResult().name())
                .tag(ADDITIONAL_INFO.identification(), lastAction.getAdditionalInfo() == null ? "None" : lastAction.getAdditionalInfo())
                .register(registry);
    }

    private void createMetricTotalSizeOfBackups(final MeterRegistry registry) {
        // Metric for monitoring total size of backups files on disk for a backup manager
        METRIC_BRO_DISK_USAGE_BYTES.unRegister();
        Gauge.builder(METRIC_BRO_DISK_USAGE_BYTES.identification(),
                backupManagerfileService, b -> backupManagerfileService.getBackupManagerBackupsFolderSize(backupManagerId))
                .description(METRIC_BRO_DISK_USAGE_BYTES.description())
                .baseUnit("bytes")
                .tag(BACKUP_TYPE.identification(), backupManagerId)
                .register(registry);
    }


    private void createMetricNumberOfBackups(final MeterRegistry registry) {
        // Metric for monitoring number of backups saved by this backup manager
        METRIC_BRO_STORED_BACKUPS.unRegister();
        Gauge.builder(METRIC_BRO_STORED_BACKUPS.identification(), this, manager -> manager.getBackups(Ownership.OWNED).size())
                .description(METRIC_BRO_STORED_BACKUPS.description())
                .baseUnit("backups")
                .tag(BACKUP_TYPE.identification(), backupManagerId)
                .register(registry);
    }

    /**
     * Gets the last action associated with a backup resource for a certain actionType
     * @param backup the backup resource
     * @param actionType the type of action
     * @return the last action with a certain actionType associated with the backup.
     */
    public Optional<Action> getLastAction(final Backup backup, final ActionType actionType) {
        return actions.stream()
            .filter(action -> action.getName().equals(actionType))
            .filter(action -> (action.getBackupName().equals(backup.getName())))
            .sorted( Comparator.comparing(Action::getStartTime).reversed())
            .findFirst();
    }

    /**
     * Gets last performed task/action on the backup manager
     * @return Optional of Action
     */
    public Optional<Action> getLastAction() {
        // If the last action was in the ignore list e.g.
        // HOUSEKEEPING, then search until an allowed action is found
        for (int i = actions.size() - 1; i >= 0; i--) {
            final var action = actions.get(i);
            switch (action.getName()) {
                case CREATE_BACKUP:
                case DELETE_BACKUP:
                case RESTORE:
                case IMPORT:
                case EXPORT:
                    return Optional.of(action);
                default:
            }
        }
        return Optional.empty();
    }

    /**
     * Gets last completed task/action on the backup manager
     * @return Optional of Action
     */
    public Optional<Action> getLastCompletedAction() {
        final Optional<Action> action = actions.stream()
                                  .filter(a -> a.getState().equals(ActionStateType.FINISHED))
                                  .sorted(Comparator.comparing(Action::getCompletionTime).reversed()).findFirst();
        return action;
    }

    public Housekeeping getHousekeeping() {
        return housekeeping;
    }

    public String getAgentVisibleBRMId() {
        return isVirtual() ? getVirtualInformation().getParentId() : getBackupManagerId();
    }

    public boolean isBackupManagerLevelProgressReportCreated() {
        return backupManagerLevelProgressReportCreated.get();
    }

    /**
     * Sets a flag to indicate the backup manager level progress report has been created in CMM.
     */
    public void backupManagerLevelProgressReportSetCreated() {
        backupManagerLevelProgressReportCreated.compareAndSet(false, true);
    }

    /**
     * Sets a flag to mark that the backup manager level progress report needs to be created in CMM.
     */
    public void backupManagerLevelProgressReportResetCreated() {
        backupManagerLevelProgressReportCreated.set(false);
    }

    @Override
    @JsonIgnore
    public Version<PersistedBackupManager> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<PersistedBackupManager> version) {
        this.version = version;
    }

    @Override
    public boolean equals(final Object backupManagerToCompare) {
        if (this == backupManagerToCompare) {
            return true;
        }
        if (backupManagerToCompare == null || getClass() != backupManagerToCompare.getClass()) {
            return false;
        }
        final BackupManager backupManager = (BackupManager) backupManagerToCompare;
        // if is the same backupmanagerId, is the same object
        return getBackupManagerId().equals(backupManager.getBackupManagerId());
    }

    @Override
    public int hashCode() {
        final int hashCode = this.backupManagerId.hashCode() +
                this.housekeeping.hashCode() +
                this.persistFunction.hashCode() +
                this.scheduler.hashCode() +
                this.backupManagerRepository.hashCode() +
                this.virtualInformation.hashCode();;
        return hashCode;
    }

    @Override
    public String toString() {
        return "BackupManager [actions=" + actions + ", owned=" + owned + ", readable=" + readable + ", sftpServers=" + sftpServers
                + ", persistFunction=" + persistFunction + ", housekeeping=" + housekeeping + ", scheduler=" + scheduler
                + ", backupManagerLevelProgressReportCreated=" + backupManagerLevelProgressReportCreated + ", backupManagerfileService="
                + backupManagerfileService + ", version=" + version + ", backupManagerRepository=" + backupManagerRepository + ", virtualInformation="
                + virtualInformation + "]";
    }

}
