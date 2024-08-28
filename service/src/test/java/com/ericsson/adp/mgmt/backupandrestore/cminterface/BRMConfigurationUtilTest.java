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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.json.JSONException;
import org.junit.After;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.caching.Cached;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMEricssonbrmJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.NewConfigurationRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.SchemaRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddProgressReportInitialPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddProgressReportPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ConfigurationPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ProgressReportPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.UpdateProgressReportPatch;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.AssertionFailedError;

public class BRMConfigurationUtilTest {

    private static final String TEST_HOST = "http://localhost:5003";
    private static final String TEST_URL = TEST_HOST + "/cm/api/v1";
    private static final String CONFIGURATION_RESOURCE = "configurations";
    private static final String CONFIGURATION_NAME = "ericsson-brm";
    private static final String CONFIGURATION_REST = TEST_URL + "/" + CONFIGURATION_RESOURCE;
    private static final String CONFIGURATION_URL = CONFIGURATION_REST + "/" + CONFIGURATION_NAME;
    private static final String SUBSCRIPTION_NAME = "ericsson-brm";
    private static final String SUBSCRIPTION_REST = TEST_URL + "/subscriptions";
    private static final String SUBSCRIPTION_URL = SUBSCRIPTION_REST + "/" + SUBSCRIPTION_NAME;
    private static final String REST_TEMPLATE_ID = "CMM";
    private static final String SCHEMA_RESOURCE = "schemas";
    private static final String SCHEMA_URL = TEST_URL + "/" + SCHEMA_RESOURCE + "/" + CONFIGURATION_NAME;
    private static final String TEST_URL_GET_DELETE = CONFIGURATION_RESOURCE + "/" + CONFIGURATION_NAME;
    private static final String TEST_URL_EXIST = TEST_URL + "/" + TEST_URL_GET_DELETE;

    private static final HttpEntity<MultiValueMap<String, Object>> HTTP_ENTITY = new HttpEntity<>(new LinkedMultiValueMap());
    private static final SchemaRequest SCHEMA_REQUEST = new SchemaRequest(CONFIGURATION_NAME, HTTP_ENTITY);

    private BRMConfigurationUtil configurationService;
    private Cached<RestTemplate> restTemplate;
    private RestTemplateFactory restTemplateFactory;
    private CMMClient cmmClient;
    private CMMRestClient cmmRestClient;
    private CMMMessageFactory cmmMessageFactory;
    private CMMSubscriptionRequestFactory cmmSubscriptionRequestFactory;
    private BackupManagerRepository backupManagerRepository;
    private Optional<BRMEricssonbrmJson> brmEricssonJson;
    private Optional<ConfigurationRequest> configurationRequest;
    private HttpHeaders headers = new HttpHeaders();
    private EtagNotifIdBase etagNotifIdBase;

    @Before
    public void setup() {
        etagNotifIdBase = new EtagNotifIdBase();
        headers = new HttpHeaders();
        headers.add("ETag", "11111");
        final RestTemplate inner = EasyMock.createMock(RestTemplate.class);
        configurationRequest = Optional.of(getConfigurationRequest(1));
        cmmSubscriptionRequestFactory = createMock(CMMSubscriptionRequestFactory.class);
        restTemplate = new Cached<>(() -> inner);
        restTemplateFactory = EasyMock.createMock(RestTemplateFactory.class);
        expect(restTemplateFactory.getRestTemplate(eq(REST_TEMPLATE_ID), anyObject())).andReturn(restTemplate).anyTimes();
        EasyMock.replay(restTemplateFactory);
        cmmClient = new CMMClient();
        cmmRestClient = new CMMRestClient();

        cmmRestClient.setCmUrl(TEST_URL);
        cmmRestClient.setEtagNotifidBase(etagNotifIdBase);
        cmmRestClient.setRestTemplateConfiguration(restTemplateFactory, false);
        cmmClient.setCmmRestClient(cmmRestClient);
        cmmClient.setFlagEnabled(true);
        cmmClient.setInitialized(false);
        cmmClient.setEtagNotifidBase(etagNotifIdBase);

        backupManagerRepository = createMock(BackupManagerRepository.class);
        etagNotifIdBase.setBackupManagerRepository(backupManagerRepository);
        BackupManager backmgr1 = mockBackupManager("1", "a", "b");
        BackupManager backmgr2 = mockBackupManager("2", null, null);

        expect(backupManagerRepository.getBackupManagers()) .andReturn(Arrays.asList(backmgr1, backmgr2)).anyTimes();
        cmmMessageFactory = new CMMMessageFactory();
        cmmMessageFactory.setBackupManagerRepository(backupManagerRepository);
        cmmMessageFactory.setCMMSubscriptionRequestFactory(cmmSubscriptionRequestFactory);
        expect(cmmSubscriptionRequestFactory.getRequestToCreateSchema()).andReturn(SCHEMA_REQUEST).anyTimes();
        expect(cmmSubscriptionRequestFactory.parseJsonStringToBRMConfiguration(EasyMock.anyString())).andReturn(brmEricssonJson).anyTimes();
        configurationService = new BRMConfigurationUtil(cmmClient, cmmMessageFactory, etagNotifIdBase);
        configurationService.enableSubscriptionUpdate(false);
    }

    @After
    public void tearDown() {
        cmmClient.stopProcessing();
    }

    @Test
    public void createConfiguration_succeeds_createsConfigurationInCMMediator() throws InterruptedException {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        final BackupManagerRepository backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);
        expect(backupManagerRepository.getBackupManagers()) .andReturn(Arrays.asList(mockBackupManager("1", "a", "b"), mockBackupManager("2", null,
        null)));
        expect(backupManagerRepository.getBackupManager(EasyMock.anyInt())).andReturn(mockBackupManager("2", null,
                null));

        cmmMessageFactory.setBackupManagerRepository(backupManagerRepository);
        restTemplate.get().put(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject(String.class));
        expectLastCall();

        EasyMock.replay(restTemplate.get(), backupManagerRepository);
        configurationService.createConfiguration();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> url.hasCaptured());

        assertEquals(TEST_URL_EXIST, url.getValue());
        final NewConfigurationRequest body =
        (NewConfigurationRequest) request.getValue().getBody();
        assertEquals(2, body.getData().getBrm().getBackupManagers().size());
        assertEquals("1", body.getData().getBrm().getBackupManagers().get(0).getBackupManagerId());
        assertEquals("a",body.getData().getBrm().getBackupManagers().get(0).getBackupDomain());
        assertEquals("b", body.getData().getBrm().getBackupManagers().get(0).getBackupType());
        assertEquals("2", body.getData().getBrm().getBackupManagers().get(1).getBackupManagerId());
        assertEquals("", body.getData().getBrm().getBackupManagers().get(1).getBackupDomain());
        assertEquals("", body.getData().getBrm().getBackupManagers().get(1).getBackupType());
        // EasyMock.verify(restTemplate.get());
    }

  @SuppressWarnings("unchecked")
  @Test()
  public void createConfiguration_retries5times_createConfiguration() throws InterruptedException {
      final Capture<HttpEntity<?>> request = Capture.newInstance();
      final Capture<String> url = Capture.newInstance();
      final Capture<String> url2 = Capture.newInstance();

      restTemplate.get().put(eq(SUBSCRIPTION_REST), EasyMock.capture(request), EasyMock.anyObject(String.class));
      expectLastCall().anyTimes();


      // expect(backupManagerRepository.getBackupManagers()).andReturn(new ArrayList<>()).anyTimes();
      //On remedy will try to create a new config and then it will fail
      restTemplate.get().put(eq(TEST_URL_EXIST), anyObject(), anyString());
      expectLastCall().andThrow(new ResourceAccessException("Failing put configuration ")).times(5);

      // Creates the new configuration
      restTemplate.get().put(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject(String.class));
      expectLastCall();
      EasyMock.replay(restTemplate.get(), backupManagerRepository);

      cmmClient.setFlagEnabled(true);
      configurationService.createConfiguration();
      Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> url.hasCaptured());

      assertEquals(TEST_URL_EXIST, url.getValue());
      EasyMock.verify(restTemplate.get());
  }

    @Test
    public void subscriptions_subscribesConfigurationInCMMediator() throws JSONException, InterruptedException {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();

        final CMMSubscriptionRequestFactory configurationRequestFactory = createConfigurationRequest();
        final BackupManagerRepository backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);
        expect(cmmSubscriptionRequestFactory.getRequestToUpdateSchema()).andReturn(SCHEMA_REQUEST).anyTimes();

        expect(backupManagerRepository.getBackupManagers())
        .andReturn(Arrays.asList(mockBackupManager("1", "a", "b"), mockBackupManager("2", null, null)));
        cmmMessageFactory.setBackupManagerRepository(backupManagerRepository);
        cmmMessageFactory.setCMMSubscriptionRequestFactory(cmmSubscriptionRequestFactory);

        restTemplate.get().put(eq(SUBSCRIPTION_REST), EasyMock.capture(request), EasyMock.anyObject(String.class));
        expectLastCall().anyTimes();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SUBSCRIPTION_URL, String.class)).andReturn("").anyTimes();
        restTemplate.get().put(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject(String.class));
        expectLastCall();

        EasyMock.replay(restTemplate.get(), backupManagerRepository, cmmSubscriptionRequestFactory);
        configurationService.updateSubscription();

        configurationService.createConfiguration();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> url.hasCaptured());

        configurationService.subscribeToMediator();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> url.hasCaptured());
    }

    @Test
    public void subscriptions_deleteConfigurationInCMMediator_deleted() throws JSONException {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();

        final CMMSubscriptionRequestFactory configurationRequestFactory = createConfigurationRequest();

        final BackupManagerRepository backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);
        expect(backupManagerRepository.getBackupManagers())
        .andReturn(Arrays.asList(mockBackupManager("1", "a", "b"), mockBackupManager("2", null, null)));
        cmmMessageFactory.setBackupManagerRepository(backupManagerRepository);

        restTemplate.get().put(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject(String.class));
        expectLastCall();
        expect(restTemplate.get().postForObject(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SUBSCRIPTION_URL, String.class)).andReturn("").anyTimes();
        restTemplate.get().delete(SUBSCRIPTION_URL);
        expectLastCall().anyTimes();

        EasyMock.replay(restTemplate.get(), backupManagerRepository, cmmSubscriptionRequestFactory);

        configurationService.createConfiguration();
        configurationService.subscribeToMediator();
        verify(restTemplate.get());
    }

    @Test
    public void subscriptions_attempsTosubscribe_exception() throws JSONException {
        final CMMSubscriptionRequestFactory configurationRequestFactory = createConfigurationRequest();
        configurationRequestFactory.buildHttpEntity();
        restTemplate.get().put(eq(SUBSCRIPTION_REST), anyObject(), EasyMock.anyObject(String.class));
        expectLastCall().anyTimes();
        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class)).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(SUBSCRIPTION_URL, String.class)).andReturn("").anyTimes();
        expect(cmmSubscriptionRequestFactory.getRequestToUpdateSchema()).andReturn(SCHEMA_REQUEST).anyTimes();
        EasyMock.replay(restTemplate.get(), cmmSubscriptionRequestFactory);
        configurationService.updateSubscriptionToMediator();
        verify(restTemplate.get());
    }

    @Test
    public void patch_succeeds_configurationIsUpdatedInCMMediator() {
        final PatchRequest request = EasyMock.createMock(PatchRequest.class);

        final ConfigurationPatch patch = EasyMock.createMock(ConfigurationPatch.class);
        expect(patch.toJson()).andReturn(request);
        expect(restTemplate.get().patchForObject(CONFIGURATION_URL, request, String.class)).andReturn("").anyTimes();
        EasyMock.replay(restTemplate.get(), patch);

        configurationService.patch(patch);

        EasyMock.verify(restTemplate.get());
    }

    @Test
    public void progressReportPatch_0percent_succeeds_configurationIsUpdatedInCMMediator() {
        final PatchRequest request = EasyMock.createMock(PatchRequest.class);

        Action action = EasyMock.createMock(Action.class);
        expect(action.getProgressPercentage()).andReturn(0.00);

        final ProgressReportPatch patch = EasyMock.createMock(AddProgressReportPatch.class);
        expect(patch.getAction()).andReturn(action);
        expect(patch.toJson()).andReturn(request);
        expect(restTemplate.get().patchForObject(CONFIGURATION_URL, request, String.class)).andReturn("").anyTimes();
        EasyMock.replay(restTemplate.get(), patch, action);

        configurationService.patch(patch);

        EasyMock.verify(restTemplate.get(), action);
    }

    
    @Test
    public void progressReportPatch_100percent_succeeds_configurationIsUpdatedInCMMediator() {
        final PatchRequest request = EasyMock.createMock(PatchRequest.class);

        Action action = EasyMock.createMock(Action.class);
        expect(action.getProgressPercentage()).andReturn(1.0);

        final ProgressReportPatch patch = EasyMock.createMock(UpdateProgressReportPatch.class);
        expect(patch.getAction()).andReturn(action);
        expect(patch.toJson()).andReturn(request);
        expect(restTemplate.get().patchForObject(CONFIGURATION_URL, request, String.class)).andReturn("").anyTimes();
        EasyMock.replay(restTemplate.get(), patch, action);

        configurationService.patch(patch);

        EasyMock.verify(restTemplate.get(), action);
    }

    @Test
    public void patch_throwsConflictException_retrySwitchOperation_ADD_to_REPLACE() throws Exception {
        patch_throwsConflictException_retryWithPatch (PatchOperation.ADD, PatchOperation.REPLACE);
    }

    @Test
    public void patch_throwsConflictException_retrySwitchOperation_REPLACE_to_ADD() throws Exception {
        patch_throwsConflictException_retryWithPatch (PatchOperation.REPLACE, PatchOperation.ADD);
    }

    private void patch_throwsConflictException_retryWithPatch(PatchOperation from, PatchOperation to) throws Exception {
        ResponseEntity<JsonNode> responseMock = createMock(ResponseEntity.class);
        final Capture<HttpEntity<?>> requestURL = Capture.newInstance();

        expect(responseMock.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
        expect(responseMock.getHeaders()).andReturn(headers).anyTimes();
        expect(responseMock.getBody()).andReturn(getJsonNode()).anyTimes();

        final Capture<String> urlPatch = Capture.newInstance();
        final PatchRequest request = EasyMock.createMock(PatchRequest.class);
        cmmClient.setInitialized(true);
        final ProgressReportPatch patch = EasyMock.createMock(AddProgressReportPatch.class);
        Action action = EasyMock.createMock(Action.class);
        expect(action.getProgressPercentage()).andReturn(0.00).times(2);
        expect(patch.getAction()).andReturn(action).anyTimes();
        expect(patch.toJson()).andReturn(request);
        expect(restTemplate.get().getForEntity(CONFIGURATION_URL, JsonNode.class))
        .andReturn(responseMock).anyTimes();
        expect(patch.toJson()).andReturn(request).anyTimes();
        expect(patch.getOperation()).andReturn(from).anyTimes();
        expect(patch.getPath()).andReturn("").anyTimes();
        patch.setOperation(to);
        expectLastCall().anyTimes();

        patch.setEtag(EasyMock.anyObject());
        expectLastCall().anyTimes();
        patch.setPath(EasyMock.anyString());
        expectLastCall().anyTimes();
        expect(restTemplate.get().patchForObject(CONFIGURATION_URL, request, String.class))
        .andThrow(new HttpClientErrorException(HttpStatus.CONFLICT));
        expect(restTemplate.get().patchForObject(EasyMock.capture(urlPatch), EasyMock.capture(requestURL), EasyMock.anyObject())).andReturn("").anyTimes();

        EasyMock.replay(responseMock, restTemplate.get(), patch, action);

        configurationService.patch(patch);
         Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> urlPatch.hasCaptured());
        EasyMock.verify(restTemplate.get());
        assertEquals(CONFIGURATION_URL, urlPatch.getValue());
    }

    @Test
    public void patch_throwsInternalServerErrorException_noRemedyExecuted() throws Exception {
        final PatchRequest patchRequest = EasyMock.createMock(PatchRequest.class);
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        final Capture<String> urlPatch = Capture.newInstance();
        final ProgressReportPatch patch = EasyMock.createMock(AddProgressReportPatch.class);
        cmmClient.setInitialized(true);
        ResponseEntity<JsonNode> responseMock = createMock(ResponseEntity.class);
        expect(responseMock.getBody()).andReturn(getJsonNode()).anyTimes();
        expect(responseMock.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
        expect(responseMock.getHeaders()).andReturn(headers).anyTimes();
        expect(restTemplate.get().getForEntity(CONFIGURATION_URL, JsonNode.class))
        .andReturn(responseMock).anyTimes();
        Action action = EasyMock.createMock(Action.class);
        expect(action.getProgressPercentage()).andReturn(0.00).times(2);
        expect(patch.getAction()).andReturn(action).times(1);
        patch.setEtag(EasyMock.anyString());
        expectLastCall().anyTimes();
        expect(patch.getOperation()).andReturn(PatchOperation.ADD).anyTimes();
        expect(patch.toJson()).andReturn(patchRequest).anyTimes();
        expect(patch.getPath()).andReturn("").anyTimes();

        expect(restTemplate.get().patchForObject(EasyMock.capture(urlPatch), EasyMock.capture(request), EasyMock.anyObject()))
        .andThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        EasyMock.replay(restTemplate.get(), patch, action, responseMock);

        configurationService.patch(patch);
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> urlPatch.hasCaptured());

        EasyMock.verify(restTemplate.get());
    }

    @Test
    public void patch_throws409_JSONPOINTERException_OnPatch_is_Ignored() throws Exception {
        final PatchRequest patchRequest = EasyMock.createMock(PatchRequest.class);
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        final Capture<String> urlPatch = Capture.newInstance();
        final ProgressReportPatch patch = EasyMock.createMock(AddProgressReportInitialPatch.class);
        BackupManager backmgr2 = mockBackupManager("2", null, null);
        expect(backupManagerRepository.getBackupManager(EasyMock.anyInt())).andReturn(backmgr2).anyTimes();
        expect(backupManagerRepository.getBRMConfiguration(EasyMock.anyString())).andReturn(Optional.empty()).anyTimes();

        cmmClient.setInitialized(true);
        cmmClient.setFlagEnabled(true);
        ResponseEntity<JsonNode> responseMock = createMock(ResponseEntity.class);
        expect(responseMock.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
        expect(responseMock.getHeaders()).andReturn(headers).anyTimes();
        expect(responseMock.getBody()).andReturn(getJsonNode()).anyTimes();

        expect(restTemplate.get().getForEntity(CONFIGURATION_URL, JsonNode.class))
        .andReturn(responseMock).anyTimes();
        Action action = EasyMock.createMock(Action.class);
        expect(action.getProgressPercentage()).andReturn(0.00).times(2);
        expect(patch.getAction()).andReturn(action).times(1);
        patch.setEtag(EasyMock.anyString());
        expectLastCall().anyTimes();
        expect(patch.toJson()).andReturn(patchRequest).anyTimes();
        expect(patch.getPath()).andReturn("/ericsson-brm:brm/backup-manager/2/backup/1/progress-report/0").anyTimes();
        patch.setPath(EasyMock.anyString());
        expectLastCall().anyTimes();

        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class)).andReturn(getConfiguration()).anyTimes();

        expect(restTemplate.get().exchange(
                EasyMock.capture(urlPatch),
                eq(HttpMethod.PATCH),
                EasyMock.isA(HttpEntity.class),
                eq(String.class)))
        .andThrow(new HttpClientErrorException(HttpStatus.CONFLICT, "Invalid JsonPointer in JsonPatch")).atLeastOnce();

        EasyMock.replay(restTemplate.get(), patch, action, responseMock, backupManagerRepository);

        configurationService.patch(patch);
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> urlPatch.hasCaptured());

        EasyMock.verify(restTemplate.get());
    }

    @Test
    public void patch_throws409_EtagException_OnPatch_retry() throws Exception {
        final PatchRequest patchRequest = EasyMock.createMock(PatchRequest.class);
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        final Capture<String> urlPatch = Capture.newInstance();
        final ProgressReportPatch patch = EasyMock.createMock(AddProgressReportPatch.class);
        BackupManager backmgr2 = mockBackupManager("2", null, null);
        expect(backupManagerRepository.getBackupManager(EasyMock.anyInt())).andReturn(backmgr2).anyTimes();
        expect(backupManagerRepository.getBRMConfiguration(EasyMock.anyString())).andReturn(Optional.empty()).anyTimes();

        cmmClient.setInitialized(true);
        cmmClient.setFlagEnabled(true);
        ResponseEntity<JsonNode> responseMock = createMock(ResponseEntity.class);
        expect(responseMock.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
        expect(responseMock.getHeaders()).andReturn(headers).anyTimes();
        expect(responseMock.getBody()).andReturn(getJsonNode()).anyTimes();

        expect(restTemplate.get().getForEntity(CONFIGURATION_URL, JsonNode.class))
        .andReturn(responseMock).anyTimes();
        Action action = EasyMock.createMock(Action.class);
        expect(action.getProgressPercentage()).andReturn(1.0).times(2);
        expect(patch.getAction()).andReturn(action).times(2);
        patch.setEtag(EasyMock.anyString());
        expectLastCall().anyTimes();
        patch.setPath(EasyMock.anyString());
        expectLastCall().anyTimes();
        expect(patch.toJson()).andReturn(patchRequest).anyTimes();
        expect(patch.getPath()).andReturn("/ericsson-brm:brm/backup-manager/2/backup/1/progress-report/0").anyTimes();

        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class)).andReturn(getConfiguration()).anyTimes();

        expect(restTemplate.get().patchForObject(EasyMock.capture(urlPatch), EasyMock.capture(request), EasyMock.anyObject()))
        .andThrow(new HttpClientErrorException(HttpStatus.CONFLICT, "ETag value not current")).atLeastOnce();

        EasyMock.replay(restTemplate.get(), patch, action, responseMock, backupManagerRepository);

        configurationService.patch(patch);
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> urlPatch.hasCaptured());
        cmmClient.setFlagEnabled(false);

        EasyMock.verify(restTemplate.get());
    }

    @Test
    public void patch_throws409_EtagException_OnPatch_runRemedy() throws Exception {
        final PatchRequest patchRequest = EasyMock.createMock(PatchRequest.class);
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        final Capture<String> urlPatch = Capture.newInstance();
        final Capture<String> urlSubs = Capture.newInstance();
        BackupManager backmgr2 = mockBackupManager("2", null, null);
        expect(backupManagerRepository.getBackupManager(EasyMock.anyInt())).andReturn(backmgr2).anyTimes();
        expect(backupManagerRepository.getBRMConfiguration(EasyMock.anyString())).andReturn(Optional.empty()).anyTimes();
        
        final ProgressReportPatch patch = EasyMock.createMock(AddProgressReportPatch.class);
        cmmClient.setInitialized(true);
        cmmClient.setFlagEnabled(true);
        ResponseEntity<JsonNode> responseMock = createMock(ResponseEntity.class);
        expect(responseMock.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
        expect(responseMock.getHeaders()).andReturn(headers).anyTimes();
        expect(responseMock.getBody()).andReturn(getJsonNode()).anyTimes();

        expect(restTemplate.get().getForEntity(CONFIGURATION_URL, JsonNode.class))
        .andReturn(responseMock).anyTimes();
        Action action = EasyMock.createMock(Action.class);
        expect(action.getProgressPercentage()).andReturn(0.0).times(2); // NO RETRY_INDEFINITELY
        expect(patch.getAction()).andReturn(action).anyTimes();
        patch.setEtag(EasyMock.anyString());
        expectLastCall().anyTimes();
        patch.setPath(EasyMock.anyString());
        expectLastCall().anyTimes();
        expect(patch.toJson()).andReturn(patchRequest).anyTimes();
        expect(patch.getPath()).andReturn("/ericsson-brm:brm/backup-manager/2/backup/1/progress-report/0").anyTimes();
        expect(patch.getOperation()).andReturn(PatchOperation.ADD).anyTimes();
        restTemplate.get().put(EasyMock.capture(urlSubs), EasyMock.capture(request), EasyMock.anyObject(String.class));
        expectLastCall().anyTimes();
        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class)).andReturn(getConfiguration()).anyTimes();

        expect(restTemplate.get().patchForObject(EasyMock.capture(urlPatch), EasyMock.capture(request), EasyMock.anyObject()))
        .andThrow(new HttpClientErrorException(HttpStatus.CONFLICT, "ETag value not current")).atLeastOnce();

        EasyMock.replay(restTemplate.get(), patch, action, responseMock, backupManagerRepository);

        configurationService.patch(patch);
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> urlPatch.hasCaptured());
        cmmClient.setFlagEnabled(false);

        EasyMock.verify(restTemplate.get());
    }

    @Test
    public void patch_anythingGoesWrong_doesNotThrowsException() {
        final PatchRequest request = EasyMock.createMock(PatchRequest.class);

        final ConfigurationPatch patch = EasyMock.createMock(ConfigurationPatch.class);

        expect(patch.toJson()).andReturn(request).anyTimes();

        expect(restTemplate.get().patchForObject(CONFIGURATION_URL, request, String.class)).andThrow(new RuntimeException());
        EasyMock.replay(restTemplate.get(), patch);

        configurationService.patch(patch);
    }

    @Test
    public void addBackupManagersOneByOne_doesNotThrowsException() {
        final PatchRequest request = EasyMock.createMock(PatchRequest.class);

        final ConfigurationPatch patch = EasyMock.createMock(ConfigurationPatch.class);
        restTemplate.get().put(eq(TEST_URL_EXIST), anyObject(), anyString());
        expectLastCall();
        
        expect(patch.toJson()).andReturn(request).anyTimes();

        expect(restTemplate.get().patchForObject(CONFIGURATION_URL, request, String.class)).andThrow(new RuntimeException());
        EasyMock.replay(restTemplate.get(), patch);

        configurationService.addBackupManagersOneByOne(Arrays.asList(mockBackupManager("1", "a", "b"), mockBackupManager("2", null, null)));
    }

    private BackupManager mockBackupManager(final String id, final String domain, final String type) {
        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn(id).anyTimes();
        expect(backupManager.getBackupDomain()).andReturn(domain);
        expect(backupManager.getBackupType()).andReturn(type);
        expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(new ArrayList<>());
        expect(backupManager.getActions()).andReturn(new ArrayList<>()).anyTimes();
        expect(backupManager.getHousekeeping()).andReturn(new Housekeeping(id, null));
        expect(backupManager.getScheduler()).andReturn(new Scheduler(id, null));
        expect(backupManager.getSftpServers()).andReturn(new ArrayList<>());

        EasyMock.replay(backupManager);
        return backupManager;
    }

    private CMMSubscriptionRequestFactory createConfigurationRequest() {
        final CMMSubscriptionRequestFactory configurationRequestFactory = new CMMSubscriptionRequestFactory();
        configurationRequestFactory.setBroServiceName("localhost");
        configurationRequestFactory.setBroPort("5003");
        configurationRequestFactory.setLeasedSeconds(5000);
        configurationRequestFactory.setJsonService(new JsonService());
        return configurationRequestFactory;
    }

    @Test
    public void validateLeasedTime_startup_createSubscribe()
            throws JSONException, JsonMappingException, JsonProcessingException, RestClientException, InterruptedException {
        final Capture<String> url = Capture.newInstance();
        final Capture<HttpEntity<?>> request = Capture.newInstance();
        


        final CMMSubscriptionRequestFactory configurationRequestFactory = createConfigurationRequest();

        final BackupManagerRepository backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);
        expect(backupManagerRepository.getBackupManagers())
        .andReturn(Arrays.asList(mockBackupManager("1", "a", "b"), mockBackupManager("2", null, null)));

        expect(restTemplate.get().postForObject(EasyMock.capture(url),
                EasyMock.capture(request), EasyMock.anyObject())).andReturn("").anyTimes();
        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class)).andReturn("config").anyTimes();
        expect(restTemplate.get().getForObject(SUBSCRIPTION_URL, String.class)).andReturn(getConfigurationRequestString(300)).anyTimes();
        restTemplate.get().put(EasyMock.capture(url), EasyMock.capture(request), EasyMock.anyObject(String.class));
        expect(cmmSubscriptionRequestFactory.parseJsonStringToSubscriptionRequest(EasyMock.anyString())).andReturn(configurationRequest).anyTimes();
        expect(cmmSubscriptionRequestFactory.getRequestToUpdateSchema()).andReturn(SCHEMA_REQUEST).anyTimes();
        restTemplate.get().put(eq(SUBSCRIPTION_REST), EasyMock.capture(request), EasyMock.anyObject(String.class));
        expectLastCall().anyTimes();

        initEtagNotifId();
        cmmMessageFactory.setBackupManagerRepository(backupManagerRepository);
        EasyMock.replay(restTemplate.get(), backupManagerRepository, cmmSubscriptionRequestFactory);

        cmmClient.setFlagEnabled(true);
        cmmClient.setInitialized(true);
        configurationService.enableSubscriptionUpdate(true);
        configurationService.createConfiguration();
        configurationService.subscribeToMediator();
        configurationService.validateLeasedTime();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> url.hasCaptured());
    }

    @Test
    public void validateLeasedTime_initialLeased_validateleasedTime_update() throws JSONException, JsonMappingException, JsonProcessingException, InterruptedException {
        final Capture<String> urlSubs = Capture.newInstance();

        final Capture<HttpEntity<?>> request = Capture.newInstance();

        final CMMSubscriptionRequestFactory configurationRequestFactory = createConfigurationRequest();

        final BackupManagerRepository backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);
        expect(backupManagerRepository.getBackupManagers())
        .andReturn(Arrays.asList(mockBackupManager("1", "a", "b"), mockBackupManager("2", null, null)));
        cmmMessageFactory.setBackupManagerRepository(backupManagerRepository);

        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class)).andReturn("config").anyTimes();
        expect(restTemplate.get().getForObject(SUBSCRIPTION_URL, String.class)).andReturn(getConfigurationRequestString(5000)).anyTimes();
        expect(cmmSubscriptionRequestFactory.getRequestToUpdateSchema()).andReturn(SCHEMA_REQUEST).anyTimes();

        restTemplate.get().put(EasyMock.capture(urlSubs), EasyMock.capture(request), EasyMock.anyObject(String.class));
        expectLastCall().times(2);
        expect(cmmSubscriptionRequestFactory.parseJsonStringToSubscriptionRequest(EasyMock.anyString())).andReturn(configurationRequest).anyTimes();
        initEtagNotifId();
        EasyMock.replay(restTemplate.get(), backupManagerRepository, cmmSubscriptionRequestFactory);

        cmmClient.setFlagEnabled(true);
        cmmClient.setInitialized(true);
        configurationService.enableSubscriptionUpdate(true);
        configurationService.initializeCMMStatus(true);
        configurationService.createConfiguration();
        configurationService.validateLeasedTime();

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> urlSubs.hasCaptured());
        assertTrue(SUBSCRIPTION_REST.equalsIgnoreCase(urlSubs.getValue()));
    }

    @Test
    public void validateLeasedTime_InmmediateReturn_InitializeCMMStatusAsFalse_NoAssertionError() {
        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class))
        .andThrow(new AssertionFailedError()).anyTimes();

        configurationService.enableSubscriptionUpdate(true);
        cmmClient.setFlagEnabled(true);
        configurationService.initializeCMMStatus(false);
        EasyMock.replay(restTemplate.get());
        configurationService.validateLeasedTime();
        EasyMock.verify(restTemplate.get());
    }

    @Test
    public void validateLeasedTime_InmmediateReturn_FlagEnableAsFalse_NoAssertionError() {
        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class))
        .andThrow(new AssertionFailedError()).anyTimes();
        configurationService.enableSubscriptionUpdate(true);
        cmmClient.setFlagEnabled(false);
        initEtagNotifId();
        EasyMock.replay(restTemplate.get());
        configurationService.initializeCMMStatus(true);
        configurationService.validateLeasedTime();
        EasyMock.verify(restTemplate.get());
    }

    @Test
    public void validateLeasedTime_InmmediateReturn_enableSubscriptionasFalse_NoAssertionError() {
        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class))
        .andThrow(new AssertionFailedError()).anyTimes();
        configurationService.enableSubscriptionUpdate(false);
        cmmClient.setFlagEnabled(true);
        initEtagNotifId();
        EasyMock.replay(restTemplate.get());
        configurationService.initializeCMMStatus(true);
        configurationService.validateLeasedTime();
        EasyMock.verify(restTemplate.get());
    }

    @Test
    public void validateLeasedTime_ConfigurationDoesntExist_enableSubscriptionas_Warning() {
        expect(restTemplate.get().getForObject(CONFIGURATION_URL, String.class)).
        andThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not found", null, null, null));
        configurationService.enableSubscriptionUpdate(true);
        cmmClient.setFlagEnabled(true);
        initEtagNotifId();
        EasyMock.replay(restTemplate.get());
        configurationService.initializeCMMStatus(true);
        configurationService.validateLeasedTime();
        EasyMock.verify(restTemplate.get());
    }

    @Test
    public void CMService_doesnt_reveal_password() {
        String result = CMMRestClient.hidePassword("Malformed Content: 'somePassword' is not a 'eric-adp-cm-secret'");
        assertEquals("Malformed Content: 'supplied information' is not a 'eric-adp-cm-secret'",result);
        result = CMMRestClient.hidePassword("Sample text");
        assertEquals("Sample text", result);
    }

    private JsonNode getJsonNode() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{\"name\":\"ericsson-brm\",\"title\":\"ericsson-brm\",\"data\":{\"ericsson-brm:brm\":{}}}";
        return objectMapper.readTree(json);
    }

    private String getConfigurationRequestString(final long leasedTime) {
        final JsonService jsonService = new JsonService();
        return jsonService.toJsonString(getConfigurationRequest(leasedTime));
    }

    private ConfigurationRequest getConfigurationRequest(final long leasedTime) {
        final String[] events = { "configUpdated" };
        final ConfigurationRequest configurationRequest = new ConfigurationRequest(SUBSCRIPTION_NAME, events, CONFIGURATION_URL);
        configurationRequest.setLeaseSeconds(leasedTime);
        configurationRequest.setUpdateNotificationFormat("patch");
        configurationRequest.setConfigName(SUBSCRIPTION_NAME);
        return configurationRequest;
    }

    private void initEtagNotifId() {
        ResponseEntity<JsonNode> responseMock = createMock(ResponseEntity.class);
        expect(responseMock.getStatusCode()).andReturn(HttpStatus.OK).anyTimes();
        expect(responseMock.getHeaders()).andReturn(headers).anyTimes();
        try {
            expect(responseMock.getBody()).andReturn(getJsonNode()).anyTimes();
        } catch (Exception e) {
            // nothing to do
        }

        expect(restTemplate.get().getForEntity(CONFIGURATION_URL, JsonNode.class))
        .andReturn(responseMock).anyTimes();
        EasyMock.replay (responseMock);

    }

    private String getConfiguration() {
        return "{\"name\":\"ericsson-brm\",\"title\":\"ericsson-brm\",\"data\":{\"ericsson-brm:brm\":{\"backup-manager\":"
                + "[{\"id\":\"DEFAULT\",\"backup\":[{\"id\":\"for-restore\",\"status\":\"backup-complete\",\"sw-version\":[],"
                + "\"backup-name\":\"for-restore\",\"creation-time\":\"2023-11-15T17:20:16.814537Z\",\"creation-type\":\"manual\","
                + "\"progress-report\":[]}],\"scheduler\":{\"admin-state\":\"unlocked\",\"auto-export\":\"disabled\",\"periodic-event\":[],"
                + "\"progress-report\":[],\"scheduled-backup-name\":\"SCHEDULED_BACKUP\"},\"backup-type\":\"\",\"sftp-server\":[],"
                + "\"housekeeping\":{\"auto-delete\":\"enabled\",\"max-stored-manual-backups\":3},\"backup-domain\":\"\","
                + "\"progress-report\":[{\"state\":\"finished\",\"result\":\"success\",\"action-id\":61355,\"action-name\":\"CREATE_BACKUP\","
                + "\"result-info\":\"{Agent: eric-}\",\"additional-info\":[],\"progress-percentage\":100,\"time-action-started\":\"2023-11-15T17:20:15.526937Z\","
                + "\"time-action-completed\":\"2023-11-15T17:20:28.90842Z\",\"time-of-last-status-update\":\"2023-11-15T17:20:28.90842Z\"}]}]}}}" ;
    }
}
