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
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType.MANUAL;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState.LOCKED;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMClient.RETRY_INDEFINITELY;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SEARCH_STRING_SYSTEM_ADMIN;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SEARCH_STRING_SYSTEM_READ_ONLY;
import static com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled.DISABLED;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.IAnswer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.caching.Cached;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMBackupManagerJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMEricssonbrmJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.SchemaRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddBackupManagerPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddBackupManagerPath;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddBackupPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddHousekeepingPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddInitialBackupPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddPeriodicEventPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddProgressReportPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddSchedulerPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.BackupManagerPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.BackupPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.DeleteBackupPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.DeletePeriodicEventPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.HousekeepingPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PeriodicEventPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ProgressReportPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.SchedulerPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.UpdateBackupManagerPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.UpdateBackupPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.UpdateHousekeepingPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.UpdatePeriodicEventPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.UpdateProgressReportPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.UpdateSchedulerPatch;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CMMediatorServiceTest {

    protected CMMediatorService service;
    private BackupManagerPatchFactory backupManagerPatchFactory;
    private BackupPatchFactory backupPatchFactory;
    private ProgressReportPatchFactory progressReportPatchFactory;
    private HousekeepingPatchFactory housekeepingPatchFactory;
    private SchedulerPatchFactory schedulerPatchFactory;
    private PeriodicEventPatchFactory periodicEventPatchFactory;
    protected Cached<RestTemplate> restTemplate;
    private RestTemplateFactory restTemplateFactory;
    private CMMMessageFactory cmmMessageFactory;
    private CMMClient cmmClient;
    private CMMRestClient cmmRestClient;
    private SchemaRequestFactory schemaRequestFactory;
    protected BackupManagerRepository backupManagerRepository;
    private CMMSubscriptionRequestFactory cmmSubscriptionRequestFactory;
    private JsonService jsonService;
    private Optional<BRMEricssonbrmJson> brmEricssonJson;
    private KeyStoreService keyStoreService;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private EtagNotifIdBase etagNotifIdBase;
    private HttpHeaders headers = new HttpHeaders();
    private ResponseEntity<String> responseMock;


    private static final Logger log = LogManager.getLogger(CMMediatorServiceTest.class);

    private static final String CONFIGURATION_NAME = "ericsson-brm";
    private static final String CONFIGURATION_RESOURCE = "configurations";
    private static final String SUBSCRIPTION_RESOURCE = "subscriptions";
    private static final String NACM_RESOURCE = "ietf-netconf-acm";
    private static final String SCHEMA_RESOURCE = "schemas";
    private static final String TEST_URL = "http://localhost:5003/cm/api/v1";
    private static final String TEST_URL_CONFIGURATION = TEST_URL + "/" + CONFIGURATION_RESOURCE;
    private static final String TEST_URL_CONFIGURATION_BASE = TEST_URL_CONFIGURATION + "/" + CONFIGURATION_NAME;
    protected static final String SCHEMA_URL = TEST_URL + "/" + SCHEMA_RESOURCE + "/" + CONFIGURATION_NAME;
    private static final String SCHEMAS_URL = TEST_URL + "/" + SCHEMA_RESOURCE;
    private static final String TEST_URL_GET_DELETE = CONFIGURATION_RESOURCE + "/" + CONFIGURATION_NAME;
    private static final String NACM_URL = TEST_URL + "/" + CONFIGURATION_RESOURCE + "/" + NACM_RESOURCE;
    protected static final String SUBSCRIPTIONS_URL = TEST_URL + "/" + SUBSCRIPTION_RESOURCE + "/" + CONFIGURATION_NAME;
    private static final String SUBSCRIPTIONS_RESOURCE_URL = TEST_URL + "/" + SUBSCRIPTION_RESOURCE;
    private static final String SUBSCRIPTIONS_RESOURCE_CONFIGURATION = SUBSCRIPTIONS_RESOURCE_URL + "/" + CONFIGURATION_NAME;
    protected static final String TEST_URL_EXIST = TEST_URL + "/" + TEST_URL_GET_DELETE;
    private final static String BCKMGR_ID = "1";
    private static final String REST_TEMPLATE_ID = "CMM";

    public EasyMockRule rule = new EasyMockRule(this);

    private static final HttpEntity<MultiValueMap<String, Object>> HTTP_ENTITY = new HttpEntity<>(new LinkedMultiValueMap());
    private static final SchemaRequest SCHEMA_REQUEST = new SchemaRequest(CONFIGURATION_NAME, HTTP_ENTITY);

    @Before
    public void setup() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        etagNotifIdBase = new EtagNotifIdBase();
        etagNotifIdBase.updateEtag("11111");
        headers = new HttpHeaders();
        headers.add("ETag", "11111");
        cmmClient = new CMMClient();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxAttempts("5");
        cmmClient.setMaxDelay("3000");
        responseMock = createMock(ResponseEntity.class);
        expect(responseMock.getBody()).andReturn(mapper.writeValueAsString(getJsonNode())).anyTimes();
        expect(responseMock.getHeaders()).andReturn(headers).anyTimes();
        expect(responseMock.getStatusCode()).andReturn(HttpStatus.OK);

        backupManagerPatchFactory = createMock(BackupManagerPatchFactory.class);
        backupPatchFactory = createMock(BackupPatchFactory.class);
        progressReportPatchFactory = createMock(ProgressReportPatchFactory.class);
        housekeepingPatchFactory = createMock(HousekeepingPatchFactory.class);

        schedulerPatchFactory = createMock(SchedulerPatchFactory.class);
        periodicEventPatchFactory = createMock(PeriodicEventPatchFactory.class);
        schemaRequestFactory = createMock(SchemaRequestFactory.class);
        backupManagerRepository = createMock(BackupManagerRepository.class);
        cmmSubscriptionRequestFactory = createMock(CMMSubscriptionRequestFactory.class);
        keyStoreService = createMock(KeyStoreService.class);

        jsonService = new JsonService();


        cmmMessageFactory = new CMMMessageFactory();
        cmmMessageFactory.setHousekeepingPatchFactory(housekeepingPatchFactory);
        cmmMessageFactory.setPeriodicEventPatchFactory(periodicEventPatchFactory);
        cmmMessageFactory.setSchedulerPatchFactory(schedulerPatchFactory);
        cmmMessageFactory.setBackupPatchFactory(backupPatchFactory);
        cmmMessageFactory.setBackupManagerPatchFactory(backupManagerPatchFactory);
        cmmMessageFactory.setSchemaRequestFactory(schemaRequestFactory);
        cmmMessageFactory.setBackupManagerRepository(backupManagerRepository);
        cmmMessageFactory.setCMMSubscriptionRequestFactory(cmmSubscriptionRequestFactory);
        cmmMessageFactory.setJsonService(jsonService);
        brmEricssonJson = Optional.empty();

        service = new CMMediatorService(cmmClient, etagNotifIdBase, cmmMessageFactory);
        service.setKeyStoreService(keyStoreService);
        service.setProgressReportPatchFactory(progressReportPatchFactory);
        service.setCMMMessageFactory(cmmMessageFactory);

        final RestTemplate inner = EasyMock.createMock(RestTemplate.class);
        restTemplate = new Cached<>(() -> inner);
        restTemplateFactory = EasyMock.createMock(RestTemplateFactory.class);
        expect(restTemplateFactory.getRestTemplate(eq(REST_TEMPLATE_ID), anyObject())).andReturn(restTemplate).anyTimes();
        restTemplate.get().put(anyObject(), anyObject(), anyObject(String.class));
        expectLastCall().anyTimes();
        replay(restTemplateFactory);

        cmmRestClient = new CMMRestClient();

        cmmRestClient.setCmUrl(TEST_URL);
        cmmRestClient.setRestTemplateConfiguration(restTemplateFactory, false);
        cmmRestClient.setEtagNotifidBase(etagNotifIdBase);


        cmmClient.setCmmRestClient(cmmRestClient);
        cmmClient.setEtagNotifidBase(etagNotifIdBase);
        service.setInitialize(true);

        expect(schemaRequestFactory.getRequestToCreateSchema()).andReturn(SCHEMA_REQUEST).anyTimes();
        expect(cmmSubscriptionRequestFactory.getRequestToCreateSchema()).andReturn(SCHEMA_REQUEST).anyTimes();
        keyStoreService.regenerateKeyStoreForAlias(anyObject());
        expectLastCall().anyTimes();
        initEtagNotifId(TEST_URL_EXIST);

    }

    @After
    public void tearDown() {
        cmmClient.setFlagEnabled(false);
        cmmClient.stopProcessing();
        executorService.shutdownNow();
    }

    @Test
    public void initCMMediator_flagEnabledTrue_preparesCMForToOrchestratorToUseIt() throws Exception {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);

        getRequests();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().anyTimes();
        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        initEtagNotifId(NACM_URL);
        replayMocks();
        service.initCMMediator();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> url.hasCaptured());

        verify(restTemplate.get());
    }

    @Test
    public void initCMMediator_flagEnabledTrueAndAnythingGoesWrong_retrySuccessful() throws Exception {
        final Capture<String> url = Capture.newInstance();
        final Capture<String> patchUrl = Capture.newInstance();

        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxAttempts("5");
        service.setCmmClient(cmmClient);

        getRequests();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.capture(patchUrl), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().andThrow(new ResourceAccessException("")).times(5);
        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall();
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall();

        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        initEtagNotifId(NACM_URL);

        replayMocks();
        service.initCMMediator();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> url.hasCaptured());

    }

    @Test
    public void uploadSchema_schemaExistsAndVersionMatch_doesNothing() {
        final JsonService jsonService = createMock(JsonService.class);
        expect(jsonService.parseJsonStringAndFetchValue(anyObject(), anyObject(), anyObject())).andReturn(Optional.of("0.0.0")).anyTimes();
        replay(jsonService);

        final Capture<String> url = Capture.newInstance();
        final Capture<String> patchUrl = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);

        getRequests();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.capture(patchUrl), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().anyTimes();
        initEtagNotifId(NACM_URL);
        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        replayMocks();
        service.initCMMediator();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> url.hasCaptured());
        verify(restTemplate.get());
    }

    @Test
    public void createSchema_throwsException_callsRemedy() throws Exception {
        final Capture<String> url = Capture.newInstance();
        final Capture<String> patchUrl = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);

        getRequests();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.capture(patchUrl), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().anyTimes();

        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        initEtagNotifId(NACM_URL);
        replayMocks();
        service.initCMMediator();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> url.hasCaptured());
    }

    @Test
    public void uploadSchema_schemaExistsAndVersionMismatch_DeletesAndCreatesSchema() {
        final JsonService jsonService = createMock(JsonService.class);
        expect(jsonService.parseJsonStringAndFetchValue(anyObject(), anyObject(), anyObject())).andReturn(Optional.empty()).anyTimes();
        replay(jsonService);

        final Capture<String> url = Capture.newInstance();
        final Capture<String> patchUrl = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);
        cmmClient.setEtagNotifidBase(etagNotifIdBase);

        getRequests();
        expect(restTemplate.get().postForObject(EasyMock.eq(SCHEMAS_URL), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("");
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").times(1);
        expect(restTemplate.get().patchForObject(EasyMock.capture(patchUrl), EasyMock.capture(request), EasyMock.anyObject())).andReturn("");
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().anyTimes();

        initEtagNotifId(NACM_URL, HttpStatus.BAD_REQUEST);

        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        replayMocks();
        service.initCMMediator();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> patchUrl.hasCaptured());
        verify(restTemplate.get());
    }

    @Test
    public void deleteSchema_exception_callRemedy() throws InterruptedException {
        final Capture<String> url = Capture.newInstance();
        final Capture<String> patchUrl = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);

        getRequests();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.capture(patchUrl), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();


        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().andThrow(new RuntimeException());
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall();
        initEtagNotifId(NACM_URL);
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().anyTimes();

        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        replayMocks();
        service.initCMMediator();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> url.hasCaptured());
    }

    @Test
    public void deleteSubscription_exception_callRemedy() throws InterruptedException {
        final JsonService jsonService = createMock(JsonService.class);
        expect(jsonService.parseJsonStringAndFetchValue(anyObject(), anyObject(), anyObject())).andReturn(Optional.empty()).anyTimes();
        replay(jsonService);

        final Capture<String> url = Capture.newInstance();
        final Capture<String> patchUrl = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        final Capture<String> postURL = Capture.newInstance();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxAttempts("1");

        getRequests();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andThrow(new ResourceAccessException("")).times(2);
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").times(1);
        // Expect the createConfiguration fail and remedy retry
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andThrow(new ResourceAccessException("")).times(1);
        expect(restTemplate.get().postForObject(EasyMock.capture(postURL), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();

        expect(restTemplate.get().patchForObject(EasyMock.capture(patchUrl), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().andThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().anyTimes();

        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        initEtagNotifId(NACM_URL);
        replayMocks();
        service.initCMMediator();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> postURL.hasCaptured() && postURL.getValue().contains("subscriptions"));
        verify(restTemplate.get());
    }

    @Test
    public void createConfiguration_exceptionOnCreateConfiguration_callsRemedy() throws InterruptedException {
        final JsonService jsonService = createMock(JsonService.class);
        expect(jsonService.parseJsonStringAndFetchValue(anyObject(), anyObject(), anyObject())).andReturn(Optional.of("0.0.0")).anyTimes();
        replay(jsonService);

        final Capture<String> url = Capture.newInstance();
        final Capture<String> schemaURL = Capture.newInstance();
        final Capture<String> patchUrl = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        final Capture<String> postURL = Capture.newInstance();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxAttempts("1");

        getRequests();
        // Delete configuration
        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();

        // UploadSchema
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").times(6);
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().times(2);
        expect(restTemplate.get().postForObject(eq(SCHEMAS_URL), anyObject(), anyObject())).andReturn("").times(2);
        // expect(restTemplate.get().postForObject(eq(SUBSCRIPTIONS_RESOURCE_URL), anyObject(), anyObject())).andReturn("");


        // Expect the createConfiguration fail and remedy retry
        expect(restTemplate.get().postForObject(eq(TEST_URL_CONFIGURATION), anyObject(), anyObject())).andThrow(new ResourceAccessException("")).times(1);
        expect(restTemplate.get().postForObject(eq(TEST_URL_CONFIGURATION), anyObject(), anyObject())).andThrow(new NullPointerException("")).times(1);
        expect(restTemplate.get().postForObject(eq(TEST_URL_CONFIGURATION), EasyMock.capture(request), anyObject())).andReturn("").anyTimes();

        expect(restTemplate.get().patchForObject(EasyMock.capture(patchUrl), anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();

        // restTemplate.get().delete(SUBSCRIPTIONS_URL);
        // expectLastCall().times(1);

        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        initEtagNotifId(NACM_URL);

        replayMocks();
        service.initCMMediator();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> request.hasCaptured());
        verify(restTemplate.get());
      }


    @Test
    public void createSubscription_exception_callRemedy() throws InterruptedException {

        final Capture<String> url = Capture.newInstance();
        final Capture<String> patchUrl = Capture.newInstance();
        RestClientException expectedException = new RestClientException("expected");
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);

        getRequests();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andThrow(new HttpClientErrorException(HttpStatus.NO_CONTENT));
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();

        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().anyTimes();
        initEtagNotifId(NACM_URL);

        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().anyTimes();

        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        replayMocks();
        service.initCMMediator();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> url.hasCaptured());
        verify(restTemplate.get());
    }

    @Test
    public void createSubscription_exception_callRemedy_BadRequest() throws InterruptedException {

        final Capture<String> url = Capture.newInstance();
        final Capture<String> patchUrl = Capture.newInstance();
        RestClientException expectedException = new RestClientException("expected");
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);

        getRequests();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class))
        .andThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();

        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().andThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall();

        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        replayMocks();
        service.initCMMediator();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> url.hasCaptured());
        verify(restTemplate.get());
    }

    @Test
    public void updateSubscription_correct() {
        cmmClient.setFlagEnabled(true);
        expect(cmmSubscriptionRequestFactory.getRequestToUpdateSchema()).andReturn(SCHEMA_REQUEST).anyTimes();
        restTemplate.get().put(eq(SUBSCRIPTIONS_RESOURCE_URL), anyObject(), anyString());
        expectLastCall().anyTimes();
        replayMocks();
    }

    @Test
    public void addNacmRole_roleExists_doesNotAddRoles() {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);
        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(NACM_URL, String.class)).andReturn(SEARCH_STRING_SYSTEM_ADMIN + SEARCH_STRING_SYSTEM_READ_ONLY).anyTimes();
        expect(restTemplate.get().getForObject(SUBSCRIPTIONS_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().anyTimes();
        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        initEtagNotifId(NACM_URL);
        replayMocks();
        service.initCMMediator();
        verify(restTemplate.get());
    }

    @Test
    public void cmmExists_infiniteRetry() {
        final CMMClient cmService;
        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
        .times(10);
        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andReturn("");
        cmService = EasyMock.createMockBuilder(CMMClient.class).createMock();
        cmmRestClient.setRestTemplateConfiguration(restTemplateFactory, false);
        cmmRestClient.setCmUrl(TEST_URL);
        cmService.setCmmRestClient(cmmRestClient);
        replay(cmService, restTemplate.get());
        Assert.assertTrue(cmService.cmmExists(TEST_URL_GET_DELETE, 5, true));
        verify(cmService, restTemplate.get());
    }

    @Test
    public void cmmExists_retryAttemptFive_failsAfterFiveRetry() {
        final CMMClient cmmClient;
        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
        .times(5);
        cmmClient = EasyMock.createMockBuilder(CMMClient.class).createMock();
        cmmRestClient.setRestTemplateConfiguration(restTemplateFactory, false);
        cmmRestClient.setCmUrl(TEST_URL);
        replay(cmmClient, restTemplate.get());
        Assert.assertFalse(cmmClient.cmmExists(TEST_URL_GET_DELETE, 5, false));
        verify(cmmClient);
    }

    @Test(expected = HttpServerErrorException.class)
    public void exists_noRetry_throwsExceptionAfterOneTry() {
        final CMMClient cmService;
        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
        .times(1);
        cmService = EasyMock.createMockBuilder(CMMClient.class).createMock();
        cmmRestClient.setRestTemplateConfiguration(restTemplateFactory, false);
        cmmRestClient.setCmUrl(TEST_URL);
        cmService.setCmmRestClient(cmmRestClient);
        replay(cmService, restTemplate.get());
        cmService.exists(TEST_URL_GET_DELETE);
        verify(cmService);
    }

    @Test
    public void prepareCMMediator_flagEnabledTrue_callsDeleteConfigurationWithRetryAttemptTen_Exception() {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxAttempts("0");
        service.setBackupManagerRepository(backupManagerRepository);

        getRequests();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().andThrow(new RuntimeException()).anyTimes();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().andThrow(new RuntimeException()).anyTimes();
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().andThrow(new RuntimeException()).anyTimes();
        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        initEtagNotifId(NACM_URL);

        replayMocks();
        service.prepareCMMediator(false);
    }

    @Test
    public void prepareCMMediator_isStartUp_callsDeleteConfigurationWithRetryAttemptTen_Exception() {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxAttempts("0");
        service.setBackupManagerRepository(backupManagerRepository);

        getRequests();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().andThrow(new RuntimeException()).anyTimes();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().andThrow(new RuntimeException()).anyTimes();
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().andThrow(new RuntimeException()).anyTimes();
        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        initEtagNotifId(NACM_URL);

        replayMocks();
        service.prepareCMMediator(true);
    }

    @Test
    public void prepareCMMediator_flagEnabledTrue_callsDeleteConfigurationWithRetryAttemptSix() throws InterruptedException {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxDelay("200");
        service.setBackupManagerRepository(backupManagerRepository);

        getRequests();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();

        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn(getSchema()).anyTimes();

        final CountDownLatch latch = new CountDownLatch(6);

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().andAnswer(new IAnswer<Void>() {
            @Override
            public Void answer() throws Throwable {
                latch.countDown();
                System.out.println("CMM Error");
                throw new ResourceAccessException("CMM is not available");
            }
        }).times(6);

        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        replayMocks();
        executorService.submit(() -> service.prepareCMMediator(false));
        latch.await();
        cmmClient.setFlagEnabled(false);
        verify(restTemplate.get());
    }

    @Test
    public void addBackupManager_cmIntegrationEnabledAndAnythingGoesWrong_triesToCleanUpCMBySendingSchemaAndConfiguration() throws Exception {
        final String ericssonBRMConfiguration=getEricssonJsonBRMConfiguration().toString();
        final AddBackupManagerPath addBackupManagerPath = new AddBackupManagerPath();

        cmmClient.setFlagEnabled(true);
        final BackupManager backupManager = createMock(BackupManager.class);
        final AddBackupManagerPatch patch = createMock(AddBackupManagerPatch.class);
        expect(backupManagerPatchFactory.getPatchToAddBackupManager(backupManager)).andReturn(patch);
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION_BASE, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().exchange(TEST_URL_CONFIGURATION_BASE, HttpMethod.GET, null, String.class)).andReturn(responseMock).anyTimes();

        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(brmEricssonJson).anyTimes();
        expect(backupPatchFactory.getPathToAddBackupManager()).andReturn(addBackupManagerPath);

        expectLastCall();
        replayMocks();

        service.addBackupManager(backupManager);
        verify(restTemplate.get());
    }

    @Test
    public void addBackupManager_cmIntegrationEnabledAndAnythingGoesWrongWhileTryingToCleanUpCM_throwsException() throws Exception {
        cmmClient.setFlagEnabled(true);
        final BackupManager backupManager = createMock(BackupManager.class);
        final AddBackupManagerPatch patch = createMock(AddBackupManagerPatch.class);
        final AddBackupManagerPath addBackupManagerPath = new AddBackupManagerPath();
        expect(backupManagerPatchFactory.getPatchToAddBackupManager(backupManager)).andReturn(patch);
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();

        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION_BASE, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().exchange(TEST_URL_CONFIGURATION_BASE, HttpMethod.GET, null, String.class)).andReturn(responseMock).anyTimes();

        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(brmEricssonJson).anyTimes();
        expect(backupPatchFactory.getPathToAddBackupManager()).andReturn(addBackupManagerPath);
        
        replayMocks();

        service.addBackupManager(backupManager);

    }

    @Test
    public void addBackupManager_cmIntegrationEnabled_createsPatchAndUpdatesConfigurationWithNewBackupManager() throws Exception {
        cmmClient.setFlagEnabled(true);
        final BackupManager backupManager = createMock(BackupManager.class);
        final AddBackupManagerPatch patch = createMock(AddBackupManagerPatch.class);
        final AddBackupManagerPath addBackupManagerPath = new AddBackupManagerPath();

        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION_BASE, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().exchange(TEST_URL_CONFIGURATION_BASE, HttpMethod.GET, null, String.class)).andReturn(responseMock).anyTimes();

        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(brmEricssonJson).anyTimes();
        expect(backupPatchFactory.getPathToAddBackupManager()).andReturn(addBackupManagerPath);
        
        expect(backupManagerPatchFactory.getPatchToAddBackupManager(backupManager)).andReturn(patch);

        replayMocks();
        service.addBackupManager(backupManager);
        verify(restTemplate.get());
    }

    @Test
    public void addBackupManager_cmIntegrationDisabled_doesNothing() throws Exception {
        final Capture<String> patchUrl = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();

        final AddBackupManagerPatch patch = createMock(AddBackupManagerPatch.class);
        final AddBackupManagerPath addBackupManagerPath = new AddBackupManagerPath();
        final BRMEricssonbrmJson brmEricConfiguration = getbrmEricConfiguration();

        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION_BASE, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().exchange(TEST_URL_CONFIGURATION_BASE, HttpMethod.GET, null, String.class)).andReturn(responseMock).anyTimes();

        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(Optional.of(brmEricConfiguration)).anyTimes();
        expect(backupPatchFactory.getPathToAddBackupManager()).andReturn(addBackupManagerPath);
        expect(backupManagerPatchFactory.getPatchToAddBackupManager(anyObject())).andReturn(patch);
        replayMocks();
        service.addBackupManager(createMock(BackupManager.class));
    }

    @Test
    public void addBackupManager_cmIntegrationDisabled_ConflictException_run_remedyPatch() throws Exception {
        final Capture<String> patchUrl = Capture.newInstance();

        final Capture<HttpEntity<?>> request = Capture.newInstance();
        final PatchRequest patchRequest = createMock(PatchRequest.class);
        final AddBackupManagerPath addBackupManagerPath = new AddBackupManagerPath();
        final Backup toAddbackup = new Backup("123", "456", Optional.empty(), MANUAL, null);

        final AddInitialBackupPatch initialPatch = new AddInitialBackupPatch(toAddbackup, addBackupManagerPath.getPath());

        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION_BASE, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().exchange(TEST_URL_CONFIGURATION_BASE, HttpMethod.GET, null, String.class)).andReturn(responseMock).anyTimes();

        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(brmEricssonJson).anyTimes();
        expect(backupPatchFactory.getPathToAddBackupManager()).andReturn(addBackupManagerPath);

        ResponseEntity<JsonNode> responseMock = createMock(ResponseEntity.class);
        expect(responseMock.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
        expect(responseMock.getHeaders()).andReturn(headers).anyTimes();
        expect(backupPatchFactory.getPatchToAddInitialBackup(EasyMock.anyObject(),EasyMock.anyObject())).andReturn(initialPatch);

        BackupManager backupManagerMock = createMock(BackupManager.class);

        final AddBackupManagerPatch patch = createMock(AddBackupManagerPatch.class);
        expect(patch.toJson()).andReturn(patchRequest);
        patch.setEtag(EasyMock.anyString());
        expectLastCall();
        expect(patch.getEtag()).andReturn("");
        expect(backupManagerPatchFactory.getPatchToAddBackupManager(anyObject())).andReturn(patch);
        expect(backupManagerMock.getBackupManagerId()).andReturn("1");
        replay(responseMock, backupManagerMock, patch);
        replayMocks();
        service.addBackupManager(backupManagerMock);
        assertEquals(initialPatch.getJsonOfOperations().get(0).getPath(), "/ericsson-brm:brm/backup-manager//ericsson-brm:brm/backup-manager");
    }

    @Test
    public void updateBackupManager_cmIntegrationEnabled_createsPatchAndUpdatesConfiguration() throws Exception {
        RestClientException expectedException = new RestClientException("expected");

        cmmClient.setFlagEnabled(true);
        final BackupManager backupManager = createMock(BackupManager.class);
        final UpdateBackupManagerPatch patch = createMock(UpdateBackupManagerPatch.class);
        expect(backupManagerPatchFactory.getPatchToUpdateBackupManager(anyObject(BackupManager.class))).andReturn(patch).anyTimes();
        restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject());
        expectLastCall().andThrow(expectedException).anyTimes();

        replayMocks();
        service.updateBackupManager(backupManager);

    }

    @Test
    public void updateBackupManager_cmIntegrationDisabled_doesNothing() throws Exception {
        cmmClient.setFlagEnabled(true);
        final AddBackupManagerPatch patch = createMock(AddBackupManagerPatch.class);
        final AddBackupManagerPath addBackupManagerPath = new AddBackupManagerPath();

        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION_BASE, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().exchange(TEST_URL_CONFIGURATION_BASE, HttpMethod.GET, null, String.class)).andReturn(responseMock).anyTimes();

        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(brmEricssonJson).anyTimes();
        expect(backupPatchFactory.getPathToAddBackupManager()).andReturn(addBackupManagerPath);

        expect(backupManagerPatchFactory.getPatchToAddBackupManager(anyObject())).andReturn(patch).anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();

        replayMocks();
        service.addBackupManager(createMock(BackupManager.class));

    }

    @Test
    public void addBackup_cmIntegrationEnabled_createsPatchAndUpdatesConfigurationAddingBackup() throws Exception {
        final Backup backup = mockBackup();;
        cmmClient.setFlagEnabled(true);
        final BackupManager manager = createMock(BackupManager.class);
        final AddBackupPatch patch = createMock(AddBackupPatch.class);
        final Backup toAddbackup = new Backup("123", "456", Optional.empty(), MANUAL, null);
        final AddBackupManagerPath addBackupManagerPath = new AddBackupManagerPath();
        final AddInitialBackupPatch initialPatch = new AddInitialBackupPatch(toAddbackup, addBackupManagerPath.getPath());


        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn(getSchema()).anyTimes();
        expect(restTemplate.get().getForObject(NACM_URL, String.class)).andReturn(SEARCH_STRING_SYSTEM_ADMIN + SEARCH_STRING_SYSTEM_READ_ONLY).anyTimes();
        expect(restTemplate.get().getForObject(SUBSCRIPTIONS_URL, String.class)).andReturn("").anyTimes();
        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        expect(manager.getBackups(anyObject())).andReturn(Arrays.asList(backup)).times(2);
        expect(manager.getBackups(anyObject())).andReturn(new ArrayList<>()).times(2);
        expect(restTemplate.get().postForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(backupPatchFactory.getPatchToAddInitialBackup(EasyMock.anyObject(),EasyMock.anyObject())).andReturn(initialPatch);

        replayMocks();
        replay(manager, patch);
        service.addBackup(manager, backup);
    }

    @Test
    public void addBackup_cmIntegrationDisabled_doesNothing() throws Exception {
        final BackupManager manager = createMock(BackupManager.class);
        final AddBackupPatch patch = createMock(AddBackupPatch.class);
        final AddInitialBackupPatch initialPatch = createMock (AddInitialBackupPatch.class);
        final Backup toAddbackup = new Backup("123", "456", Optional.empty(), MANUAL, null);
        final ArrayList arrayBackup = new ArrayList();
        cmmClient.setFlagEnabled(true);
        arrayBackup.add(toAddbackup);
        expect(manager.getBackups(Ownership.READABLE)).andReturn(arrayBackup).anyTimes();
        expect(backupPatchFactory.getPatchToAddInitialBackup(EasyMock.anyObject(),EasyMock.anyObject())).andReturn(initialPatch);

        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn(getSchema()).anyTimes();
        expect(restTemplate.get().getForObject(NACM_URL, String.class)).andReturn(SEARCH_STRING_SYSTEM_ADMIN + SEARCH_STRING_SYSTEM_READ_ONLY).anyTimes();
        expect(restTemplate.get().getForObject(SUBSCRIPTIONS_URL, String.class)).andReturn("").anyTimes();
        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        expect(restTemplate.get().postForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().anyTimes();
        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();

        replay(manager, initialPatch);
        replayMocks();
        service.addBackup(manager, toAddbackup);
    }

    @Test
    public void updateBackup_cmIntegrationEnabled_createsPatchAndUpdatesConfigurationUpdatingBackup() throws Exception {
        cmmClient.setFlagEnabled(true);
        final BackupManager manager = createMock(BackupManager.class);
        final Backup backup = createMock(Backup.class);
        final UpdateBackupPatch patch = createMock(UpdateBackupPatch.class);
        expect(backupPatchFactory.getPatchToUpdateBackup(manager, backup)).andReturn(patch).anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();

        replayMocks();
        service.updateBackup(manager, backup);

    }

    @Test
    public void updateBackup_cmIntegrationDisabled_doesNothing() throws Exception {
        expect(restTemplate.get().patchForObject(anyObject(), anyObject(), anyObject())).andReturn("").anyTimes();
        UpdateBackupPatch updateBackupPatch = createMock(UpdateBackupPatch.class);
        expect(backupPatchFactory.getPatchToUpdateBackup(anyObject(), anyObject())).andReturn(updateBackupPatch).anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();

        replayMocks();
        service.updateBackup(createMock(BackupManager.class), createMock(Backup.class));

    }

    @Test
    public void deleteBackup_cmIntegrationEnabled_createsPatchAndUpdatesConfigurationDeletingBackup() throws Exception {
        cmmClient.setFlagEnabled(true);
        final DeleteBackupPatch patch = createMock(DeleteBackupPatch.class);
        expect(backupPatchFactory.getPatchToDeleteBackup("1", 1)).andReturn(patch).anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();

        replayMocks();
        service.deleteBackup("1", 1);
        verify(restTemplate.get());
    }

    @Test
    public void deleteBackup_cmIntegrationDisabled_doesNothing() throws Exception {
        cmmClient.setFlagEnabled(false);
        replayMocks();

        service.deleteBackup("1", 1);

    }

    @Test
    public void addProgressReport_cmIntegrationEnabledAndNoProgressReportExist_addNewProgressReport() throws Exception {
        final Action action = createMock(Action.class);
        final AddProgressReportPatch add = createMock(AddProgressReportPatch.class);

        final BRMEricssonbrmJson brmEricConfiguration = getbrmEricConfiguration();
        final List<BackupManager> backupManagers = new ArrayList<BackupManager>();
        final BackupManager backupMgr = mockBackupManager(BCKMGR_ID, "a", "b", false);
        final UpdateProgressReportPatch update = createMock(UpdateProgressReportPatch.class);
        backupManagers.add(backupMgr);

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();

        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().anyTimes();

        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMAS_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(NACM_URL, String.class)).andReturn("").anyTimes();
        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(Optional.empty()).anyTimes();

        expect(action.getActionId()).andReturn("123456").anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.isRestoreOrExport()).andReturn(false).once();
        expect (action.getBackupManagerId()).andReturn("1").anyTimes();
        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(EasyMock.anyObject(Action.class))).andReturn(Arrays.asList(update)).anyTimes();
        expect(progressReportPatchFactory.getPatchToAddProgressReport(EasyMock.anyObject(Action.class))).andReturn(Arrays.asList(add)).anyTimes();

        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();

        expect(backupManagerRepository.getBackupManager(EasyMock.anyString())).andReturn(backupMgr).anyTimes();

        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andReturn("").anyTimes();
        service.setBackupManagerRepository(backupManagerRepository);
        replayMocks();
        replay(action);
        cmmClient.setFlagEnabled(true);
        service.enqueueProgressReport(action);
    }

    @Test
    public void addProgressReport_cmIntegrationEnabledAndNoProgressReportExist_addNewProgressReport_getBackups_BRMBackupJsonEmpty() throws Exception {
        final Action action = createMock(Action.class);
        final BRMEricssonbrmJson brmEricConfiguration = getbrmEricConfiguration(false);
        final List<BackupManager> backupManagers = new ArrayList<BackupManager>();
        final BackupManager backupMgr = mockBackupManager(BCKMGR_ID, "a", "b", false);
        final UpdateProgressReportPatch update = createMock(UpdateProgressReportPatch.class);
        final AddProgressReportPatch add = createMock(AddProgressReportPatch.class);
        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(Optional.empty()).anyTimes();

        backupManagers.add(backupMgr);

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall();

        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(NACM_URL, String.class)).andReturn("").anyTimes();

        expect(action.getActionId()).andReturn("123456").anyTimes();
        expect(action.getName()).andReturn(ActionType.EXPORT).anyTimes();
        expect(action.isRestoreOrExport()).andReturn(false).once();
        expect (action.getBackupManagerId()).andReturn("1").anyTimes();
        expect (action.getBackupName()).andReturn("Error").anyTimes();
        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(update));
        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();


        expect(backupManagerRepository.getBackupManager(EasyMock.anyString())).andReturn(backupMgr).anyTimes();
        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(add)).anyTimes();
        service.setBackupManagerRepository(backupManagerRepository);
        replay(action);
        replayMocks();
        cmmClient.setFlagEnabled(true);
        service.enqueueProgressReport(action);
    }

    @Test
    public void addProgressReport_cmIntegrationEnabledAndNoProgressReportExist_addNewProgressReport_getBackups_getProgressReports() throws Exception {
        final Action action = createMock(Action.class);
        final List<BackupManager> backupManagers = new ArrayList<BackupManager>();
        final BackupManager backupMgr = mockBackupManager(BCKMGR_ID, "a", "b", false);
        final UpdateProgressReportPatch update = createMock(UpdateProgressReportPatch.class);
        final AddProgressReportPatch add = createMock(AddProgressReportPatch.class);
        final BackupManagerRepository backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);
        backupManagers.add(backupMgr);
        expect (action.getProgressPercentage()).andReturn(0.9);
        expect(add.getAction()).andReturn(action);

        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().anyTimes();
        restTemplate.get().delete(SCHEMA_URL);
        expectLastCall().anyTimes();
        expect(restTemplate.get().postForObject(EasyMock.anyObject(String.class), EasyMock.anyObject(),
                EasyMock.anyObject())).andReturn("");

        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();

        expect(action.getActionId()).andReturn("123456").anyTimes();
        expect(action.getName()).andReturn(ActionType.EXPORT).anyTimes();
        expect(action.isRestoreOrExport()).andReturn(false).once();
        expect (action.getBackupManagerId()).andReturn("1").anyTimes();
        expect (action.getBackupName()).andReturn("E").anyTimes();
        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(Optional.empty()).anyTimes();
        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(update));
        expect(backupManagerRepository.getBackupManagers()).andReturn(backupManagers).anyTimes();
        expect(backupManagerRepository.getBackupManager(EasyMock.anyString())).andReturn(backupMgr).anyTimes();

        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(add)).anyTimes();

        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andReturn("");
        EasyMock.expectLastCall().anyTimes();
        service.setBackupManagerRepository(backupManagerRepository);
        replay(action, backupManagerRepository, add);
        replayMocks();
        cmmClient.setFlagEnabled(true);
        service.enqueueProgressReport(action);
    }

    @Test
    public void addProgressReport_cmIntegrationEnabled_ProgressPercentageIs33Percent_CMIsNotAvailable_NoExceptionThrown() throws Exception {
        final Action action = createMock(Action.class);
        final BackupManager backupMgr = mockBackupManager(BCKMGR_ID, "a", "b", false);
        final BackupManagerRepository backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);
        final AddProgressReportPatch add = createMock(AddProgressReportPatch.class);
        final PatchRequest request = EasyMock.createMock(PatchRequest.class);

        expect(action.getActionId()).andReturn("123456").anyTimes();
        expect(add.toJson()).andReturn(request);
        expect (action.getProgressPercentage()).andReturn(0.9);
        expect(add.getAction()).andReturn(action);
        expect(action.isRestoreOrExport()).andReturn(false).once();

        expect(backupManagerRepository.getBackupManager(EasyMock.anyString())).andReturn(backupMgr).anyTimes();
        expect(action.getName()).andReturn(ActionType.EXPORT).anyTimes();
        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(brmEricssonJson).anyTimes();

        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMAS_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();
        restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject());
        expectLastCall().andReturn("").anyTimes();

        expect(progressReportPatchFactory.getPatchToAddProgressReport(EasyMock.anyObject(Action.class))).andReturn(Arrays.asList(add)).anyTimes();

        expect(action.getProgressPercentage()).andReturn(0.33).anyTimes();
        expect (action.getBackupManagerId()).andReturn("1").anyTimes();
        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andReturn("").anyTimes();

        service.setBackupManagerRepository(backupManagerRepository);
        replay(action, backupManagerRepository, add);
        replayMocks();
        cmmClient.setFlagEnabled(true);
        service.enqueueProgressReport(action);
    }

    @Test
    public void addProgressReport_cmIntegrationEnabledAndNoProgressReportExist_isEmptyProgressReport_emptyBackups() throws Exception {
        cmmClient.setFlagEnabled(true);
        final Action action = createMock(Action.class);
        final BackupManager backupMgr = mockBackupManager(BCKMGR_ID, "a", "b", false);
        final List<BackupManager> backupManagers = new ArrayList<BackupManager>();
        final UpdateProgressReportPatch update = createMock(UpdateProgressReportPatch.class);
        final AddProgressReportPatch add = createMock(AddProgressReportPatch.class);

        backupManagers.add(backupMgr);
        service.setBackupManagerRepository(backupManagerRepository);
        final BRMEricssonbrmJson brmEricConfiguration = getbrmEricConfiguration(true);

        expect(action.getActionId()).andReturn("123456").anyTimes();
        expect(action.getName()).andReturn(ActionType.EXPORT).times(2);
        expect(action.isRestoreOrExport()).andReturn(false);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupManagerId()).andReturn("1").anyTimes();
        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(Optional.empty()).anyTimes();

        getRequests();
        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();

        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(update));
        expect(backupManagerRepository.getBackupManager(EasyMock.anyString())).andReturn(backupMgr).anyTimes();
        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(add));

        replayMocks();
        replay(action);
        service.enqueueProgressReport(action);
    }

    @Test
    public void addProgressReport_cmIntegrationEnabledAndNoProgressReportExist_updateProgressReport() throws Exception {
        cmmClient.setFlagEnabled(true);
        final Action action = createMock(Action.class);
        final BackupManager backupMgr = mockBackupManager(BCKMGR_ID, "a", "b", true);
        final UpdateProgressReportPatch update = createMock(UpdateProgressReportPatch.class);
        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION, String.class)).andReturn("").anyTimes();
        expect(update.getAction()).andReturn(action);
        expect (action.getProgressPercentage()).andReturn(0.9);

        final BackupManagerRepository backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);

        getbrmEricConfiguration();
        service.setBackupManagerRepository(backupManagerRepository);

        expect(action.getBackupManagerId()).andReturn("1").anyTimes();
        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(update));
        expect(backupManagerRepository.getBackupManager(EasyMock.anyString())).andReturn(backupMgr).anyTimes();
        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(update));
        expect(action.isRestoreOrExport()).andReturn(false);
        expect(action.getActionId()).andReturn("123456").anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        replayMocks();
        replay(action, backupManagerRepository, update);

        service.enqueueProgressReport(action);
        verify(restTemplate.get());
    }

    @Test
    public void addProgressReport_cmIntegrationDisabled_doesNothing() throws Exception {
        cmmClient.setFlagEnabled(false);
        Action action = mockAction("1","test", ActionType.EXPORT);
        service.setBackupManagerRepository(backupManagerRepository);
        replayMocks();
        service.enqueueProgressReport(action);
        verify(restTemplate.get());
    }

    @Test
    public void addHousekeeping_cmIntegrationDisabled_doesNothing() throws Exception {
        cmmClient.setFlagEnabled(false);
        replayMocks();
        service.addHousekeeping(createMock(Housekeeping.class));
        verify(restTemplate.get());
    }

    @Test
    public void addHousekeeping_cmIntegrationEnabled_createsPatchAndUpdatesConfigurationAddingHousekeeping() throws Exception {
        cmmClient.setFlagEnabled(true);
        final Housekeeping housekeeping = createMock(Housekeeping.class);
        final AddHousekeepingPatch patch = createMock(AddHousekeepingPatch.class);
        expect(housekeepingPatchFactory.getPatchToAddHousekeeping(housekeeping)).andReturn(patch);
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();

        replayMocks();
        service.addHousekeeping(housekeeping);

    }

    @Test
    public void updateHousekeeping_cmIntegrationDisabled_doesNothing() throws Exception {
        final UpdateHousekeepingPatch patch = createMock(UpdateHousekeepingPatch.class);
        expect(housekeepingPatchFactory.getPatchToUpdateHousekeeping(EasyMock.anyObject())).andReturn(patch).anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();

        replayMocks();
        service.updateHousekeeping(createMock(Housekeeping.class));

    }

    @Test
    public void updateHousekeeping_cmIntegrationEnabled_createsPatchAndUpdatesConfigurationUpdatingHousekeeping_afterBAD_REQUEST() throws Exception {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        final PatchRequest patchRequest = createMock(PatchRequest.class);
        JSONObject json = getEricssonJsonBRMConfiguration();

        cmmClient.setFlagEnabled(true);
        cmmClient.setEtagNotifidBase(etagNotifIdBase);
        final Housekeeping housekeeping = createMock(Housekeeping.class);
        final UpdateHousekeepingPatch patch = createMock(UpdateHousekeepingPatch.class);
        patch.setEtag(EasyMock.anyString());
        expectLastCall().times(2);
        patch.setPath(EasyMock.anyString());
        expectLastCall().anyTimes();
        
        expect(patch.toJson()).andReturn(patchRequest).times(2);
        expect(patch.getPath()).andReturn("/data/ericsson-brm:brm/backup-manager").anyTimes();
        expect(patch.getOperation()).andReturn(PatchOperation.ADD).anyTimes();
        expect(housekeepingPatchFactory.getPatchToUpdateHousekeeping(housekeeping)).andReturn(patch).times(2);

        // fail with a BAD_REQUEST call and retry successfully
        expect(restTemplate.get().patchForObject(EasyMock.eq(TEST_URL_CONFIGURATION_BASE), EasyMock.eq(patchRequest), EasyMock.eq(String.class)))
        .andThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION_BASE, String.class)).andReturn(json.toString()).anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.capture(url), EasyMock.eq(patchRequest), EasyMock.eq(String.class))).andReturn("");

        replayMocks();
        replay(patch);

        // When patching CM, it will receive a BAD_REQUEST indicating that the command is malformed,
        // so it will stop retrying the execution.
        service.updateHousekeeping(housekeeping); // This will be ignored
        // The second PATCH, responds with a valid string, indicating that the call was successful.
        service.updateHousekeeping(housekeeping); // This will work ok
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> url.hasCaptured());
        assertTrue(url.getValue().equals(TEST_URL_CONFIGURATION_BASE));
        verify(restTemplate.get());

    }

    @Test
    public void addScheduler_cmIntegrationDisabled_doesNothing() throws Exception {
        cmmClient.setFlagEnabled(false);
        replayMocks();
        service.addScheduler(createMock(Scheduler.class));
        verify(restTemplate.get());
    }

    @Test
    public void addScheduler_cmIntegrationEnabled_createsPatchAndUpdatesConfigurationAddingScheduler() throws Exception {
        cmmClient.setFlagEnabled(true);
        final Scheduler scheduler = createMock(Scheduler.class);
        final AddSchedulerPatch patch = createMock(AddSchedulerPatch.class);
        expect(schedulerPatchFactory.getPatchToAddScheduler(scheduler)).andReturn(patch);
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();

        replayMocks();
        service.addScheduler(scheduler);
        verify(restTemplate.get());
    }

    @Test
    public void addPeriodicEvent_cmIntegrationEnabled_createsPatchAndUpdatesConfigurationUpdatingEvent() throws Exception {
        cmmClient.setFlagEnabled(true);
        final PeriodicEvent periodicEvent = createMock(PeriodicEvent.class);
        final AddPeriodicEventPatch patch = createMock(AddPeriodicEventPatch.class);
        expect(periodicEventPatchFactory.getPatchToAddPeriodicEvent(periodicEvent)).andReturn(patch);
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();

        replayMocks();
        service.addPeriodicEvent(periodicEvent);
        verify(restTemplate.get());
    }

    @Test
    public void updatePeriodicEvent_cmIntegrationEnabled_createsPatchAndUpdatesConfigurationUpdatingEvent() throws Exception {
        cmmClient.setFlagEnabled(true);
        final PeriodicEvent periodicEvent = createMock(PeriodicEvent.class);
        final UpdatePeriodicEventPatch patch = createMock(UpdatePeriodicEventPatch.class);
        expect(periodicEventPatchFactory.getPatchToUpdatePeriodicEvent(periodicEvent)).andReturn(patch).anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();
        replayMocks();
        service.updatePeriodicEvent(periodicEvent);
        verify(restTemplate.get());
    }

    @Test
    public void deletePeriodicEvent_cmIntegrationEnabled_createsPatchAndUpdatesConfigurationDeletingEvent() throws Exception {
        cmmClient.setFlagEnabled(true);
        final PeriodicEvent periodicEvent = createMock(PeriodicEvent.class);
        final DeletePeriodicEventPatch patch = createMock(DeletePeriodicEventPatch.class);
        expect(periodicEventPatchFactory.getPatchToDeletePeriodicEvent(periodicEvent)).andReturn(patch);
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();
        replayMocks();
        replay(periodicEvent, patch);
        service.deletePeriodicEvent(periodicEvent);
        verify(restTemplate.get());
    }

    @Test
    public void updateScheduler_cmIntegrationDisabled_doesNothing() throws Exception {
        cmmClient.setFlagEnabled(false);
        replayMocks();
        service.updateScheduler(createMock(Scheduler.class));
        verify(restTemplate.get());
    }

    @Test
    public void updateScheduler_cmIntegrationEnabled_createsPatchAndUpdatesConfigurationUpdatingScheduler() throws Exception {
        final Scheduler scheduler = createMock(Scheduler.class);
        final UpdateSchedulerPatch patch = createMock(UpdateSchedulerPatch.class);
        expect(schedulerPatchFactory.getPatchToUpdateScheduler(scheduler)).andReturn(patch).anyTimes();
        expect(restTemplate.get().patchForObject(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn("").anyTimes();
        replayMocks();
        service.updateScheduler(scheduler);
        verify(restTemplate.get());
    }

    @Test
    public void BRMConfigurationService_getEricssonBRMConfiguration() {
        final JsonService jsonService = EasyMock.createMock(JsonService.class);
        final BRMEricssonbrmJson brmEricConfiguration = getbrmEricConfiguration();
        expect(jsonService.parseJsonString(EasyMock.anyString(), EasyMock.anyObject())).andReturn(Optional.of(brmEricConfiguration));
        final CMMSubscriptionRequestFactory cmmSubscriptionRequestFactory = new CMMSubscriptionRequestFactory();
        cmmSubscriptionRequestFactory.setJsonService(new JsonService());
        replay(jsonService);
    }

    @Test
    public void validEtagNotifId_biggerNotifId_True() {
        // service includes a default etag "11111",0
        // comparing with the same etag and notifId value
        EtagNotifIdBase etagNotifIdNew = new EtagNotifIdBase();
        etagNotifIdNew.updateEtag("11111");
        etagNotifIdNew.setNotifId(1000);

        assertTrue(service.isValidEtagNotifId(etagNotifIdNew.getEtag(), etagNotifIdNew.getNotifId()));
    }

    @Test
    public void getIndexBackupManagerIndexfromRepository_reloadConfiguration_ContextFromCMM() throws JSONException {
        // service includes a default etag "11111",0
        // comparing with the same etag and notifId value
        final JSONObject json = getEricssonJsonBRMConfiguration();
        final String ericssonBRMConfiguration=json.toString();
        final BackupManager backupMgr = mockBackupManager(BCKMGR_ID, "a", "b", false);
        final CMMSubscriptionRequestFactory configurationRequestFactory = new CMMSubscriptionRequestFactory();
        final String BACKUPMANAGER_ID = "DEFAULT"; // registered in Repository
        final String BACKUPMANAGEBACKUP_ID = "testbackup"; // Index backup on repository
        final int BACKUPMANAGE_INDEX = 1; // Index on repository
        final int BACKUPMANAGEBACKUP_INDEX = 2; // Index backup on repository
        
        final JsonService jsonService = new JsonService();
        configurationRequestFactory.setJsonService(jsonService);
        final Optional<BRMEricssonbrmJson> request =configurationRequestFactory.parseJsonStringToBRMConfiguration(json.toString());

        EtagNotifIdBase etagNotifIdNew = new EtagNotifIdBase();
        etagNotifIdNew.updateEtag("11111");
        etagNotifIdNew.setNotifId(1000);
        etagNotifIdNew.setConfiguration(ericssonBRMConfiguration);
        etagNotifIdNew.setBackupManagerRepository(backupManagerRepository);
        backupManagerRepository.getLastEtagfromCMM();
        expectLastCall().times(2);
        expect(backupManagerRepository.getBackupManager(EasyMock.anyInt())).andReturn(backupMgr).anyTimes();
        expect(backupManagerRepository.getBRMConfiguration(EasyMock.anyString())).andReturn(request).times(2);
        expect(backupManagerRepository.getIndex(BACKUPMANAGER_ID)).andReturn(BACKUPMANAGE_INDEX); // Id in the repository
        expect(backupManagerRepository.getBackupManager(BACKUPMANAGER_ID)).andReturn(backupMgr); // Id in the repository

        replay (backupManagerRepository);
        // Simulate a context received from YP and it was looking for BM/0 
        final int bro_Index = etagNotifIdNew.getIndexBackupManager("/ericsson-brm:brm/backup-manager/0/progress-report", "11112");
        final int broBackup_Index = etagNotifIdNew.getIndexBackupManagerBackup("/ericsson-brm:brm/backup-manager/0/backup/0/status", "11112");

        assertTrue(service.isValidEtagNotifId(etagNotifIdNew.getEtag(), etagNotifIdNew.getNotifId()));
        assertTrue(bro_Index == BACKUPMANAGE_INDEX);
        assertTrue(broBackup_Index == BACKUPMANAGEBACKUP_INDEX);
    }

    @Test
    public void getCMMIndexBackupManager_successful() throws JSONException {
        final JSONObject json = getEricssonJsonBRMConfiguration();
        final String ericssonBRMConfiguration=json.toString();
        final int BACKUPMANAGE_INDEX = 0; // Index on CMM
        final BackupManager backupMgr = mockBackupManager("DEFAULT", "a", "b", false);
        final JsonService jsonService = new JsonService();
        final CMMSubscriptionRequestFactory configurationRequestFactory = new CMMSubscriptionRequestFactory();
        configurationRequestFactory.setJsonService(jsonService);

        final Optional<BRMEricssonbrmJson> request =configurationRequestFactory.parseJsonStringToBRMConfiguration(json.toString());
        expect(backupManagerRepository.getBackupManager(EasyMock.anyInt())).andReturn(backupMgr).anyTimes();
        expect(backupManagerRepository.getBRMConfiguration(EasyMock.anyString())).andReturn(request).times(2);

        replay (backupManagerRepository);

        EtagNotifIdBase etagNotifIdNew = new EtagNotifIdBase();
        etagNotifIdNew.updateEtag("11111");
        etagNotifIdNew.setNotifId(1000);
        etagNotifIdNew.setConfiguration(ericssonBRMConfiguration);
        etagNotifIdNew.setBackupManagerRepository(backupManagerRepository);
        // From Repository BM/o returns CMM BM/Index 1, Search for DEFAULT into getEricssonJsonBRMConfiguration()
        final int broBackup_Index = etagNotifIdNew.getCMMIndexBackupManager("/ericsson-brm:brm/backup-manager/2/backup/0/status");
        assertTrue(broBackup_Index == BACKUPMANAGE_INDEX);
    }

    @Test
    public void getCMMIndexBackupManager_invalidConfiguration() throws JSONException {
        final String ericssonBRMConfiguration = "{ \"message\":\"Database restore operation ongoing\" }";
        final int BACKUPMANAGE_INDEX = -1; // Missing in CMM
        final BackupManager backupMgr = mockBackupManager("DEFAULT", "a", "b", false);
        final JsonService jsonService = new JsonService();
        final CMMSubscriptionRequestFactory configurationRequestFactory = new CMMSubscriptionRequestFactory();
        configurationRequestFactory.setJsonService(jsonService);

        final Optional<BRMEricssonbrmJson> request =configurationRequestFactory.parseJsonStringToBRMConfiguration(ericssonBRMConfiguration);
        expect(backupManagerRepository.getBackupManager(EasyMock.anyInt())).andReturn(backupMgr).anyTimes();
        expect(backupManagerRepository.getBRMConfiguration(EasyMock.anyString())).andReturn(request).times(2);

        replay (backupManagerRepository);

        EtagNotifIdBase etagNotifIdNew = new EtagNotifIdBase();
        etagNotifIdNew.updateEtag("11111");
        etagNotifIdNew.setNotifId(1000);
        etagNotifIdNew.setConfiguration(ericssonBRMConfiguration);
        etagNotifIdNew.setBackupManagerRepository(backupManagerRepository);
        // From Repository BM/o returns CMM BM/Index 1, Search for DEFAULT into getEricssonJsonBRMConfiguration()
        final int broBackup_Index = etagNotifIdNew.getCMMIndexBackupManager("/ericsson-brm:brm/backup-manager/2/backup/0/status");
        assertTrue(broBackup_Index == BACKUPMANAGE_INDEX);
    }

    @Test
    public void getCMMIndexBackupManager_failed() throws JSONException {
        final JSONObject json = getEricssonJsonBRMConfiguration();
        final String ericssonBRMConfiguration=json.toString();
        final int BACKUPMANAGE_INVALID_INDEX = -1; // Index on CMM
        final BackupManager backupMgr = mockBackupManager("NOT_IN_CMM", "a", "b", false);
        final JsonService jsonService = new JsonService();
        final CMMSubscriptionRequestFactory configurationRequestFactory = new CMMSubscriptionRequestFactory();
        configurationRequestFactory.setJsonService(jsonService);

        final Optional<BRMEricssonbrmJson> request =configurationRequestFactory.parseJsonStringToBRMConfiguration(json.toString());
        expect(backupManagerRepository.getBackupManager(EasyMock.anyInt())).andReturn(backupMgr).anyTimes();
        expect(backupManagerRepository.getBRMConfiguration(EasyMock.anyString())).andReturn(request).times(2);

        replay (backupManagerRepository);

        EtagNotifIdBase etagNotifIdNew = new EtagNotifIdBase();
        etagNotifIdNew.updateEtag("11111");
        etagNotifIdNew.setNotifId(1000);
        etagNotifIdNew.setConfiguration(ericssonBRMConfiguration);
        etagNotifIdNew.setBackupManagerRepository(backupManagerRepository);
        // From Repository BM/o returns CMM BM/Index 1, Search for DEFAULT into getEricssonJsonBRMConfiguration()
        final int broBackup_Index = etagNotifIdNew.getCMMIndexBackupManager("/ericsson-brm:brm/backup-manager/2/backup/0/status");
        assertTrue(broBackup_Index == BACKUPMANAGE_INVALID_INDEX);
    }

    @Test
    public void getCMMIndexBackupManagerBackup_successful() throws JSONException {
        // JSON content, including a backup bckup1
        final JSONObject json = getEricssonJsonBRMConfiguration();
        final String ericssonBRMConfiguration=json.toString();
        final int BACKUPMANAGE_INDEX = 0; // Index on CMM
        final int BACKUPMANAGE_BACKUP_INDEX = 0; // Index on CMM
        final int BACKUPMANAGE_REPO_INDEX = 2; // Index on CMM
        // Mock including a backup bckup1
        final BackupManager backupMgr = mockBackupManager("DEFAULT", "a", "b", false);
        final JsonService jsonService = new JsonService();
        final CMMSubscriptionRequestFactory configurationRequestFactory = new CMMSubscriptionRequestFactory();
        configurationRequestFactory.setJsonService(jsonService);

        final Optional<BRMEricssonbrmJson> request =configurationRequestFactory.parseJsonStringToBRMConfiguration(json.toString());
        expect(backupManagerRepository.getBackupManager(EasyMock.anyInt())).andReturn(backupMgr).anyTimes();
        expect(backupManagerRepository.getBRMConfiguration(EasyMock.anyString())).andReturn(request).anyTimes();

        replay (backupManagerRepository);

        EtagNotifIdBase etagNotifIdNew = new EtagNotifIdBase();
        etagNotifIdNew.updateEtag("11111");
        etagNotifIdNew.setNotifId(1000);
        etagNotifIdNew.setConfiguration(ericssonBRMConfiguration);
        etagNotifIdNew.setBackupManagerRepository(backupManagerRepository);
        // From Repository BM/o returns CMM BM/Index 1, Search for DEFAULT into getEricssonJsonBRMConfiguration()
        final int broBManager_Index = etagNotifIdNew.getCMMIndexBackupManager("/ericsson-brm:brm/backup-manager/2/backup/0/status");
        final int broBackup_Index = etagNotifIdNew.getCMMIndexBackupManagerBackup("/ericsson-brm:brm/backup-manager/2/backup/0/status", BACKUPMANAGE_INDEX);
        assertTrue(broBackup_Index == BACKUPMANAGE_INDEX);
        assertTrue(BACKUPMANAGE_BACKUP_INDEX == BACKUPMANAGE_INDEX);
    }

    @Test
   public void validEtagNotifId_differenteTag_False() {
        // service includes a default etag "11111",0
        // comparing with the same etag and notifId value
        EtagNotifIdBase etagNotifIdNew = new EtagNotifIdBase();
        etagNotifIdNew.updateEtag("11112"); // eTag different
        etagNotifIdNew.setNotifId(1000);

        assertFalse(service.isValidEtagNotifId(etagNotifIdNew.getEtag(), etagNotifIdNew.getNotifId()));
    }

    @Test
   public void updateEtagNotifId_newEtag_True() {
        EtagNotifIdBase etagNotifIdNew = new EtagNotifIdBase();
        etagNotifIdNew.updateEtag("11112"); // eTag different
        etagNotifIdNew.setNotifId(1000);
        service.updateEtagNotifId(etagNotifIdNew.getEtag(), etagNotifIdNew.getNotifId());
        etagNotifIdNew.setNotifId(1001);

        assertTrue(service.isValidEtagNotifId(etagNotifIdNew.getEtag(), etagNotifIdNew.getNotifId()));
    }

    @Test
   public void CMMediatorService_updateEtagNotifId_True() {
        CMMMessage cmmMessage = createMock(CMMMessage.class);
        EtagNotifIdBase etagNotifIdNew = new EtagNotifIdBase();
        etagNotifIdNew.updateEtag("11112"); // eTag different
        etagNotifIdNew.setNotifId(1000);
        service.updateEtagNotifId(etagNotifIdNew.getEtag(), etagNotifIdNew.getNotifId());
        etagNotifIdNew.setNotifId(1001);

        assertTrue(service.isValidEtagNotifId(etagNotifIdNew.getEtag(), etagNotifIdNew.getNotifId()));
    }

    @Test
    public void addBackupManager_cmIntegrationEnabledAndAnythingGoesWrong_triesToMovePOSTtoTop() throws Exception {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);
        final BackupManager backupManager = createMock(BackupManager.class);
        final AddBackupManagerPatch patch = createMock(AddBackupManagerPatch.class);
        
        // expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(backupManagerPatchFactory.getPatchToAddBackupManager(backupManager)).andReturn(patch).anyTimes();
        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();

        replayMocks();
        cmmClient.processMessage(cmmMessageFactory.getPatchToAddBackupManager(backupManager, RETRY_INDEFINITELY));
        cmmClient.processMessage(cmmMessageFactory.getPatchToAddBackupManager(backupManager, RETRY_INDEFINITELY));
        cmmClient.processMessage(cmmMessageFactory.getMessageToCreateConfiguration(1));
        // Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> url.hasCaptured());
        verify(restTemplate.get());

    }

    @Test
    public void validatesCMMMediatorExist_dontStartAfter5Attempts_Represents5Seconds() {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        cmmClient.setFlagEnabled(true);
        service.setInitialize(false);

        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION, String.class)).andThrow(new ResourceAccessException("")).times(5);
        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION, String.class)).andReturn("").times(1);
        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SUBSCRIPTIONS_RESOURCE_CONFIGURATION, String.class)).andReturn("").anyTimes();

        expect(restTemplate.get().getForObject(SCHEMA_URL, String.class)).andReturn("").anyTimes();
        expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
        expect(restTemplate.get().getForObject(NACM_URL, String.class)).andReturn(SEARCH_STRING_SYSTEM_ADMIN + SEARCH_STRING_SYSTEM_READ_ONLY).anyTimes();
        restTemplate.get().delete(TEST_URL_EXIST);
        expectLastCall().anyTimes();

        restTemplate.get().delete(EasyMock.capture(url));
        expectLastCall().anyTimes();
        restTemplate.get().delete(SUBSCRIPTIONS_URL);
        expectLastCall().anyTimes();
        replayMocks();
        long startTime = System.nanoTime();
        service.initCMMediator();
        long endTime = System.nanoTime();
        double durationInSeconds = (endTime - startTime) % 1_000_000_000.0;

        assertTrue(durationInSeconds >= 5);
        verify(restTemplate.get());
    }

    protected void getRequests() {
        expect(restTemplate.get().getForObject(TEST_URL_EXIST, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(NACM_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SUBSCRIPTIONS_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(TEST_URL_CONFIGURATION, String.class)).andReturn("").anyTimes();
    }

    private void initFailedEtagNotifId(final String url) {
        ResponseEntity<JsonNode> responseMock = createMock(ResponseEntity.class);
        expect(responseMock.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
        expect(responseMock.getHeaders()).andReturn(headers).anyTimes();

        expect(restTemplate.get().getForEntity(url, JsonNode.class))
        .andThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "")).anyTimes();

        replay (responseMock);
    }

    private void initEtagNotifId(final String url) {
        initEtagNotifId (url, null);
    }

    private void initEtagNotifId(final String url, final HttpStatus httpStatus) {
        ResponseEntity<JsonNode> responseMock = createMock(ResponseEntity.class);

        if (httpStatus != null) {
            expect(responseMock.getStatusCode()).andReturn(httpStatus);
            expect(responseMock.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
            expect(responseMock.getHeaders()).andReturn(headers).anyTimes();
            try {
                expect(responseMock.getBody()).andReturn(getJsonNode()).anyTimes();
            } catch (Exception e) {
                // empty catch
            }
            expect(restTemplate.get().getForEntity(url, JsonNode.class))
            .andThrow(new HttpClientErrorException(httpStatus));
            expect(restTemplate.get().getForEntity(url, JsonNode.class))
            .andReturn(responseMock);
        } else {
            expect(responseMock.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
            expect(responseMock.getHeaders()).andReturn(headers).anyTimes();
            try {
                expect(responseMock.getBody()).andReturn(getJsonNode()).anyTimes();
            } catch (Exception e) {
                // empty catch
            }
            expect(restTemplate.get().getForEntity(url, JsonNode.class))
            .andReturn(responseMock).anyTimes();
        }
        replay (responseMock);
    }

    private JsonNode getJsonNode() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{\"name\":\"ericsson-brm\",\"title\":\"ericsson-brm\",\"data\":{\"ericsson-brm:brm\":{}}}";
        return objectMapper.readTree(json);
    }

    protected void replayMocks() {
        replay(backupManagerPatchFactory, backupPatchFactory, responseMock);
        replay(progressReportPatchFactory, housekeepingPatchFactory, schedulerPatchFactory, periodicEventPatchFactory);
        replay(restTemplate.get(), schemaRequestFactory, backupManagerRepository, cmmSubscriptionRequestFactory);
    }

    private BackupManager mockBackupManager(final String id, final String domain, final String type, final boolean progressReport) {
        final Scheduler scheduler = EasyMock.createMock(Scheduler.class);
        final Version version = EasyMock.createMock(Version.class);
        final Backup backup = createMock(Backup.class);
        expect(backup.getBackupId()).andReturn("bckup1").anyTimes();

        EasyMock.expect(scheduler.getAdminState()).andReturn(LOCKED).anyTimes();
        EasyMock.expect(scheduler.getMostRecentlyCreatedAutoBackup()).andReturn("none created for mock").anyTimes();
        EasyMock.expect(scheduler.getNextScheduledTime()).andReturn("no time set for the mock").anyTimes();
        EasyMock.expect(scheduler.getScheduledBackupName()).andReturn("no backup name set for the mock").anyTimes();
        EasyMock.expect(scheduler.getAutoExport()).andReturn(DISABLED).anyTimes();
        EasyMock.expect(scheduler.getPeriodicEvents()).andReturn(new ArrayList<PeriodicEvent>()).anyTimes();
        EasyMock.expect(scheduler.getVersion()).andReturn(version).anyTimes();
        EasyMock.expect(scheduler.getAutoExportPassword()).andReturn("no password set for the mock").anyTimes();
        try {
            EasyMock.expect(scheduler.getAutoExportUri()).andReturn(new URI("sftp://localhost:9999/mock")).anyTimes();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        EasyMock.expect(backupManager.getBackup(EasyMock.anyString(), EasyMock.anyObject(Ownership.class))).andReturn(backup).anyTimes();

        EasyMock.expect(backupManager.getBackupIndex(anyString())).andReturn(2).anyTimes();

        EasyMock.expect(backupManager.getBackupManagerId()).andReturn(id).anyTimes();
        EasyMock.expect(backupManager.getBackupDomain()).andReturn(domain).anyTimes();
        EasyMock.expect(backupManager.getBackupType()).andReturn(type).anyTimes();
        EasyMock.expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(progressReport).anyTimes();
        EasyMock.expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(backupManager.getActions()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(backupManager.getHousekeeping()).andReturn(new Housekeeping(id, null)).anyTimes();
        EasyMock.expect(backupManager.getScheduler()).andReturn(scheduler).anyTimes();
        EasyMock.expect(backupManager.getSftpServers()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(backupManager.getBackup(EasyMock.anyInt())).andReturn(Optional.of(backup)).anyTimes();
        backupManager.backupManagerLevelProgressReportSetCreated();
        EasyMock.expectLastCall().anyTimes();
        EasyMock.replay(backupManager, scheduler, backup);
        return backupManager;
    }

    private BRMEricssonbrmJson getbrmEricConfiguration() {
        return getbrmEricConfiguration(true);
    }

    private BRMEricssonbrmJson getbrmEricConfiguration(boolean emptyBackups) {
        final BRMBackupManagerJson brmBackupManager = new BRMBackupManagerJson(mockBackupManager(Arrays.asList(mockAction("1","test", ActionType.EXPORT))));
        final BRMEricssonbrmJson brmEricConfiguration = EasyMock.createMock(BRMEricssonbrmJson.class);
        final BRMConfiguration brmConfiguration = EasyMock.createMock(BRMConfiguration.class);
        final BRMJson brmJson = EasyMock.createMock(BRMJson.class);
        final BRMBackupManagerJson brmBackupJson = EasyMock.createMock(BRMBackupManagerJson.class);
        expect(brmBackupJson.getBackupManagerId()).andReturn(BCKMGR_ID);
        expect(brmBackupJson.getProgressReports()).andReturn(Collections.emptyList()).anyTimes();
        expect(brmBackupJson.getBackups()).andReturn(emptyBackups ? Collections.emptyList() : brmBackupManager.getBackups()).anyTimes();
        expect(brmJson.getBackupManagers()).andReturn(Arrays.asList(brmBackupJson));
        expect(brmConfiguration.getBrm()).andReturn(brmJson);
        expect(brmEricConfiguration.getBRMConfiguration()).andReturn(brmConfiguration).anyTimes();
        replay(brmEricConfiguration, brmJson, brmConfiguration, brmBackupJson);
        return brmEricConfiguration;
    }

    private Action mockAction(final String id, final String backup, ActionType actionType) {
        final String SCHEDULED_BACKUP = "scheduled";

        final Action action = EasyMock.createMock(Action.class);
        EasyMock.expect(action.getActionId()).andReturn(id).anyTimes();
        EasyMock.expect(action.getName()).andReturn(actionType).anyTimes();
        EasyMock.expect(action.getBackupName()).andReturn(backup).anyTimes();
        EasyMock.expect(action.getBackupManagerId()).andReturn(id).anyTimes();
        EasyMock.expect(action.getAdditionalInfo()).andReturn("add").anyTimes();
        EasyMock.expect(action.getProgressInfo()).andReturn("pro").anyTimes();
        EasyMock.expect(action.getProgressPercentage()).andReturn(0.99).anyTimes();
        EasyMock.expect(action.getResult()).andReturn(ResultType.NOT_AVAILABLE).anyTimes();
        EasyMock.expect(action.getResultInfo()).andReturn("").anyTimes();
        EasyMock.expect(action.getState()).andReturn(ActionStateType.FINISHED).anyTimes();
        EasyMock.expect(action.getStartTime()).andReturn(getDateTime(1985, 1, 2, 3, 4, 5)).anyTimes();
        EasyMock.expect(action.getLastUpdateTime()).andReturn(getDateTime(1986, 1, 2, 3, 4, 5)).anyTimes();
        EasyMock.expect(action.getCompletionTime()).andReturn(getDateTime(1987, 1, 2, 3, 4, 5)).anyTimes();
        EasyMock.expect(action.getCopyOfMessages()).andReturn(Arrays.asList("")).anyTimes();
        EasyMock.expect(action.getAllMessagesAsSingleString()).andReturn("").anyTimes();
        expect(action.hasMessages()).andReturn(true).anyTimes();

        if(backup == null) {
            EasyMock.expect(action.isRestoreOrExport()).andReturn(false).anyTimes();
            EasyMock.expect(action.belongsToBackup(EasyMock.anyObject())).andReturn(false).anyTimes();
            EasyMock.expect(action.isPartOfHousekeeping()).andReturn(false).anyTimes();
            EasyMock.expect(action.isScheduledEvent()).andReturn(false).anyTimes();
        } else if(backup == SCHEDULED_BACKUP) {
            EasyMock.expect(action.isScheduledEvent()).andReturn(true).anyTimes();
            EasyMock.expect(action.isRestoreOrExport()).andReturn(false).anyTimes();
            EasyMock.expect(action.belongsToBackup(EasyMock.anyObject())).andReturn(false).anyTimes();
            EasyMock.expect(action.isPartOfHousekeeping()).andReturn(false).anyTimes();
        } else {
            EasyMock.expect(action.isRestoreOrExport()).andReturn(true).anyTimes();
            EasyMock.expect(action.belongsToBackup(backup)).andReturn(true).anyTimes();
            EasyMock.expect(action.belongsToBackup(EasyMock.anyObject())).andReturn(false).anyTimes();
            EasyMock.expect(action.isPartOfHousekeeping()).andReturn(false).anyTimes();
            EasyMock.expect(action.isScheduledEvent()).andReturn(false).anyTimes();
        }

        EasyMock.replay(action);
        return action;
    }

    private BackupManager mockBackupManager(final List<Action> actions) {
        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        EasyMock.expect(backupManager.getBackupManagerId()).andReturn("A");
        EasyMock.expect(backupManager.getBackupDomain()).andReturn("B");
        EasyMock.expect(backupManager.getBackupType()).andReturn("C");
        EasyMock.expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(Arrays.asList(mockBackup()));
        EasyMock.expect(backupManager.getActions()).andReturn(actions).anyTimes();
        EasyMock.expect(backupManager.getHousekeeping()).andReturn(new Housekeeping("A", null));
        EasyMock.expect(backupManager.getScheduler()).andReturn(new Scheduler("A", null));
        EasyMock.expect(backupManager.getSftpServers()).andReturn(new ArrayList<>());
        EasyMock.replay(backupManager);
        return backupManager;
    }

    private Backup mockBackup() {
        final Backup backup = EasyMock.createMock(Backup.class);
        EasyMock.expect(backup.getBackupId()).andReturn("D").anyTimes();
        EasyMock.expect(backup.getName()).andReturn("E").anyTimes();
        EasyMock.expect(backup.getCreationTime()).andReturn(getDateTime(1984, 1, 2, 3, 4, 5)).anyTimes();
        EasyMock.expect(backup.getCreationType()).andReturn(BackupCreationType.SCHEDULED).anyTimes();
        EasyMock.expect(backup.getStatus()).andReturn(BackupStatus.CORRUPTED).anyTimes();
        EasyMock.expect(backup.getSoftwareVersions()).andReturn(Arrays.asList()).anyTimes();
        EasyMock.expect(backup.getBackupManagerId()).andReturn("666").anyTimes();
        EasyMock.expect(backup.getUserLabel()).andReturn("").anyTimes();
        EasyMock.replay(backup);
        return backup;
    }

    private OffsetDateTime getDateTime(final int year, final int month, final int dayOfMonth, final int hour, final int minute, final int second) {
        final LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
        final ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        return OffsetDateTime.of(dateTime, offset);
    }

    protected JSONObject getEricssonJsonBRMConfiguration() throws JSONException {
        return new JSONObject()
                .put("name", "ericsson-brm")
                .put("title", "ericsson-brm")
                .put("data", getJsonBRMConfiguration());
    }

    protected JSONObject getJsonBRMConfiguration() throws JSONException {
        JSONArray brmBackupManagerArray = new JSONArray();
        JSONObject jsonHousekeeping = new JSONObject();
        JSONObject brmJson = new JSONObject();
        JSONObject brmBackupManager = new JSONObject();
        JSONArray backups = new JSONArray();
        JSONObject backup = new JSONObject();
        backup.put("id", "bckup1");
        backup.put("status", "backup-complete");
        backup.put("backup-name", "backup_1");
        backup.put("creation-time", "2024-02-27T10:07:49");
        backup.put("creation-type", "manual");
        backups.put(backup);
        JSONObject brmdata= new JSONObject();
        jsonHousekeeping.put("auto-delete", "enabled");
        jsonHousekeeping.put("max-stored-manual-backups", 1);
        brmBackupManager.put("backup-domain","");
        brmBackupManager.put("backup-type","");
        brmBackupManager.put("backup",backups);
        brmBackupManager.put("progress-report",backups);
        brmBackupManager.put("housekeeping",jsonHousekeeping);
        brmBackupManager.put("id","DEFAULT");
        brmBackupManagerArray.put(brmBackupManager);

        brmJson.put("backup-manager",brmBackupManagerArray);
        brmdata.put("ericsson-brm:brm", brmJson);
        return brmdata;
   }

    private String getSchema() {
        return "{\n"
                + "        \"name\": \"ericsson-brm\",\n"
                + "        \"title\": \"ericsson-brm\",\n"
                + "        \"jsonSchema\": {\n"
                + "                \"type\": \"object\",\n"
                + "                \"title\": \"ericsson-brm\",\n"
                + "                \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "                \"eric-adp-version\": \"1.3.0\"\n"
                + "        }\n"
                + "}" ;
    }
}
