/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.rules.TemporaryFolder;

public class TemporaryFolderProperties {
    public static final String BRO = "bro";
    public static final String BACKUPMANAGERS = "backupManagers";
    public static final String BACKUPS = "backups";
    public static final String BACKUP_NAME = "mybackup";
    public static final String BACKUP_ID = "DEFAULT";
    public static final String BACKUP_DATA_FOLDER_NAME = "backupdata";
    public static final String BACKUP_FILE_FOLDER_NAME = "backupfile";
    public static final String AGENT = "BROAgent";
    public static final String FRAGMENT = "1";
    public static final String DATA_FOLDER = "data";
    public static final String BACKUP_FILE_NAME = "mybackup.json";
    public static final String DATA_FILE = "data.json";
    public static final String FRAGMENT_FILE = "Fragment.json";
    public static final String GZIP_FILE = "mybackup.tar.gz";
    public static final String BACKUP_CONTENT = "{ \"backupId\": \"mybackup\", \"backupManagerId\": \"DEFAULT\", \"status\": \"COMPLETE\" }";
    public static final String DATA_CONTENT = "{ \"data\": { \"enableAlarm\": true, \"alarmExpTimer\": 30 } }";
    public static final String FRAGMENT_CONTENT = "{ \"fragmentId\": \"1\", \"version\": \"0.1\", \"sizeInBytes\": \"124\" }";
    public static final String SEP = File.separator;
    public static final String FRAGMENT_FILE_PATH = BACKUP_NAME + SEP + AGENT + SEP + FRAGMENT + SEP + FRAGMENT_FILE;
    public static final String DATA_FILE_PATH = BACKUP_NAME + SEP + AGENT + SEP + FRAGMENT + SEP + DATA_FOLDER + SEP + DATA_FILE;
    private static File backup;

    public static File createEmptyBackupFolder(TemporaryFolder folderForEmptyBackup) throws IOException {
        folderForEmptyBackup.newFolder(BRO);
        folderForEmptyBackup.newFolder(BRO, BACKUPS);
        folderForEmptyBackup.newFolder(BRO, BACKUPMANAGERS);
        folderForEmptyBackup.newFolder(BRO, BACKUPMANAGERS, BACKUP_ID);
        return folderForEmptyBackup.newFolder(BRO, BACKUPS, BACKUP_ID);
    }

    public static File createCompressedFile(final boolean isValid, TemporaryFolder folderForCompressedFile) throws IOException {
        File compressedFile = folderForCompressedFile.newFile(GZIP_FILE);
        final TarArchiveOutputStream tos = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(compressedFile)));
        tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        tos.putArchiveEntry(new TarArchiveEntry(BACKUP_ID + SEP));
        tos.putArchiveEntry(new TarArchiveEntry(BACKUP_ID + SEP + BACKUP_NAME + SEP));
        tos.putArchiveEntry(new TarArchiveEntry(BACKUP_ID + SEP + BACKUP_NAME + SEP + BACKUP_FILE_FOLDER_NAME + SEP));
        final String backupFile = BACKUP_ID + SEP + BACKUP_NAME + SEP + BACKUP_FILE_FOLDER_NAME + SEP + BACKUP_FILE_NAME;
        byte[] msg = BACKUP_CONTENT.getBytes();
        TarArchiveEntry entry = new TarArchiveEntry(new File(backupFile));
        entry.setSize(msg.length);
        tos.putArchiveEntry(entry);
        tos.write(msg, 0, msg.length);
        tos.closeArchiveEntry();
        if (isValid) {
            final String backupDataPath = BACKUP_ID + SEP + BACKUP_NAME + SEP + BACKUP_DATA_FOLDER_NAME + SEP;
            tos.putArchiveEntry(new TarArchiveEntry(backupDataPath));
            tos.putArchiveEntry(new TarArchiveEntry(backupDataPath + BACKUP_NAME + SEP));
            tos.putArchiveEntry(new TarArchiveEntry(backupDataPath + BACKUP_NAME + SEP + AGENT + SEP));
            tos.putArchiveEntry(new TarArchiveEntry(backupDataPath + BACKUP_NAME + SEP + AGENT + SEP + FRAGMENT + SEP));
            final String fragmentFile = BACKUP_ID + SEP + BACKUP_NAME + SEP + BACKUP_DATA_FOLDER_NAME + SEP + FRAGMENT_FILE_PATH;
            msg = FRAGMENT_CONTENT.getBytes();
            entry = new TarArchiveEntry(new File(fragmentFile));
            entry.setSize(msg.length);
            tos.putArchiveEntry(entry);
            tos.write(msg, 0, msg.length);
            tos.closeArchiveEntry();
            tos.putArchiveEntry(new TarArchiveEntry(backupDataPath + BACKUP_NAME + SEP + AGENT + SEP + FRAGMENT + SEP + DATA_FOLDER + SEP));
            final String dataFile = BACKUP_ID + SEP + BACKUP_NAME + SEP + BACKUP_DATA_FOLDER_NAME + SEP + DATA_FILE_PATH;
            msg = DATA_CONTENT.getBytes();
            entry = new TarArchiveEntry(new File(dataFile));
            entry.setSize(msg.length);
            tos.putArchiveEntry(entry);
            tos.write(msg, 0, msg.length);
            tos.closeArchiveEntry();
        }
        tos.close();
        return compressedFile;
    }

    public static File createUnCompressedFile(TemporaryFolder folderForUnCompressedFile) throws IOException {
        folderForUnCompressedFile.newFolder(BACKUP_ID);
        File uncompressedFolder = folderForUnCompressedFile.newFolder(BACKUP_ID, BACKUP_NAME);
        File file = folderForUnCompressedFile.newFolder(BACKUP_ID, BACKUP_NAME, BACKUP_FILE_FOLDER_NAME);
        final File backupFile = new File(file, BACKUP_FILE_NAME);
        byte[] msg = BACKUP_CONTENT.getBytes();
        FileOutputStream fos = new FileOutputStream(backupFile);
        fos.write(msg, 0, msg.length);
        fos.close();
        folderForUnCompressedFile.newFolder(BACKUP_ID, BACKUP_NAME, BACKUP_DATA_FOLDER_NAME);
        folderForUnCompressedFile.newFolder(BACKUP_ID, BACKUP_NAME, BACKUP_DATA_FOLDER_NAME, BACKUP_NAME);
        folderForUnCompressedFile.newFolder(BACKUP_ID, BACKUP_NAME, BACKUP_DATA_FOLDER_NAME, BACKUP_NAME, AGENT);
        file = folderForUnCompressedFile.newFolder(BACKUP_ID, BACKUP_NAME, BACKUP_DATA_FOLDER_NAME, BACKUP_NAME, AGENT, FRAGMENT);
        final File fragmentFile = new File(file, FRAGMENT_FILE);
        msg = FRAGMENT_CONTENT.getBytes();
        fos = new FileOutputStream(fragmentFile);
        fos.write(msg, 0, msg.length);
        fos.close();
        file = folderForUnCompressedFile.newFolder(BACKUP_ID, BACKUP_NAME, BACKUP_DATA_FOLDER_NAME, BACKUP_NAME, AGENT, FRAGMENT, DATA_FOLDER);
        final File dataFile = new File(file, DATA_FILE);
        msg = DATA_CONTENT.getBytes();
        fos = new FileOutputStream(dataFile);
        fos.write(msg, 0, msg.length);
        fos.close();
        return uncompressedFolder;
    }

    public static File createBackupFolderWithContent(TemporaryFolder folderForBackup) throws IOException {
        folderForBackup.newFolder(BRO);
        folderForBackup.newFolder(BRO, BACKUPMANAGERS);
        folderForBackup.newFolder(BRO, BACKUPMANAGERS, BACKUP_ID);
        File file = folderForBackup.newFolder(BRO, BACKUPMANAGERS, BACKUP_ID, BACKUPS);
        //global
        backup = new File(file, BACKUP_FILE_NAME);
        byte[] msg = BACKUP_CONTENT.getBytes();
        FileOutputStream fos = new FileOutputStream(backup);
        fos.write(msg, 0, msg.length);
        fos.close();
        folderForBackup.newFolder(BRO, BACKUPS);
        folderForBackup.newFolder(BRO, BACKUPS, BACKUP_ID);
        //global
        File backupDataFolder = folderForBackup.newFolder(BRO, BACKUPS, BACKUP_ID, BACKUP_NAME);
        folderForBackup.newFolder(BRO, BACKUPS, BACKUP_ID, BACKUP_NAME, AGENT);
        file = folderForBackup.newFolder(BRO, BACKUPS, BACKUP_ID, BACKUP_NAME, AGENT, FRAGMENT);
        final File fragmentFile = new File(file, FRAGMENT_FILE);
        msg = FRAGMENT_CONTENT.getBytes();
        fos = new FileOutputStream(fragmentFile);
        fos.write(msg, 0, msg.length);
        fos.close();
        file = folderForBackup.newFolder(BRO, BACKUPS, BACKUP_ID, BACKUP_NAME, AGENT, FRAGMENT, DATA_FOLDER);
        final File dataFile = new File(file, DATA_FILE);
        msg = DATA_CONTENT.getBytes();
        fos = new FileOutputStream(dataFile);
        fos.write(msg, 0, msg.length);
        fos.close();
        return backupDataFolder;
    }

    public static File getBackupFile() {
        return backup;
    }
}
