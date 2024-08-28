package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import static org.junit.Assert.assertEquals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class BRMEricssonbrmJsonTest {

    @Test
    void new_BRMEricssonbrmJson_FromJsonString() throws JsonMappingException, JsonProcessingException, JSONException {
        final BRMEricssonbrmJson brmEricConfiguration = new ObjectMapper().readValue(getEricssonJsonBRMConfiguration().toString(),BRMEricssonbrmJson.class);
        assertEquals("ericsson-brm",brmEricConfiguration.getName());
        assertEquals("ericsson-brm",brmEricConfiguration.getTitle());
        assertEquals("DEFAULT", brmEricConfiguration.getBRMConfiguration().getBrm().getBackupManagers()
                .get(0).getBackupManagerId());
        assertEquals(null, brmEricConfiguration.getBRMConfiguration().getBrm().getBackupManagers()
                .get(0).getProgressReports());
    }

    @Test
    void new_BRMEricssonbrmJson_newInstance() throws JsonMappingException, JsonProcessingException, JSONException {
        BRMConfiguration brmConfiguration = new ObjectMapper().readValue(getJsonBRMConfiguration().toString(),BRMConfiguration.class);
        final BRMEricssonbrmJson brmEricConfiguration = new BRMEricssonbrmJson("ericsson-brm","ericsson-brm",brmConfiguration);
        assertEquals("ericsson-brm",brmEricConfiguration.getName());
        assertEquals("ericsson-brm",brmEricConfiguration.getTitle());
        assertEquals(brmConfiguration,brmEricConfiguration.getBRMConfiguration());
        assertEquals("DEFAULT", brmEricConfiguration.getBRMConfiguration().getBrm().getBackupManagers()
                .get(0).getBackupManagerId());
    }

    @Test
    void new_BRMEricssonbrmJson_newEmptyInstance() throws JsonMappingException, JsonProcessingException, JSONException {
        BRMConfiguration brmConfiguration = new ObjectMapper().readValue(getJsonBRMConfiguration().toString(),BRMConfiguration.class);
        final BRMEricssonbrmJson brmEricConfiguration = new BRMEricssonbrmJson();
        brmEricConfiguration.setName("ericsson-brm");
        brmEricConfiguration.setTitle("ericsson-brm");
        brmEricConfiguration.setData(brmConfiguration);
        assertEquals("ericsson-brm",brmEricConfiguration.getName());
        assertEquals("ericsson-brm",brmEricConfiguration.getTitle());
        assertEquals("DEFAULT", brmEricConfiguration.getBRMConfiguration().getBrm().getBackupManagers()
                .get(0).getBackupManagerId());
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
