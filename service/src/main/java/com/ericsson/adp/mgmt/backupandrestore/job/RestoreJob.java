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
package com.ericsson.adp.mgmt.backupandrestore.job;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.exception.AgentsNotAvailableException;
import com.ericsson.adp.mgmt.backupandrestore.exception.RestoreDownloadException;
import com.ericsson.adp.mgmt.backupandrestore.exception.SemanticVersionNullValueException;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnauthorizedDataChannelException;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.ProductInfoService;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.exception.UnableToRetrieveDataFromConfigmapException;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.exception.UnsupportedSoftwareVersionException;
import com.ericsson.adp.mgmt.backupandrestore.restore.FragmentFileService;
import com.ericsson.adp.mgmt.backupandrestore.restore.RestoreInformation;
import com.ericsson.adp.mgmt.data.Metadata;

import io.kubernetes.client.openapi.ApiException;

/**
 * Responsible for performing a restore.
 */
public class RestoreJob extends JobWithStages<RestoreJob> {

    private static final Logger log = LogManager.getLogger(RestoreJob.class);
    private static final String APP_PRODUCT_INFO_ID = "APPLICATION_INFO";
    private static final String EXACT_MATCH_TYPE = "EXACT_MATCH";
    private static final String LIST_MATCH_TYPE = "LIST";
    private static final String GREATER_THAN_MATCH_TYPE = "GREATER_THAN";
    private BackupLocationService backupLocationService;
    private FragmentFileService fragmentFileService;
    private Backup backup;
    private ProductInfoService productInfoService;
    private boolean backupCorrupted;

    /**
     * creates restoreInformation for the restore action for agent.
     * @param agentId - agentId.
     * @return restoreInformation.
     */
    public RestoreInformation createRestoreInformation(final String agentId) {
        BackupManager agentFacing = backupManager;
        final Optional<BackupManager> parentBackupManager = backupManager.getParent();
        if (isVbrmRestoringParentBackup() && parentBackupManager.isPresent()) {
            agentFacing =  parentBackupManager.get();
        }
        final Optional<Agent> agent = agents.stream().filter(agent1 -> agent1.getAgentId().equals(agentId)).findFirst();
        return new RestoreInformation(backup, agentFacing, agentId, fragmentFileService, agent);
    }

    private boolean isVbrmRestoringParentBackup() {
        return backupManager.isVirtual() && !backupManager.ownsBackup(backup.getName());
    }

    /**
     * Where data regarding fragment is stored.
     * @param metadata - contains fragment information.
     * @return where fragment data is stored.
     */
    @Override
    public FragmentFolder getFragmentFolder(final Metadata metadata) {
        if (!metadataBelongsToBackup(metadata)) {
            throw new UnauthorizedDataChannelException(backup.getBackupId(), metadata);
        }
        if (isVbrmRestoringParentBackup()) {
            return backupLocationService.getFragmentFolder(metadata, backupManager.getAgentVisibleBRMId(), backup.getName());
        } else {
            return backupLocationService.getFragmentFolder(metadata, backupManager.getBackupManagerId(), backup.getName());
        }
    }

    @Override
    protected void triggerJob() {
        backup = backupManager.getBackup(action.getBackupName(), Ownership.READABLE);
        validate();
        buildJobPerfMetric();
        jobStage.trigger();
    }

    @Override
    protected void completeJob() {
        forceResetCM();
        super.completeJob();
    }

    @Override
    protected void fail() {
        super.fail();
        if (backupCorrupted) {
            log.info("Changing backup status to CORRUPTED: {}", backup.getName());
            backup.setStatus(BackupStatus.CORRUPTED);
            backup.persist();
        }
        forceResetCM();
    }

    private boolean metadataBelongsToBackup(final Metadata metadata) {
        return backup.getBackupId().equals(metadata.getBackupName())
                && agents.stream().anyMatch(agent -> agent.getAgentId().equals(metadata.getAgentId()));
    }

    private void validate() {
        validateBackupStatus();
        ensureAgentsAreAvailable();
        validateProductNumber();
    }

    private void validateBackupStatus() {
        if (!backup.getStatus().equals(BackupStatus.COMPLETE)) {
            throw new RestoreDownloadException(
                    "Cannot restore backup <" + backup.getBackupId() + ">, status - " + backup.getStatus().toString());
        }
    }

    private void ensureAgentsAreAvailable() {
        final Set<String> agentIdsToBeRestored = getAgentIdsToBeRestored();
        final Set<String> idsOfRestoreAgents = getIdsOfRestoregents();
        if (isVbrmRestoringParentBackup()) {
            if (idsOfRestoreAgents.isEmpty()) {
                final String errorMessage = "The virtual backup manager: " + backupManager.getBackupManagerId()
                        + " does not have at least one of the following agents registered:\n" + agentIdsToBeRestored;
                throw new AgentsNotAvailableException(errorMessage);
            }
        } else {
            final Set<String> unavailableAgents = new HashSet<>(agentIdsToBeRestored);
            unavailableAgents.removeAll(idsOfRestoreAgents);
            if (!unavailableAgents.isEmpty()) {
                throw new AgentsNotAvailableException(unavailableAgents, backupManager.getBackupManagerId());
            }
        }
    }

    private void validateProductNumber() {
        final String selectedMatchType = productInfoService.getSelectedMatchType();

        if ("PRODUCT".equals(selectedMatchType)) {
            final String productMatchType = productInfoService.getProductMatchType();
            final Consumer<SoftwareVersion> exactMatcher = backupVersion -> {
                log.info("Comparing backup product number, " + backupVersion.getProductNumber() +
                         ", to configmap product number, " + getProductNumberFromConfigmap());
                if (!backupVersion.getProductNumber().equals(getProductNumberFromConfigmap())) {
                    throw new UnsupportedSoftwareVersionException();
                }
            };
            final Consumer<SoftwareVersion> listMatcher = backupVersion -> {
                log.info("Comparing backup product number, " + backupVersion.getProductNumber() +
                         ", to list of product numbers, " + productInfoService.getProductNumberList());
                if (!productInfoService.getProductNumberList().contains(backupVersion.getProductNumber())) {
                    throw new UnsupportedSoftwareVersionException();
                }
            };
            if (EXACT_MATCH_TYPE.equals(productMatchType)) {
                validateProductNumber(exactMatcher);
            } else if (LIST_MATCH_TYPE.equals(productMatchType)) {
                validateProductNumber(listMatcher);
            }
        } else if ("SEMVER".equals(selectedMatchType)) {
            final String semVerMatchType = productInfoService.getSemVerMatchType();
            final String lowestAllowedVersion = productInfoService.getProductLowestAllowedVersion();

            final Consumer<SoftwareVersion> anyMatcher = backupVersion -> {
                log.info("Comparing backup semantic version, " + backupVersion.getSemanticVersion() +
                            ", to lowest allowed semantic version, " + lowestAllowedVersion);
                if (compareVersions(backupVersion.getSemanticVersion(), lowestAllowedVersion) == -1) {
                    throw new UnsupportedSoftwareVersionException();
                }
            };
            final Consumer<SoftwareVersion> greaterThanMatcher = backupVersion -> {
                log.info("Comparing backup semantic version, " + backupVersion.getSemanticVersion() +
                         ", to configmap semantic version, " + getSemVerFromConfigmap());
                if (compareVersions(backupVersion.getSemanticVersion(), getSemVerFromConfigmap()) == 1) {
                    throw new UnsupportedSoftwareVersionException();
                }
            };

            validateProductNumber(anyMatcher);

            if (GREATER_THAN_MATCH_TYPE.equals(semVerMatchType)) {
                validateProductNumber(greaterThanMatcher);
            }
        }
    }

    private void validateProductNumber(final Consumer<SoftwareVersion> consumer) {
        backup.getSoftwareVersions()
        .stream()
        .filter(softwareVersion -> softwareVersion.getAgentId().equals(APP_PRODUCT_INFO_ID))
        .findFirst().ifPresent(consumer);
    }

    private static int compareVersions(final String semVer1, final String semVer2) {
        try {
            int comparisonResult = 0;
            final String[] semVer1Split = Optional.ofNullable(semVer1).
                    orElseThrow(() -> new SemanticVersionNullValueException("backup")).split("\\.");
            final String[] semVer2Split = Optional.ofNullable(semVer2).
                    orElseThrow(() -> new SemanticVersionNullValueException("Product Config Map")).split("\\.");
            final int maxLengthOfVersionSplits = Math.max(semVer1Split.length, semVer2Split.length);

            for (int i = 0; i < maxLengthOfVersionSplits; i++) {
                final Integer version1 = i < semVer1Split.length ? Integer.parseInt(semVer1Split[i]) : 0;
                final Integer version2 = i < semVer2Split.length ? Integer.parseInt(semVer2Split[i]) : 0;
                final int compare = version1.compareTo(version2);
                if (compare != 0) {
                    comparisonResult = compare;
                    break;
                }
            }
            return comparisonResult;
        } catch (final SemanticVersionNullValueException exception) {
            log.error("The backup failed to restore because " + exception.getMessage());
            return -1;
        } catch (final Exception exception) {
            log.error("The backup failed to restore because the compare version method received an invalid semantic version");
            throw exception;
        }
    }

    private String getProductNumberFromConfigmap() {
        try {
            final String productNumber = productInfoService.getAppProductInfo().getProductNumber();
            log.info("Product Info Configmap found with product number: {}", productNumber);
            return productNumber;
        } catch (final Exception exception) {
            throw new UnableToRetrieveDataFromConfigmapException(
                    "Unable to retrieve product information from product info configmap during restore", exception);
        }
    }

    private String getSemVerFromConfigmap() {
        try {
            final String semVer = productInfoService.getAppProductInfo().getSemanticVersion();
            log.info("Product Info Configmap found with semantic version: {}", semVer);
            return semVer;
        } catch (final ApiException exception) {
            throw new UnableToRetrieveDataFromConfigmapException(
                    "Error loading product info configmap during restore ", exception);
        } catch (final Exception exception) {
            throw new UnableToRetrieveDataFromConfigmapException(
                    "Unable to retrieve semantic version from product info configmap during restore", exception);
        }
    }

    private Set<String> getAgentIdsToBeRestored() {
        BackupManager ownerOfBackupToRestore = backupManager;
        final Optional<BackupManager> backupManagerParent = backupManager.getParent();
        if (isVbrmRestoringParentBackup() && backupManagerParent.isPresent()) {
            ownerOfBackupToRestore = backupManagerParent.get();
        }
        final List<String> agentIds = backupLocationService.getAgentIdsForBackup(ownerOfBackupToRestore, backup.getBackupId());
        return new HashSet<>(agentIds);
    }

    private Set<String> getIdsOfRestoregents() {
        return agents.stream().map(Agent::getAgentId).collect(Collectors.toSet());
    }

    private void forceResetCM() {
        resetCM(false);
    }

    protected void setBackupLocationService(final BackupLocationService backupLocationService) {
        this.backupLocationService = backupLocationService;
    }

    protected void setFragmentFileService(final FragmentFileService fragmentFileService) {
        this.fragmentFileService = fragmentFileService;
    }

    public void setProductInfoService(final ProductInfoService productInfoService) {
        this.productInfoService = productInfoService;
    }

    /**
     * Marks the backup being restored as corrupted.
     */
    public void markBackupAsCorrupted() {
        this.backupCorrupted = true;
    }

}
