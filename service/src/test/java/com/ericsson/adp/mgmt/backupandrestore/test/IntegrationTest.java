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
package com.ericsson.adp.mgmt.backupandrestore.test;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_MANAGER_CONFIG_BACKUP_FOLDER;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MyTestConfiguration.class)
public abstract class IntegrationTest {

    protected static final Path TEMP_FOLDER = Paths.get(System.getProperty("java.io.tmpdir"));
    protected static final Path BACKUP_MANAGERS_LOCATION = TEMP_FOLDER.resolve(BACKUP_MANAGER_CONFIG_BACKUP_FOLDER);
    protected static final Path BACKUP_DATA_LOCATION = TEMP_FOLDER.resolve("backups");
    protected static final Path DUMMY_LOCATION = TEMP_FOLDER.resolve("reservedspace");

    private static boolean performedSetup;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        if (!performedSetup) {
            performedSetup = true;
            resetFolder(BACKUP_MANAGERS_LOCATION);
            resetFolder(BACKUP_DATA_LOCATION);
            resetFolder(DUMMY_LOCATION);
            initializeBackupManagers();
        }
    }

    private static void resetFolder(final Path folder) throws Exception {
        if (folder.toFile().exists()) {
            try (Stream<Path> files = Files.walk(folder)) {
                files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
        Files.createDirectories(folder);
    }

    private static void initializeBackupManagers() throws Exception {
        final String backupManagerId = "backupManagerWithBackupToDelete";
        final String backupManagerId2 = "backupManagerWithoutHousekeepingInfo";
        final String backupManagerId3 = "backupManagerWithSchedulerInfo";

        final Path backupManagerFolder = BACKUP_MANAGERS_LOCATION.resolve(backupManagerId);
        final String housekeepingInformation = new JSONObject()
                .put("max-stored-manual-backups", 1)
                .put("auto-delete", AUTO_DELETE_ENABLED)
                .toString();
        final String schedulerInfo = new JSONObject()
                .put("adminState", "UNLOCKED")
                .put("scheduledBackupName", "SCHEDULED_BUP")
                .put("nextScheduledTime", null)
                .put("mostRecentlyCreatedAutoBackup", null)
                .toString();
        final String periodicEvent = new JSONObject()
                .put("id", "periodic")
                .put("hours", 2)
                .put("minutes", 3)
                .put("weeks", 0)
                .put("days", 0)
                .put("startTime", "2020-09-30T11:23:22.265Z").put("stopTime", "2020-10-03T11:23:22.265Z")
                .toString();
        final String backupManagerInformation = new JSONObject()
                .put("id", backupManagerId)
                .toString();

        Files.createDirectories(backupManagerFolder.resolve("periodic-events"));
        Files.write(backupManagerFolder.resolve("backupManagerInformation.json"), (backupManagerInformation).getBytes());
        Files.write(backupManagerFolder.resolve("housekeepingInformation.json"), (housekeepingInformation).getBytes());
        Files.write(backupManagerFolder.resolve("schedulerInformation.json"), schedulerInfo.getBytes());
        Files.write(backupManagerFolder.resolve("periodic-events").resolve("periodic.json"), periodicEvent.getBytes());


        final Path backupManagerFolder2 = BACKUP_MANAGERS_LOCATION.resolve(backupManagerId2);
        Files.createDirectories(backupManagerFolder2);
        Files.write(backupManagerFolder2.resolve("backupManagerInformation.json"), ("{\"id\":\"" + backupManagerId2 + "\"}").getBytes());
        Files.write(backupManagerFolder2.resolve("schedulerInformation.json"), schedulerInfo.getBytes());

        final Path backupManagerFolder3 = BACKUP_MANAGERS_LOCATION.resolve(backupManagerId3);
        Files.createDirectories(backupManagerFolder3);
        Files.write(backupManagerFolder3.resolve("backupManagerInformation.json"), ("{\"id\":\"" + backupManagerId3 + "\"}").getBytes());
        Files.write(backupManagerFolder3.resolve("schedulerInformation.json"), schedulerInfo.getBytes());
        Files.write(backupManagerFolder3.resolve("housekeepingInformation.json"), (housekeepingInformation).getBytes());


        createBackup("housekeepingBackup", backupManagerId2, backupManagerFolder2);

        createBackupMetadata("emptyBackup", backupManagerId, backupManagerFolder);
        createBackup("backupToDelete", backupManagerId, backupManagerFolder);
        createBackup("backupToKeep", backupManagerId, backupManagerFolder);
        createBackup("otherBackupToDelete", backupManagerId, backupManagerFolder);
        createBackup("v4BackupToDelete", backupManagerId, backupManagerFolder);


        createBackupMetadata("incompleteBackup", backupManagerId, "INCOMPLETE", backupManagerFolder);
        createActionMetadata("runningAction", backupManagerFolder, "myBackup", "NOT_AVAILABLE", "RUNNING");
    }

    private static void createBackup(final String backupId, final String backupManagerId, final Path backupManagerFolder) throws Exception {
        createBackupMetadata(backupId, backupManagerId, backupManagerFolder);

        final Path fragmentDataFolder = BACKUP_DATA_LOCATION.resolve(backupManagerId).resolve(backupId).resolve("1").resolve("A");
        Files.createDirectories(fragmentDataFolder);
        Files.write(fragmentDataFolder.resolve("Fragment.json"),
                ("{\"fragmentId\":\"A\",\"version\":\"version\",\"sizeInBytes\":\"bytes\"}").getBytes());
        Files.write(fragmentDataFolder.resolve("BackupFile"), ("AAAAAAAAAAAAAAAAA").getBytes());

        createActionMetadata(backupId + "Action", backupManagerFolder, backupId, "SUCCESS", "FINISHED");

    }

    private static void createBackupMetadata(final String backupId, final String backupManagerId, final Path backupManagerFolder) throws IOException {
        createBackupMetadata(backupId, backupManagerId, "COMPLETE", backupManagerFolder);
    }

    private static void createBackupMetadata(final String backupId, final String backupManagerId, final String backupStatus, final Path backupManagerFolder) throws IOException {
        final Path backupFolder = backupManagerFolder.resolve("backups");
        Files.createDirectories(backupFolder);
        Files.write(backupFolder.resolve(backupId + ".json"), getBackupMetadata(backupId, backupManagerId, backupStatus).getBytes());
    }

    private static String getBackupMetadata(final String backupId, final String backupManagerId, final String status) {

        final StringBuilder backupMetaDataBuilder = new StringBuilder();
        backupMetaDataBuilder.append("{\"backupId\":\"").append(backupId).append("\",");
        backupMetaDataBuilder.append("\"name\":\"").append(backupId).append("\",");
        backupMetaDataBuilder.append("\"creationType\":\"").append("MANUAL").append("\",");
        backupMetaDataBuilder.append("\"status\":\"").append(status).append("\",");
        backupMetaDataBuilder.append("\"userLabel\":").append("null").append(",");
        backupMetaDataBuilder.append("\"softwareVersions\":").append("[]").append(",");
        backupMetaDataBuilder.append("\"creationTime\":\"").append("2019-06-10T10:21:12.428Z").append("\",");
        backupMetaDataBuilder.append("\"backupManagerId\":\"").append(backupManagerId).append("\"}");

        return backupMetaDataBuilder.toString();
    }

    private static void createActionMetadata(final String actionId, final Path backupManagerFolder,
                                             final String backupName, final String result, final String state) throws Exception {
        final Path backupFolder = backupManagerFolder.resolve("actions");
        Files.createDirectories(backupFolder);

        final StringBuilder actionMetaDataBuilder = new StringBuilder();
        actionMetaDataBuilder.append("{\"name\":\"").append("CREATE_BACKUP").append("\",");
        actionMetaDataBuilder.append("\"result\":\"").append(result).append("\",");
        actionMetaDataBuilder.append("\"payload\":").append("{\"backupName\":\"").append(backupName).append("\"}").append(",");
        actionMetaDataBuilder.append("\"additionalInfo\":\"").append("a").append("\",");
        actionMetaDataBuilder.append("\"progressInfo\":\"").append("b").append("\",");
        actionMetaDataBuilder.append("\"resultInfo\":\"").append("c").append("\",");
        actionMetaDataBuilder.append("\"state\":\"").append(state).append("\",");
        actionMetaDataBuilder.append("\"progressPercentage\":").append("1.0").append(",");
        actionMetaDataBuilder.append("\"startTime\":\"").append("2019-08-12T15:26:13.450Z").append("\",");
        actionMetaDataBuilder.append("\"completionTime\":\"").append("2019-08-12T15:31:13.481Z").append("\",");
        actionMetaDataBuilder.append("\"lastUpdateTime\":\"").append("2019-08-12T15:30:13.482Z").append("\",");
        actionMetaDataBuilder.append("\"id\":\"").append(actionId).append("\"}");

        Files.write(backupFolder.resolve(actionId + ".json"), actionMetaDataBuilder.toString().getBytes());
    }
}
