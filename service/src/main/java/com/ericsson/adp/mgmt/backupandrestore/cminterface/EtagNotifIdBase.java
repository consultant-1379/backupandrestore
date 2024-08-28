/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.cminterface;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_MANAGER_POSITION_IN_CONTEXT;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_POSITION_IN_CONTEXT;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.action.yang.UnprocessableYangRequestException;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMBackupJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMBackupManagerJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMEricssonbrmJson;

/**
 * Class used to keep a track of the last request patch executed
 */
@Service
public class EtagNotifIdBase {

    private static final Logger log = LogManager.getLogger(EtagNotifIdBase.class);

    private volatile Optional<String> lastCMMEtag;
    private volatile Optional<Integer> lastNotifId;
    private volatile Optional<String> configuration;
    private BackupManagerRepository backupManagerRepository;

    /**
     * Constructor
     * eTag and notifId are set Optional empty
     */
    public EtagNotifIdBase() {
        this(null, null, null);
    }

    /**
     * Constructor used in tests
     * @param eTag initial value
     * @param notifId initial value
     * @param configuration current configuration for etag
     * eTag and notifId are set empty
     */
    public EtagNotifIdBase(final String eTag, final Integer notifId, final String configuration) {
        super();
        lastCMMEtag = Optional.ofNullable(eTag);
        lastNotifId = Optional.ofNullable(notifId);
        this.configuration = Optional.ofNullable(configuration);
    }

    /**
     * Update the last eTag received
     * @param eTag last eTag received
     */
    public synchronized void updateEtag(final String eTag) {
        lastCMMEtag = Optional.of(eTag);
    }

    /**
     * Update the last last notifID
     * @param notifId last notifId received
     */
    public synchronized void setNotifId(final Integer notifId) {
        lastNotifId = Optional.of(notifId);
    }

    /**
     * Validate the mediator request received to be processed
     * The new notifId should be greater than the lastnotifId
     * The Etag to be compared should be the same as the last etag processed
     * @param eTag last eTag received
     * @param notifId last notifId received
     * @return true if etag and notifId are valid
     */
    public boolean isValidMediatorRequest(final String eTag, final Integer notifId) {
        if (lastNotifId.isPresent() && lastNotifId.get() >= notifId ) {
            log.warn("The Etag notification Id {} received is lower than the last mediator notifId {} processed", notifId, lastNotifId.get());
            return false;
        }

        // Check if the message already exists
        if (lastCMMEtag.isPresent() && ! lastCMMEtag.get().equals(eTag)) {
            log.warn("The baseEtag  {} is different to last baseEtag expected {}, mediator notification is invalid", eTag, lastCMMEtag.get());
            return false;
        }
        return true;
    }

    /**
     * Retrieves etag from base
     * @return last eTag keep
     */
    public String getEtag() {
        if (lastCMMEtag.isPresent()) {
            return lastCMMEtag.get();
        }
        return "";
    }

    /**
     * Retrieves eNotifId defined in base
     * @return the last eNotifId defined in the base
     */
    public synchronized Integer getNotifId() {
        if (lastNotifId.isPresent()) {
            return lastNotifId.get();
        }
        return 0;
    }

    /**
     * Get the last configuration from CMM
     * @return Configuration
     */
    public synchronized String getConfiguration() {
        if (configuration.isPresent()) {
            return configuration.get();
        }
        return "";
    }

    public synchronized void setConfiguration(final String configuration) {
        this.configuration = Optional.ofNullable(configuration);
    }

    /**
     * Cross reference the index from the CMM Configuration into the Index in the local BackupManagers
     * @param context including the backupmanager from CMM
     * @param configETag Etag received from CMM
     * @return cross-reference backup manager Index from BRO repository
     */
    public int getIndexBackupManager(final String context, final String configETag) {
        try {
            final Optional<BRMBackupManagerJson> brmBackupManagerJson = getBRMBackupManagerJson(context, configETag);
            // returns the Index from backupmanager repository
            if (brmBackupManagerJson.isPresent()) {
                return backupManagerRepository.getIndex(brmBackupManagerJson.get().getBackupManagerId());
            } else {
                return Integer.valueOf(context.split("/")[BACKUP_MANAGER_POSITION_IN_CONTEXT]);
            }
        } catch (final Exception e) {
            throw new UnprocessableYangRequestException("Invalid context", e);
        }
    }

    /**
     * Cross reference the index from the CMM Configuration into the Index in the local BackupManagers/backups
     * @param context including the backupmanager/backup from CMM
     * @param configETag Etag received from CMM
     * @return backup Index from BRO Repository
     */
    public int getIndexBackupManagerBackup(final String context, final String configETag) {
        try {
            final int requestPositionBackup = Integer.valueOf(context.split("/")[BACKUP_POSITION_IN_CONTEXT]);
            final Optional<BRMBackupManagerJson> optionalBrmBackupManagerJson = getBRMBackupManagerJson(context, configETag);
            if (optionalBrmBackupManagerJson.isPresent()) {
                final BRMBackupManagerJson brmBackupManagerJson = optionalBrmBackupManagerJson.get();
                final Optional<BRMBackupJson> brmBackupJson = brmBackupManagerJson.getBackups().stream()
                        .skip(requestPositionBackup)  // Skip the first nth elements
                        .findFirst();
                if (brmBackupJson.isPresent()) {
                    return backupManagerRepository.getBackupManager(brmBackupManagerJson.getBackupManagerId())
                            .getBackupIndex(brmBackupJson.get().getBackupId());
                }
            }
            return Integer.valueOf(context.split("/")[BACKUP_POSITION_IN_CONTEXT]);
        } catch (final Exception e) {
            throw new UnprocessableYangRequestException("Invalid context", e);
        }
    }

    /**
     * get the backupIndex from CMM using the backup id from repository
     * @param cmmMessagePath path from CMM
     * @param pathPositionBackupManagerRepository current backupManager position in repository
     * @return CMM backupPosition, -1 if backupManager is not present in CMM, original position if configuration is not provided
     */
    public int getCMMIndexBackupManagerBackup(final String cmmMessagePath, final int pathPositionBackupManagerRepository) {
        final int pathPositionBackup;

        try {
            pathPositionBackup = Integer.valueOf(cmmMessagePath.split("/")[BACKUP_POSITION_IN_CONTEXT]);
        } catch (Exception e) {
            return -1;
        }

        final Optional<BRMEricssonbrmJson> brmEricssonbrmJson =
                backupManagerRepository.getBRMConfiguration(getConfiguration());
        // If BRM Configuration is not present
        if (brmEricssonbrmJson.isPresent() && pathPositionBackup >= 0) {
            // get the current backup position in the backupManager repository
            final Optional<Backup> backup = backupManagerRepository.
                    getBackupManager(pathPositionBackupManagerRepository).getBackup(pathPositionBackup);
            // Get Remote CMM Index
            final int cmmbackupManager = getCMMIndexBackupManager(cmmMessagePath);
            if (cmmbackupManager >= 0) {
                final Optional<BRMBackupManagerJson> brmBackupManagerJson = brmEricssonbrmJson.get().getBRMConfiguration().
                        getBrm().getBackupManagers().stream()
                        .skip(cmmbackupManager) // Skip the first nth elements for BM
                        .findFirst();
                //is backup is registered in backupManagerRepository, it looks into the CMM index
                if (brmBackupManagerJson.isPresent() && backup.isPresent()) {
                    // search for the backup Index in CMM
                    log.debug("Searching for backup <{}> in CMM", backup.get().getBackupId());
                    final List<BRMBackupJson>  backups = brmBackupManagerJson.get().getBackups();
                    return IntStream.range(0, backups.size())
                            .filter(i -> backups.get(i).getBackupId().equals(backup.get().getBackupId()))
                            .findFirst()
                            .orElse(-1);
                }
            } else {
                // NO backup manager is present in CMM
                return -1;
            }
        }

        return pathPositionBackup;
    }

    /**
     * Cross-reference to retrieve the CMM backupManager index position using the BRO bm repository
     * @param cmmMessagePath Message path including the repository BM position
     * @return index CMM position
     */
    public int getCMMIndexBackupManager(final String cmmMessagePath) {
        final int pathbackUpManager;
        try {
            pathbackUpManager = Integer.valueOf(cmmMessagePath.split("/")[BACKUP_MANAGER_POSITION_IN_CONTEXT]);
        } catch (Exception e) {
            return -1;
        }
        final String backupManagerId = backupManagerRepository.getBackupManager(pathbackUpManager).getBackupManagerId();

        final Optional<BRMEricssonbrmJson> brmEricssonbrmJson =
                backupManagerRepository.getBRMConfiguration(getConfiguration());
        if (brmEricssonbrmJson.isPresent()) {
            final Optional<BRMConfiguration> brmConfiguration = Optional.ofNullable(brmEricssonbrmJson.get().getBRMConfiguration());
            if (brmConfiguration.isEmpty()) {
                return -1;
            }
            final List<BRMBackupManagerJson> backupManagers = brmEricssonbrmJson.get().getBRMConfiguration().
                    getBrm().getBackupManagers();
            final String result = IntStream.range(0, backupManagers.size())
                    .mapToObj(index -> index + "--> " + backupManagers.get(index).getBackupManagerId())
                    .collect(Collectors.joining(", "));
            log.info("Searching for backup manager <{}> in CMM : <{}>", backupManagerId, result);
            return IntStream.range(0, backupManagers.size())
                    .filter(i -> backupManagers.get(i).getBackupManagerId().equals(backupManagerId))
                    .findFirst()
                    .orElse(-1);
        } else {
            return pathbackUpManager;
        }
    }

    /**
     * Returns the BackupManagerJson object from configuration, using the CMM Context as base
     * @param cmmContext context get from CMM
     * @param configETagfromCMM Etag received from CMM
     * @return BRMBackupManagerJson pointing the Configuration backup Manager details
     */
    private Optional<BRMBackupManagerJson> getBRMBackupManagerJson(final String cmmContext, final String configETagfromCMM) {
        final int requestPositionManager = Integer.valueOf(cmmContext.split("/")[BACKUP_MANAGER_POSITION_IN_CONTEXT]);
        if (configETagfromCMM != null && !configETagfromCMM.isBlank()) {
            // Compares from current Etag vs the Etag from CMM
            if (!getEtag().equalsIgnoreCase(configETagfromCMM)) {
                // Update the last Etag and get the configuration
                backupManagerRepository.getLastEtagfromCMM();
            }
        }
        // converts Configuration into an object
        final Optional<BRMEricssonbrmJson> brmEricssonbrmJson =
                backupManagerRepository.getBRMConfiguration(getConfiguration());
        if (brmEricssonbrmJson.isPresent()) {
            return brmEricssonbrmJson.get().getBRMConfiguration().
                    getBrm().getBackupManagers().stream()
                    .skip(requestPositionManager) // Skip the first nth elements for BM
                    .findFirst();
        } else {
            return Optional.empty();
        }
    }

    @Autowired
    @Lazy
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    @Override
    public String toString() {
        return "EtagNotifIdBase [lastCMMEtag=" + lastCMMEtag + ", lastNotifId=" + lastNotifId + ", configuration=" + configuration + "]";
    }
}
