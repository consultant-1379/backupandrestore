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
package com.ericsson.adp.mgmt.backupandrestore.util;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.CONFIGURATION_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SCHEMA_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.TO_TOP_POST_PUT_DELETE_PATCH;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.ADD_PARAMETER;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_MANAGER_POSITION_IN_CONTEXT;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_POSITION_IN_CONTEXT;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import akka.http.scaladsl.model.headers.Expect;
import org.awaitility.Awaitility;
import org.easymock.EasyMock;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.caching.Cached;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMMessage;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMMessageFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ConfigurationPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;

public class ManagedQueueingWorkerTest extends SystemTest {

    private static final String TEST_HOST = "http://localhost:5003";
    private static final String TEST_URL = TEST_HOST + "/cm/api/v1";
    private static final String RESOURCE = TEST_URL + "/" + CONFIGURATION_RESOURCE + "/" + SCHEMA_NAME;
    private static final String REST_TEMPLATE_ID = "RestTemplateConfigurationTest";
    private enum Response_expected {RESPONSE_SUCCESS, RESPONSE_4XX, RESPONSE_5XX, RESPONSE_500}

    private MockRestServiceServer mockServer;

    @Autowired
    private CMMMessageFactory cmmMessageFactory;

    Cached<RestTemplate> restTemplate;

    private ManagedQueueingWorker<CMMMessage> blockingQueueService;

    @Before
    public void init() {
        final RestTemplateFactory configuration = new RestTemplateFactory();
        restTemplate = configuration.getRestTemplate(REST_TEMPLATE_ID, RestTemplateFactory.Security.NONE);
        mockServer = MockRestServiceServer.bindTo(restTemplate.get()).build();
        mockServer.verify();
    }   

    @After
    public void finish() {
        blockingQueueService.stopProcessing();
    }

    @Test
    public void testAddMessage_singleMessagetoQueue_SUCCESS_RESPONSE() throws URISyntaxException, JSONException, InterruptedException {
        blockingQueueService = new ManagedQueueingWorker(new Processor(Response_expected.RESPONSE_SUCCESS, RESOURCE));
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isProcessRunning());
        final boolean result = blockingQueueService.add(getCMMMessage());
        Assert.assertTrue(result);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isEmpty());
        Assert.assertTrue(blockingQueueService.isEmpty());
    }

    @Test
    public void testAddMessage_severalMessagestoQueue_SUCCESS_RESPONSE() throws URISyntaxException, JSONException, InterruptedException {
        final Processor processor = new Processor(Response_expected.RESPONSE_SUCCESS, RESOURCE);
        blockingQueueService = new ManagedQueueingWorker(processor, 3, 1);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isProcessRunning());

        boolean result = blockingQueueService.add(getCMMMessage());
        result=result & blockingQueueService.add(getCMMMessage());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isEmpty());
        Assert.assertTrue(blockingQueueService.isEmpty());
        Assert.assertEquals(processor.getProcessed(), 2);
        Assert.assertEquals(processor.getServerRetries(),0);
        Assert.assertEquals(processor.getClientRetries(),0);
        Assert.assertTrue(result);
    }

    @Test
    public void testAddMessage_singleMessagetoQueue_RESPONSE_5XX() throws URISyntaxException, JSONException, InterruptedException {
        // On Error the queue is empty
        final Processor processor = new Processor(Response_expected.RESPONSE_5XX, RESOURCE);
        blockingQueueService = new ManagedQueueingWorker(processor, 1);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isProcessRunning());

        final boolean result = blockingQueueService.add(getCMMMessage());

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isEmpty());

        Assert.assertTrue(result);
        Assert.assertTrue(blockingQueueService.isEmpty());
        Assert.assertTrue(processor.getProcessed() > 0);
        Assert.assertTrue(processor.getServerRetries() > 0);
        Assert.assertEquals(processor.getClientRetries(),0);
    }

    @Test
    public void testAddMessage_severalMessagetoQueue_MovePOSTtoTOP() throws URISyntaxException, JSONException, InterruptedException {
        // On Error the queue is empty
        final EmptyProcessor processor = new EmptyProcessor();
        blockingQueueService = new ManagedQueueingWorker(processor, 5);

        boolean result = blockingQueueService.add(getCMMMessage());
        result=result & blockingQueueService.add(getCMMMessage());
        result=result & blockingQueueService.add(getCMMMessageCreateConfiguration());
        result=result & blockingQueueService.add(getCMMMessage());
        result=result & blockingQueueService.add(getCMMMessage());
        blockingQueueService.ToTop(TO_TOP_POST_PUT_DELETE_PATCH);

        Assert.assertTrue(result);
        Assert.assertTrue(((CMMMessage) blockingQueueService.getTop()).getHttpMethod()==HttpMethod.PATCH);
        Assert.assertTrue(((CMMMessage) blockingQueueService.getTop()).getConfigurationPatch().getPath().endsWith(ADD_PARAMETER));
    }

    @Test
    public void testAddSeveralMessages_MovetoTop_PUT_usedInCreateConfiguration() throws URISyntaxException, JSONException, InterruptedException {
        // On Error the queue is empty
        final EmptyProcessor processor = new EmptyProcessor();
        blockingQueueService = new ManagedQueueingWorker(processor, 6);

        boolean result = blockingQueueService.add(getPatchCMMMessage());
        result=result & blockingQueueService.add(getPatchCMMMessage());
        result=result & blockingQueueService.add(getPatchCMMMessage());
        result=result & blockingQueueService.add(getPatchCMMMessage());
        // getCMMMessageCreateConfiguration uses a PUT Command to create the configuration
        result=result & blockingQueueService.add(getCMMMessageCreateConfiguration());
        result=result & blockingQueueService.add(getPatchToAddBackupManager());
        blockingQueueService.stopProcessing();
        blockingQueueService.ToTop(TO_TOP_POST_PUT_DELETE_PATCH);
        Assert.assertTrue(((CMMMessage) blockingQueueService.getTop()).getHttpMethod()==HttpMethod.PUT);
    }

    @Test
    public void testAddMessage_severalMessageMovetoTopPATCH_ADD() throws URISyntaxException, JSONException, InterruptedException {
        // On Error the queue is empty
        final EmptyProcessor processor = new EmptyProcessor();
        blockingQueueService = new ManagedQueueingWorker(processor, 6);

        boolean result = blockingQueueService.add(getPatchCMMMessage());
        result=result & blockingQueueService.add(getPatchCMMMessage());
        result=result & blockingQueueService.add(getPatchCMMMessage());
        result=result & blockingQueueService.add(getPatchCMMMessage());
        result=result & blockingQueueService.add(getPatchToAddBackupManager());
        result=result & blockingQueueService.add(getCMMMessageCreateConfiguration());
        blockingQueueService.stopProcessing();
        blockingQueueService.ToTop(TO_TOP_POST_PUT_DELETE_PATCH);

        Assert.assertTrue(result);
        Assert.assertTrue(((CMMMessage) blockingQueueService.getTop()).getHttpMethod()==HttpMethod.PATCH);
        Assert.assertTrue(((CMMMessage) blockingQueueService.getTop()).getConfigurationPatch().getOperation().equals(PatchOperation.ADD));
    }

    @Test
    public void testAddMessage_singleMessagetoQueue_RESPONSE_4XX() throws URISyntaxException, JSONException, InterruptedException {
        // On service OPEN the queue is being clear until it's restored
        final Processor processor = new Processor(Response_expected.RESPONSE_4XX, RESOURCE);
        blockingQueueService = new ManagedQueueingWorker(processor);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isProcessRunning());

        final boolean result = blockingQueueService.add(getCMMMessage());

        Assert.assertTrue(result);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isEmpty());
        Assert.assertTrue(blockingQueueService.isEmpty());
        Assert.assertEquals(processor.getProcessed(),1);
        Assert.assertEquals(processor.getServerRetries(),0);
        Assert.assertEquals(processor.getClientRetries(),1);
    }

    @Test
    public void testInterruptWorker_singleMessagetoQueue() throws URISyntaxException, JSONException, InterruptedException {
        final Processor processor = new Processor(Response_expected.RESPONSE_5XX, RESOURCE);
        blockingQueueService = new ManagedQueueingWorker(processor, 1);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isProcessRunning());

        blockingQueueService.getWorker().interrupt();
        Assert.assertTrue(blockingQueueService.isProcessRunning());

        final boolean result = blockingQueueService.add(getCMMMessage());
        Assert.assertTrue(result);

        blockingQueueService.stopProcessing();
        Assert.assertFalse(blockingQueueService.isProcessRunning());
    }

    @Test
    public void testAddMessage_QueueMessagesifServiceisDown_NumberOfMessagesRemoved() throws InterruptedException {
        final Processor processor = new Processor(Response_expected.RESPONSE_500, RESOURCE);
        blockingQueueService = new ManagedQueueingWorker(processor, 3, 1);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isProcessRunning());

        boolean result = blockingQueueService.add(getCMMMessage());
        result=result & blockingQueueService.add(getCMMMessage());
        result=result & blockingQueueService.add(getCMMMessage());
        result=result & blockingQueueService.add(getCMMMessage());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isEmpty());

        Assert.assertTrue(blockingQueueService.isEmpty());
    }

    @Test
    public void testAddMessage_EngineProcessor_AddAndWait() throws InterruptedException {
        final Processor processor = new Processor(Response_expected.RESPONSE_5XX, RESOURCE);
        blockingQueueService = new ManagedQueueingWorker(processor, 3, 1);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isProcessRunning());
        blockingQueueService.addAndWait(getCMMMessage());
        blockingQueueService.stopProcessing();
    }

    @Test
    public void testAddMessage_EngineProcessor_restart() throws InterruptedException {
        final Processor processor = new Processor(Response_expected.RESPONSE_SUCCESS, RESOURCE);
        blockingQueueService = new ManagedQueueingWorker(processor, 3, 1);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isProcessRunning());

        boolean result = blockingQueueService.add(getCMMMessage());
        blockingQueueService.stopProcessing();
        result=result & blockingQueueService.add(getCMMMessage());
        blockingQueueService.stopProcessing();
        result=result & blockingQueueService.add(getCMMMessage());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> blockingQueueService.isEmpty());

        Assert.assertTrue(blockingQueueService.isEmpty());
    }

    protected CMMMessage getCMMMessageCreateConfiguration() {
        return cmmMessageFactory.getMessageToCreateConfiguration(1);
    }

    protected CMMMessage getCMMMessage() {
        final Backup backup = createMock(Backup.class);
        final BackupManager manager = createMock(BackupManager.class);
        expect(manager.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        expect(backup.getName()).andReturn("myBackup").anyTimes();
        expect(backup.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        expect(backup.getBackupId()).andReturn("backup1").anyTimes();
        expect(backup.getCreationTime()).andReturn(OffsetDateTime.now()).anyTimes();
        expect(backup.getCreationType()).andReturn(BackupCreationType.MANUAL).anyTimes();
        expect(backup.getStatus()).andReturn(BackupStatus.COMPLETE).anyTimes();
        expect(backup.getUserLabel()).andReturn("label").anyTimes();
        expect(backup.getSoftwareVersions()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.replay(backup);
        EasyMock.replay(manager);
        final int RETRY_INDEFINITELY = -1;
        final CMMMessage message = cmmMessageFactory.getMessageToAddBackup(manager, backup, RETRY_INDEFINITELY);
        message.setRetry(3);
        return message;
    }

    protected CMMMessage getPatchCMMMessage() {
        final Backup backup = createMock(Backup.class);
        final BackupManager manager = createMock(BackupManager.class);
        expect(manager.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        expect(backup.getName()).andReturn("myBackup").anyTimes();
        expect(backup.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        expect(backup.getBackupId()).andReturn("backup1").anyTimes();
        expect(backup.getCreationTime()).andReturn(OffsetDateTime.now()).anyTimes();
        expect(backup.getCreationType()).andReturn(BackupCreationType.MANUAL).anyTimes();
        expect(backup.getStatus()).andReturn(BackupStatus.COMPLETE).anyTimes();
        expect(backup.getUserLabel()).andReturn("label").anyTimes();
        expect(backup.getSoftwareVersions()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.replay(backup);
        EasyMock.replay(manager);
        final int RETRY_INDEFINITELY = -1;
        final CMMMessage message = cmmMessageFactory.getMessageToUpdateBackup(manager, backup, (a, b) -> null, 0);
        message.setRetry(3);
        return message;
    }

    protected CMMMessage getPatchToAddBackupManager() {
        final Backup backup = createMock(Backup.class);
        final BackupManager manager = createMock(BackupManager.class);
        expect(manager.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        expect(backup.getName()).andReturn("myBackup").anyTimes();
        expect(backup.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        expect(backup.getBackupId()).andReturn("backup1").anyTimes();
        expect(backup.getCreationTime()).andReturn(OffsetDateTime.now()).anyTimes();
        expect(backup.getCreationType()).andReturn(BackupCreationType.MANUAL).anyTimes();
        expect(backup.getStatus()).andReturn(BackupStatus.COMPLETE).anyTimes();
        expect(backup.getUserLabel()).andReturn("label").anyTimes();
        expect(backup.getSoftwareVersions()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.replay(backup);
        EasyMock.replay(manager);
        final CMMMessage message = cmmMessageFactory.getPatchToAddBackupManager(manager, 1);
        message.setRetry(3);
        return message;
    }

    class DelegateResponseCreator implements ResponseCreator {
        private final ResponseCreator[] delegates;
        private int toExecute = 0;

        public DelegateResponseCreator(final ResponseCreator... delegates) {
            this.delegates = delegates;
        }

        @Override
        public ClientHttpResponse createResponse(final ClientHttpRequest request) throws IOException {
            final ClientHttpResponse ret = delegates[toExecute % delegates.length].createResponse(request);
            toExecute++;

            return ret;
        }
    }

    class EmptyProcessor implements ProcessorEngine<CMMMessage> {
        @Override
        public CMMMessage transferMessage(CMMMessage message) {
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }
    }

    class Processor implements ProcessorEngine<CMMMessage> {
        final String responseBody = "{\"response\" : \"true\"}";
        final Response_expected response_expected;
        private final AtomicLong processed = new AtomicLong();
        private final AtomicLong clientRetries = new AtomicLong();
        private final AtomicLong serverRetries = new AtomicLong();

        public Processor(final Response_expected response_expected, final String resource) {
            super();
            this.response_expected = response_expected;
            declareRestServerExpecting(resource);
        }

        public long getProcessed() {
            return processed.get();
        }

        public long getClientRetries() {
            return clientRetries.get();
        }

        public long getServerRetries() {
            return serverRetries.get();
        }

        @Override
        public CMMMessage transferMessage(final CMMMessage cmmMessage) {
            processed.incrementAndGet();
            try {
                executeRestActions (cmmMessage);
                return null;
            } catch (final HttpServerErrorException ioException) {
                serverRetries.incrementAndGet();
                if (cmmMessage.getRetryAndDecrement() > 0) {
                    return transferMessage(cmmMessage);
                } else {
                    return remedy(cmmMessage, ioException);
                }
            } catch (final Exception exception) {
                clientRetries.incrementAndGet();
                return remedy(cmmMessage, exception);
            }
        }

        private void executeRestActions(final CMMMessage cmmMessage) throws ResourceAccessException, URISyntaxException {
            final HttpMethod httpMethod = cmmMessage.getHttpMethod();

            if (HttpMethod.PATCH.equals(httpMethod)) {
                patch(cmmMessage.getResource(), cmmMessage.getConfigurationPatch());
            } else if (HttpMethod.POST.equals(httpMethod)) {
                post(cmmMessage.getResource(), cmmMessage.getHttpEntity());
            } else if (HttpMethod.PUT.equals(httpMethod)) {
                put(cmmMessage.getResource(), cmmMessage.getHttpEntity());
            } else if (HttpMethod.DELETE.equals(httpMethod)) {
                delete(cmmMessage.getResource());
            } else if (HttpMethod.GET.equals(httpMethod)) {
                get(cmmMessage.getResource());
            }
        }

        private CMMMessage remedy(final CMMMessage cmmMessage, final Exception exception) {
            final CMMMessage next = cmmMessage.getFallback().apply(cmmMessage, exception).orElse(null);
            return next;
        }

        private void declareRestServerExpecting(final String resource) {
            try {
                switch (response_expected) {
                    case RESPONSE_4XX:
                        mockServer.expect(ExpectedCount.times(5),
                                requestTo(new URI(resource)))
                        .andExpect(method(PATCH))
                        .andRespond(new DelegateResponseCreator(
                                withStatus(UNAUTHORIZED),
                                withStatus(UNAUTHORIZED),
                                withStatus(UNAUTHORIZED),
                                withStatus(UNAUTHORIZED),
                                withSuccess()
                                ));
                        break;
                    case RESPONSE_5XX:
                        mockServer.expect(ExpectedCount.times(5),
                                requestTo(new URI(resource)))
                        .andExpect(method(PATCH))
                        .andRespond(new DelegateResponseCreator(
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withSuccess()
                                ));
                        break;
                    case RESPONSE_500:
                        mockServer.expect(ExpectedCount.times(20),
                                requestTo(new URI(resource)))
                        .andExpect(method(PATCH))
                        .andRespond(new DelegateResponseCreator(
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError(),
                                withServerError()
                                ));
                        break;
                    case RESPONSE_SUCCESS:
                    default:
                        mockServer.expect(ExpectedCount.manyTimes(),
                                requestTo(new URI(resource)))
                        .andExpect(method(PATCH))
                        .andRespond(withSuccess(responseBody, APPLICATION_JSON));
                        break;
                }
            } catch (final URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        private void post(final String resource, final HttpEntity<?> requestEntity) {}

        public void put(final String resource, final HttpEntity<?> requestEntity) {}

        public void patch(final String resource, final ConfigurationPatch patch) throws ResourceAccessException, URISyntaxException {
            restTemplate.get().patchForObject(RESOURCE, patch.toJson(), String.class);
        }

        public void delete(final String resource) {}

        public String get(final String resource) {
            final ResponseEntity<String> result = restTemplate.get().getForEntity(RESOURCE, String.class);
            return result.getBody();
        }

    }

}
