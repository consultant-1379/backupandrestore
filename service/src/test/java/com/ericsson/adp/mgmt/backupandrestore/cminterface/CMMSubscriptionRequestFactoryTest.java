/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.URLMAPPING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.SchemaRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMEricssonbrmJson;

public class CMMSubscriptionRequestFactoryTest {

    private static final String TEST_HOSTNAME = "localhost";
    private static final String TEST_PORT = "7001";
    private static final String TLS_PORT = "7002";
    private static final String TLS_CMM_NOTIF_PORT = "7004";
    private static final String TEST_URL = "http://" + TEST_HOSTNAME + ":" + TEST_PORT;
    private static final String TLS_CMM_NOTIF = "https://" + TEST_HOSTNAME + ":" + TLS_CMM_NOTIF_PORT;
    private static final String V3_CONFIGHANDLER = "/v3/" + URLMAPPING;
    private static final long SUBSCRIPTION_LEASED_SECONDS = 5000;

    @Test
    public void getRequestToSubscribe_tlsEnabled_requestWithJsonContent() throws JSONException {
        final CMMSubscriptionRequestFactory configurationRequestFactory = new CMMSubscriptionRequestFactory();
        final JsonService jsonService = new JsonService();
        configurationRequestFactory.setGlobalTlsEnabled(true);
        configurationRequestFactory.setBroServiceName(TEST_HOSTNAME);
        configurationRequestFactory.setBroPort(TEST_PORT);
        configurationRequestFactory.setTlsPort(TLS_PORT);
        configurationRequestFactory.setTlsCMMNotifPort(TLS_CMM_NOTIF_PORT);
        configurationRequestFactory.setLeasedSeconds(SUBSCRIPTION_LEASED_SECONDS);
        configurationRequestFactory.setJsonService(jsonService);
        final SchemaRequest request = configurationRequestFactory.getRequestToCreateSchema();
        assertEquals(ApplicationConstantsUtils.SCHEMA_NAME, request.getName());

        final HttpEntity<MultiValueMap<String, Object>> httpEntity = request.getHttpEntity();

        assertEquals(MediaType.APPLICATION_JSON, httpEntity.getHeaders().getContentType());
        final StringBuilder builder = new StringBuilder();
        builder.append(httpEntity.getBody());
        final String content=builder.toString();
        final JSONObject configRequestJson = new JSONObject(content);
        assertEquals(ApplicationConstantsUtils.SCHEMA_NAME, configRequestJson.get("configName"));
        assertEquals(ApplicationConstantsUtils.SCHEMA_NAME, configRequestJson.get("id"));
        assertEquals("patch", configRequestJson.get("updateNotificationFormat"));
        assertEquals(SUBSCRIPTION_LEASED_SECONDS, configRequestJson.getLong("leaseSeconds"));
        assertEquals(TLS_CMM_NOTIF + V3_CONFIGHANDLER, configRequestJson.get("callback"));
        assertEquals("configUpdated", ((JSONArray) configRequestJson.get("event")).get(0));
    }

    @Test
    public void getRequestToSubscribe_tlsDisabled_requestWithJsonContent() throws JSONException {
        final CMMSubscriptionRequestFactory configurationRequestFactory = new CMMSubscriptionRequestFactory();
        final JsonService jsonService = new JsonService();
        configurationRequestFactory.setGlobalTlsEnabled(false);
        configurationRequestFactory.setBroServiceName(TEST_HOSTNAME);
        configurationRequestFactory.setBroPort(TEST_PORT);
        configurationRequestFactory.setTlsPort(TLS_PORT);
        configurationRequestFactory.setTlsCMMNotifPort(TLS_CMM_NOTIF_PORT);
        configurationRequestFactory.setLeasedSeconds(SUBSCRIPTION_LEASED_SECONDS);
        configurationRequestFactory.setJsonService(jsonService);
        final SchemaRequest request = configurationRequestFactory.getRequestToCreateSchema();
        assertEquals(ApplicationConstantsUtils.SCHEMA_NAME, request.getName());

        final HttpEntity<MultiValueMap<String, Object>> httpEntity = request.getHttpEntity();

        assertEquals(MediaType.APPLICATION_JSON, httpEntity.getHeaders().getContentType());
        final StringBuilder builder = new StringBuilder();
        builder.append(httpEntity.getBody());
        final String content=builder.toString();
        final JSONObject configRequestJson = new JSONObject(content);
        assertEquals(ApplicationConstantsUtils.SCHEMA_NAME, configRequestJson.get("configName"));
        assertEquals(ApplicationConstantsUtils.SCHEMA_NAME, configRequestJson.get("id"));
        assertEquals("patch", configRequestJson.get("updateNotificationFormat"));
        assertEquals(SUBSCRIPTION_LEASED_SECONDS, configRequestJson.getLong("leaseSeconds"));
        assertEquals(TEST_URL + V3_CONFIGHANDLER, configRequestJson.get("callback"));
        assertEquals("configUpdated", ((JSONArray) configRequestJson.get("event")).get(0));
    }

    @Test
    public void validateRequest() {
        final String mediatorResponse="{\"id\": \"ericsson-brm\", \"configName\": \"ericsson-brm\", \"event\": [\"configUpdated\"], \"updateNotificationFormat\": \"patch\", \"callback\": \"http://eric-ctrl-bro:7001/v3/confighandler\", \"leaseSeconds\": 9999}";
        final CMMSubscriptionRequestFactory configurationRequestFactory = new CMMSubscriptionRequestFactory();
        final JsonService jsonService = new JsonService();
        configurationRequestFactory.setJsonService(jsonService);
        final Optional<ConfigurationRequest> request =configurationRequestFactory.parseJsonStringToSubscriptionRequest(mediatorResponse);
        assertTrue(request.isPresent());
        assertEquals("ericsson-brm", request.get().getIdSubscription());
        assertEquals("ericsson-brm", request.get().getConfigName());
        assertEquals(9999, request.get().getLeaseSeconds());
    }

    @Test
    public void parseJsonStringToBRMConfiguration_fromJson() throws JSONException {
        final String ericssonBRMConfiguration=getEricssonJsonBRMConfiguration().toString();
        final CMMSubscriptionRequestFactory configurationRequestFactory = new CMMSubscriptionRequestFactory();
        final JsonService jsonService = new JsonService();
        configurationRequestFactory.setJsonService(jsonService);
        final Optional<BRMEricssonbrmJson> request =configurationRequestFactory.parseJsonStringToBRMConfiguration(ericssonBRMConfiguration);
        assertTrue(request.isPresent());
        assertEquals("ericsson-brm", request.get().getTitle());
        assertEquals("ericsson-brm", request.get().getName());
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
        JSONObject brmdata= new JSONObject();
        jsonHousekeeping.put("auto-delete", "enabled");
        jsonHousekeeping.put("max-stored-manual-backups", 1);
        brmBackupManager.put("backup-domain","");
        brmBackupManager.put("backup-type","");
        brmBackupManager.put("backup",new JSONArray());
        brmBackupManager.put("progress-report",new JSONArray());
        brmBackupManager.put("housekeeping",jsonHousekeeping);
        brmBackupManager.put("id","DEFAULT");
        brmBackupManagerArray.put(brmBackupManager);

        brmJson.put("backup-manager",brmBackupManagerArray);
        brmdata.put("ericsson-brm:brm", brmJson);
        return brmdata;
   }
}
