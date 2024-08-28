/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.archive.StreamingArchiveService;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFolder;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.HousekeepingFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.ScheduledEventHandler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerFileService;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidBackupFileException;
import com.ericsson.adp.mgmt.backupandrestore.exception.JobFailedException;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProvider;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResetConfigJobTest {

    @Test(expected = JobFailedException.class)
    public void givenNonResetVBRMTest() {
        final BackupManager otherVBRM = createMock(BackupManager.class);
        expect(otherVBRM.getBackupManagerId()).andReturn("DEFAULT-not-reset").once();
        expect(otherVBRM.getParent()).andAnswer(() -> Optional.of(getBRM(brm -> {})));
        replay(otherVBRM);
        final ResetConfigJob job = new ResetConfigJob();
        job.setBackupManager(otherVBRM);
        job.triggerJob();
    }

    @Test(expected = JobFailedException.class)
    public void givenNonVBRMTest() {
        final BackupManager someBRM = createMock(BackupManager.class);
        expect(someBRM.getBackupManagerId()).andReturn("some-top-level-brm").once();
        expect(someBRM.getParent()).andAnswer(Optional::empty);
        replay(someBRM);
        final ResetConfigJob job = new ResetConfigJob();
        job.setBackupManager(someBRM);
        job.triggerJob();
    }

    @Test
    public void standardRunThroughTest() {
        final Path base = Path.of("/bro/backupManagers/DEFAULT/").toAbsolutePath();
        final List<Path> configFiles = List.of(
                base.resolve("backupManager.json").toAbsolutePath(),
                base.resolve("scheduler.json").toAbsolutePath(),
                base.resolve("housekeeping.json").toAbsolutePath(),
                base.resolve("periodic-events/existing.json").toAbsolutePath(),
                base.resolve("periodic-events/new.json").toAbsolutePath()
        );
        final List<Path> resetFiles = new java.util.ArrayList<>(configFiles);
        resetFiles.remove(base.resolve("periodic-events/new.json"));
        final MockedPersistProvider provider = getProvider(configFiles, resetFiles);
        provider.setConfigTarballPath("DEFAULT", "test");
        final ResetConfigJob job = getJob(provider, base);
        job.triggerJob();
        assertTrue(job.didFinish());
        job.completeJob();
        assertEquals(provider.getWriteHistory(), getExpectedOutcome(configFiles, resetFiles,false, false, false));
    }

    @Test
    public void failDuringSetupCopy() {
        final Path base = Path.of("/bro/backupManagers/DEFAULT/").toAbsolutePath();
        final List<Path> configFiles = List.of(
                base.resolve("backupManager.json").toAbsolutePath(),
                base.resolve("scheduler.json").toAbsolutePath(),
                base.resolve("housekeeping.json").toAbsolutePath(),
                base.resolve("periodic-events/existing.json").toAbsolutePath(),
                base.resolve("periodic-events/new.json").toAbsolutePath()
        );
        final List<Path> resetFiles = new java.util.ArrayList<>(configFiles);
        resetFiles.remove(base.resolve("periodic-events/new.json"));
        final MockedPersistProvider provider = getProvider(configFiles, resetFiles);
        provider.setConfigTarballPath("DEFAULT", "test");
        final AtomicInteger countToFailure = new AtomicInteger(3); // Fail after the 3rd copy
        provider.setShouldFail((o, p) -> countToFailure.decrementAndGet() < 1 && p.toString().endsWith(ResetConfigJob.RESET_BACKUP_SUFFIX));
        final ResetConfigJob job = getJob(provider, base);
        try {
            job.triggerJob();
            throw new RuntimeException("Expected triggerJob to throw");
        } catch (JobFailedException e) {
            job.fail();
        }
        assertEquals(provider.getWriteHistory(), getExpectedOutcome(configFiles, resetFiles, true, false, false));
    }

    @Test
    public void failDuringResetExtract() {
        final Path base = Path.of("/bro/backupManagers/DEFAULT/").toAbsolutePath();
        final List<Path> configFiles = List.of(
                base.resolve("backupManager.json").toAbsolutePath(),
                base.resolve("scheduler.json").toAbsolutePath(),
                base.resolve("housekeeping.json").toAbsolutePath(),
                base.resolve("periodic-events/existing.json").toAbsolutePath(),
                base.resolve("periodic-events/new.json").toAbsolutePath()
        );
        final List<Path> resetFiles = new java.util.ArrayList<>(configFiles);
        resetFiles.remove(base.resolve("periodic-events/new.json"));
        final MockedPersistProvider provider = getProvider(configFiles, resetFiles);
        provider.setConfigTarballPath("DEFAULT", "test");
        provider.setShouldFail((o, p) -> p.toString().endsWith(ResetConfigJob.RESET_TEMP_SUFFIX));
        final ResetConfigJob job = getJob(provider, base);
        try {
            job.triggerJob();
            throw new RuntimeException("Expected triggerJob to throw");
        } catch (JobFailedException e) {
            job.fail();
        }
        assertEquals(provider.getWriteHistory(), getExpectedOutcome(configFiles, resetFiles, true, false, false));
    }

    @Test
    public void failDuringPreReplaceMarkerWrite() {
        final Path base = Path.of("/bro/backupManagers/DEFAULT/").toAbsolutePath();
        final List<Path> configFiles = List.of(
                base.resolve("backupManager.json").toAbsolutePath(),
                base.resolve("scheduler.json").toAbsolutePath(),
                base.resolve("housekeeping.json").toAbsolutePath(),
                base.resolve("periodic-events/existing.json").toAbsolutePath(),
                base.resolve("periodic-events/new.json").toAbsolutePath()
        );
        final List<Path> resetFiles = new java.util.ArrayList<>(configFiles);
        resetFiles.remove(base.resolve("periodic-events/new.json"));
        final MockedPersistProvider provider = getProvider(configFiles, resetFiles);
        provider.setConfigTarballPath("DEFAULT", "test");
        provider.setShouldFail((o, p) -> p.toString().endsWith(ResetConfigJob.RESET_TEMP_SUFFIX));
        final ResetConfigJob job = getJob(provider, base);
        try {
            job.triggerJob();
            throw new RuntimeException("Expected triggerJob to throw");
        } catch (JobFailedException e) {
            job.fail();
        }
        assertEquals(provider.getWriteHistory(), getExpectedOutcome(configFiles, resetFiles, true, false, false));
    }

    @Test
    public void failDuringReplace() {
        final Path base = Path.of("/bro/backupManagers/DEFAULT/").toAbsolutePath();
        final List<Path> configFiles = List.of(
                base.resolve("backupManager.json").toAbsolutePath(),
                base.resolve("scheduler.json").toAbsolutePath(),
                base.resolve("housekeeping.json").toAbsolutePath(),
                base.resolve("periodic-events/existing.json").toAbsolutePath(),
                base.resolve("periodic-events/new.json").toAbsolutePath()
        );
        final List<Path> resetFiles = new java.util.ArrayList<>(configFiles);
        resetFiles.remove(base.resolve("periodic-events/new.json"));
        final MockedPersistProvider provider = getProvider(configFiles, resetFiles);
        provider.setConfigTarballPath("DEFAULT", "test");
        provider.setShouldFail((o, p) -> o.equals("copy") && p.toString().endsWith(ResetConfigJob.RESET_TEMP_SUFFIX));
        final ResetConfigJob job = getJob(provider, base);
        try {
            job.triggerJob();
            throw new RuntimeException("Expected triggerJob to throw");
        } catch (JobFailedException e) {
            job.fail();
        }
        assertEquals(provider.getWriteHistory(), getExpectedOutcome(configFiles, resetFiles, true, true, false));
    }

    @Test
    public void failDuringPreDeleteMarkerWrite() {
        final Path base = Path.of("/bro/backupManagers/DEFAULT/").toAbsolutePath();
        final List<Path> configFiles = List.of(
                base.resolve("backupManager.json").toAbsolutePath(),
                base.resolve("scheduler.json").toAbsolutePath(),
                base.resolve("housekeeping.json").toAbsolutePath(),
                base.resolve("periodic-events/existing.json").toAbsolutePath(),
                base.resolve("periodic-events/new.json").toAbsolutePath()
        );
        final List<Path> resetFiles = new java.util.ArrayList<>(configFiles);
        resetFiles.remove(base.resolve("periodic-events/new.json"));
        final MockedPersistProvider provider = getProvider(configFiles, resetFiles);
        provider.setConfigTarballPath("DEFAULT", "test");
        provider.setShouldFail((o, p) -> o.equals("write") && p.toString().endsWith(ResetConfigJob.DELETE_PHASE_MARKER));
        final ResetConfigJob job = getJob(provider, base, brm -> {
            try {
                brm.reload(anyObject(BackupManagerFileService.class));
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
            expectLastCall().once();
        });
        try {
            job.triggerJob();
            throw new RuntimeException("Expected triggerJob to throw");
        } catch (JobFailedException e) {
            job.fail();
        }
        System.out.println(provider.getWriteHistory());
        System.out.println(getExpectedOutcome(configFiles, resetFiles, true, true, false));
        assertEquals(provider.getWriteHistory(), getExpectedOutcome(configFiles, resetFiles, true, true, false));
    }

    @Test
    public void failDuringTempCleanup() {
        final Path base = Path.of("/bro/backupManagers/DEFAULT/").toAbsolutePath();
        final List<Path> configFiles = List.of(
                base.resolve("backupManager.json").toAbsolutePath(),
                base.resolve("scheduler.json").toAbsolutePath(),
                base.resolve("housekeeping.json").toAbsolutePath(),
                base.resolve("periodic-events/existing.json").toAbsolutePath(),
                base.resolve("periodic-events/new.json").toAbsolutePath()
        );
        final List<Path> resetFiles = new java.util.ArrayList<>(configFiles);
        resetFiles.remove(base.resolve("periodic-events/new.json"));
        final MockedPersistProvider provider = getProvider(configFiles, resetFiles);
        provider.setConfigTarballPath("DEFAULT", "test");
        final AtomicBoolean hasFailed = new AtomicBoolean(false);
        provider.setShouldFail((o, p) -> {
            final boolean res = o.equals("delete") && p.toString().endsWith(ResetConfigJob.RESET_TEMP_SUFFIX);
            return res && hasFailed.compareAndSet(false, true);
        });
        final ResetConfigJob job = getJob(provider, base, brm -> {
            try {
                brm.reload(anyObject(BackupManagerFileService.class));
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
            expectLastCall().once();
        });
        try {
            job.triggerJob();
            throw new RuntimeException("Expected triggerJob to throw");
        } catch (JobFailedException e) {
            job.fail();
        }
        assertEquals(provider.getWriteHistory(), getExpectedOutcome(configFiles, resetFiles, true, true, false));
    }

    @Test
    public void failDuringCloneCleanup() {
        final Path base = Path.of("/bro/backupManagers/DEFAULT/").toAbsolutePath();
        final List<Path> configFiles = List.of(
                base.resolve("backupManager.json").toAbsolutePath(),
                base.resolve("scheduler.json").toAbsolutePath(),
                base.resolve("housekeeping.json").toAbsolutePath(),
                base.resolve("periodic-events/existing.json").toAbsolutePath(),
                base.resolve("periodic-events/new.json").toAbsolutePath()
        );
        final List<Path> resetFiles = new java.util.ArrayList<>(configFiles);
        resetFiles.remove(base.resolve("periodic-events/new.json"));
        final MockedPersistProvider provider = getProvider(configFiles, resetFiles);
        provider.setConfigTarballPath("DEFAULT", "test");
        final AtomicBoolean hasFailed = new AtomicBoolean(false);
        provider.setShouldFail((o, p) -> {
            final boolean res = o.equals("delete") && p.toString().endsWith(ResetConfigJob.RESET_BACKUP_SUFFIX);
            return res && hasFailed.compareAndSet(false, true);
        });
        final ResetConfigJob job = getJob(provider, base, brm -> {
            try {
                brm.reload(anyObject(BackupManagerFileService.class));
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
            expectLastCall().once();
        });
        try {
            job.triggerJob();
            throw new RuntimeException("Expected triggerJob to throw");
        } catch (JobFailedException e) {
            job.fail();
        }
        System.out.println(provider.getWriteHistory());
        System.out.println(getExpectedOutcome(configFiles, resetFiles, true, true, true));
        assertEquals(provider.getWriteHistory(), getExpectedOutcome(configFiles, resetFiles, true, true, true));
    }

    private ResetConfigJob getJob(final PersistProvider provider, final Path base) {
        return getJob(provider, base, b -> {});
    }

    private ResetConfigJob getJob(final PersistProvider provider, final Path base, final Consumer<BackupManager> brmInit) {
        final ResetConfigJob job = new ResetConfigJob();
        job.setHandler(getHandler(handler -> {}));
        job.setHousekeepingFileService(getHFS(service -> {}));
        job.setSchedulerFileService(getSFS(service -> {}));
        job.setProvider(provider);
        final BackupManagerFileService brmFS = getBRMFS(service -> {
            expect(service.getBackupManagerFolder("DEFAULT")).andReturn(base).anyTimes();
        });
        job.setBrmFileService(brmFS);
        job.setBackupLocationService(getBLS(service -> {
            expect(service.getBackupManagerLocation(anyString())).andReturn(base).anyTimes();
            final BackupFolder folder = new BackupFolder(Path.of("/bro/backups/DEFAULT/test").toAbsolutePath());
            expect(service.getBackupFolder("DEFAULT", "test")).andReturn(folder).anyTimes();
        }));
        job.setBackupManager(getVBRM("DEFAULT", brm -> {
            brmInit.accept(brm);
            expect(brm.getBackup("test", Ownership.OWNED)).andReturn(getBackup(backup -> {
                expect(backup.getBackupId()).andReturn("test").anyTimes();
                expect(backup.getName()).andReturn("test").anyTimes();
            })).once();
            try {
                brm.reload(brmFS);
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
            expectLastCall().once();
            expect(brm.getScheduler()).andReturn(mock(Scheduler.class)).anyTimes();
            expect(brm.getHousekeeping()).andReturn(mock(Housekeeping.class)).anyTimes();
        }));
        job.setAction(getAction(action -> {
            expect(action.getBackupName()).andReturn("test").once();
        }));

        return job;
    }

    private Map<Path, Optional<Path>> getExpectedOutcome(final List<Path> starting, final List<Path> resetContents,
                                                         final boolean failAfterOperationStarted,
                                                         final boolean failAfterReplaceMarkerWritten,
                                                         final boolean failAfterDeleteMarkerWritten) {
        final Map<Path, Optional<Path>> outcome = new HashMap<>();
        for (final Path p: resetContents) {
            outcome.put(p, Optional.of(p.getParent().resolve(p.getFileName() + ResetConfigJob.RESET_TEMP_SUFFIX)));
        }
        if (failAfterOperationStarted) {
            outcome.clear();
            starting.forEach(p -> outcome.put(p, Optional.empty()));
        }
        if (failAfterReplaceMarkerWritten) {
            // If we fail at any point during the replacement phase, the filesystem should look as though the original config
            // files were copied out to their backup, and then back in
            outcome.clear();
            for (final Path p: starting) {
                outcome.put(p, Optional.of(p.getParent().resolve(p.getFileName() + ResetConfigJob.RESET_BACKUP_SUFFIX)));
            }
        }
        if (failAfterDeleteMarkerWritten) {
            outcome.clear();
            // If we fail during the deletion phase, the filesystem should look like the reset has occurred
            for (final Path p: resetContents) {
                outcome.put(p, Optional.of(p.getParent().resolve(p.getFileName() + ResetConfigJob.RESET_TEMP_SUFFIX)));
            }
        }
        // In the event of either no failures or failure before the replacement phase, the original config files should
        // be untouched
        return outcome;
    }

    private ScheduledEventHandler getHandler(final Consumer<ScheduledEventHandler> init) {
        final ScheduledEventHandler handler = mock(ScheduledEventHandler.class);
        init.accept(handler);
        replay(handler);
        return handler;
    }

    private HousekeepingFileService getHFS(final Consumer<HousekeepingFileService> init) {
        final HousekeepingFileService service = mock(HousekeepingFileService.class);
        init.accept(service);
        replay(service);
        return service;
    }

    private SchedulerFileService getSFS(final Consumer<SchedulerFileService> init) {
        final SchedulerFileService service = mock(SchedulerFileService.class);
        init.accept(service);
        replay(service);
        return service;
    }

    private BackupManagerFileService getBRMFS(final Consumer<BackupManagerFileService> init) {
        final BackupManagerFileService service = mock(BackupManagerFileService.class);
        init.accept(service);
        replay(service);
        return service;
    }

    private BackupLocationService getBLS(final Consumer<BackupLocationService> init) {
        final BackupLocationService service = mock(BackupLocationService.class);
        expect(service.getBackupLocation()).andReturn(Path.of("/bro").toAbsolutePath()).anyTimes();
        init.accept(service);
        replay(service);
        return service;
    }

    private BackupManager getVBRM(final String parentName, final Consumer<BackupManager> parentInit) {
        final String vbrmName = parentName + "-bro";
        final Consumer<BackupManager> wrapped = brm -> {
            expect(brm.getBackupManagerId()).andReturn(parentName).anyTimes();
            parentInit.accept(brm);
        };
        final BackupManager vBRM = mock(BackupManager.class);
        expect(vBRM.getParent()).andReturn(Optional.of(getBRM(wrapped))).times(2);
        expect(vBRM.getBackupManagerId()).andReturn(vbrmName).once();
        replay(vBRM);
        return vBRM;
    }

    private BackupManager getBRM(final Consumer<BackupManager> init) {
        final BackupManager brm = mock(BackupManager.class);
        init.accept(brm);
        replay(brm);
        return brm;
    }

    private Action getAction(final Consumer<Action> init) {
        final Action action = mock(Action.class);
        init.accept(action);
        replay(action);
        return action;
    }

    private Backup getBackup(final Consumer<Backup> init) {
        final Backup backup = mock(Backup.class);
        init.accept(backup);
        replay(backup);
        return backup;
    }


    private MockedPersistProvider getProvider(final List<Path> configFiles, final List<Path> resetFiles) {
        final Function<Path, OutputStream> osFactory = p -> OutputStream.nullOutputStream();
        final Function<Path, InputStream> isFactory = p -> {
            if (!p.toString().endsWith(".tar.gz")) {
                throw new RuntimeException("This failed because the job should only try to read the tarball as a stream");
            }
            return new ByteArrayInputStream(getMockedTarballContents(resetFiles));
        };
        final MockedPersistProvider provider = new MockedPersistProvider(isFactory, osFactory);

        final PersistProviderFactory providerFactory = mock(PersistProviderFactory.class);
        expect(providerFactory.getPersistProvider()).andReturn(provider).anyTimes();
        replay(providerFactory);
        configFiles.forEach(p -> provider.write(p.getParent(), p, p.toString().getBytes()));
        return provider;
    }


    private byte[] getMockedTarballContents(final List<Path> toAdd) {
        final Path basePath = Path.of("/bro").toAbsolutePath();
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final TarArchiveOutputStream tos = new TarArchiveOutputStream(os);
        tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        for (final Path p: toAdd) {
            if (!p.startsWith(basePath)) {
                throw new InvalidBackupFileException("This failed because every path in the config backup should start with \"/bro\"");
            }
            StreamingArchiveService.passDataToTarOutputStreamLocation(
                    basePath.relativize(p).getParent(),// We relativize and getParent here because that's basically what the BackupManagerFileService does
                    tos,
                    p.getFileName().toString(),
                    p.toString()); // Just use the files name as it's contents, they're irrelevant for this test suite
        }
        return os.toByteArray();
    }

    private static class MockedPersistProvider extends PersistProvider {
        private final Map<Path, Optional<Path>> writeHistory = new HashMap<>();
        private final Function<Path, InputStream> inputStreamFactory;
        private final Function<Path, OutputStream> outputStreamFactory;
        private Path configTarballPath;
        private BiFunction<String, Path, Boolean> shouldFail = (o, p) -> false;

        private MockedPersistProvider(Function<Path, InputStream> inputStreamFactory, Function<Path, OutputStream> outputStreamFactory) {
            this.inputStreamFactory = inputStreamFactory;
            this.outputStreamFactory = outputStreamFactory;
        }

        @Override
        public void write(Path folder, Path file, byte[] content) {
            failGuard("write", file);
            writeHistory.put(file, Optional.empty());
        }

        @Override
        public Stream<Path> walk(final Path path, final int maxDepth) throws IOException {
            failGuard("walk", path);
            return new HashSet<>(writeHistory.keySet()).stream().filter(p -> p.startsWith(path));
        }

        @Override
        public String read(Path path) {
            failGuard("read", path);
            throw new NotImplementedException();
        }

        @Override
        public void delete(Path path) throws IOException {
            failGuard("delete", path);
            if (!writeHistory.containsKey(path)) {
                throw new NotFoundException("Couldn't find file " + path);
            }
            writeHistory.remove(path);
        }

        @Override
        public boolean exists(Path path) {
            failGuard("exists", path);
            return writeHistory.containsKey(path) || path.equals(configTarballPath);
        }

        @Override
        public List<Path> list(Path dir) {
            failGuard("list", dir);
            return writeHistory.keySet().stream().filter(p -> p.startsWith(dir)).collect(Collectors.toList());
        }

        @Override
        public boolean isDir(Path path) {
            failGuard("isDir", path);
            return !list(path).isEmpty() && !(list(path).size() == 1 && list(path).get(0).equals(path));
        }

        @Override
        public boolean isFile(Path path) {
            failGuard("isFile", path);
            return !isDir(path);
        }

        @Override
        public long length(Path path) throws IOException {
            failGuard("length", path);
            return path.toString().length(); // May as well put some variance in here
        }

        @Override
        public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
            failGuard("newInputStream", path);
            return inputStreamFactory.apply(path);
        }

        @Override
        public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
            failGuard("newOutputStream", path);
            write(path.getParent(), path, new byte[0]);
            return outputStreamFactory.apply(path);
        }

        @Override
        public boolean mkdirs(Path dir) {
            failGuard("mkdirs", dir);
            return false;
        }

        @Override
        public boolean mkdir(Path dir) {
            failGuard("mkdir", dir);
            return false;
        }

        @Override
        public void deleteDummyFile() throws IOException {
            // No-op on purpose
        }

        @Override
        public void createDummyFile(int size) {
            // No-op on purpose
        }

        @Override
        public void setReservedSpace(Path path) {
            // No-op on purpose
        }

        @Override
        public boolean copy(Path src, Path dst, boolean replace) throws IOException {
            failGuard("copy", src);
            failGuard("copy", dst);
            if (!writeHistory.containsKey(src)) {
                throw new FileNotFoundException();
            }
            final boolean res = writeHistory.containsKey(dst);
            if (res && !replace) {
                throw new IOException("Copy would overwrite, dst exists");
            }
            writeHistory.put(dst, Optional.of(src));
            return res;
        }

        public Map<Path, Optional<Path>> getWriteHistory() {
            return writeHistory;
        }

        public void setConfigTarballPath(final String managerId, final String backupId) {
            this.configTarballPath = Path.of("/bro/backups/" + managerId + "/" + backupId + "/backupManagers.tar.gz").toAbsolutePath();
        }

        public void setShouldFail(final BiFunction<String, Path, Boolean> shouldFail) {
            this.shouldFail = shouldFail;
        }

        private void failGuard(final String operation, final Path toTest) {
            if (shouldFail.apply(operation, toTest)) {
                throw new RuntimeException(new IOException("Injected fault"));
            }
        }

        @Override
        public Stream<Path> walk(Path path, int maxDepth, boolean ordered) throws IOException {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
