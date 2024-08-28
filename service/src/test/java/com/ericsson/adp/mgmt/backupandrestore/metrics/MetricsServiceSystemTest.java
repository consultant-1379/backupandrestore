/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.metrics;

import com.ericsson.adp.mgmt.backupandrestore.rest.metrics.PrometheusMetric;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.METRICS_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MetricsServiceSystemTest extends SystemTest {

    @Test
    public void check_endpoint_bro_stored_backups() {
        final ResponseEntity<PrometheusMetric> stored_backups_response = restTemplate.exchange(METRICS_URL + "bro.stored.backups" , HttpMethod.GET, null, PrometheusMetric.class);
        assertEquals(HttpStatus.OK, stored_backups_response.getStatusCode());
        assertEquals("bro.stored.backups", stored_backups_response.getBody().getName());
        assertEquals("backups", stored_backups_response.getBody().getBaseUnit());
        assertTrue(stored_backups_response.getBody().getAvailableTags()
                .stream()
                .filter(t -> t.getTag().equals("backup_type"))
                .findFirst().get()
                .getValues()
                .contains("DEFAULT"));
    }

    @Test
    public void check_endpoint_http_server_requests() {
        final ResponseEntity<PrometheusMetric> http_server_requests_seconds_count = restTemplate.exchange(METRICS_URL + "bro_http.server.requests" , HttpMethod.GET, null, PrometheusMetric.class);
        assertEquals(HttpStatus.OK, http_server_requests_seconds_count.getStatusCode());
        assertEquals("bro_http.server.requests", http_server_requests_seconds_count.getBody().getName());
        assertTrue(http_server_requests_seconds_count.getBody().getAvailableTags()
                .stream()
                .filter(t -> t.getTag().equals("exception"))
                .findFirst().get()
                .getValues()
                .contains("none"));
    }

    @Test
    public void check_endpoint_bro_operations_total() {
        final ResponseEntity<PrometheusMetric> action_counts_response = restTemplate.exchange(METRICS_URL + "bro.operations.total" , HttpMethod.GET, null, PrometheusMetric.class);
        assertEquals(HttpStatus.OK, action_counts_response.getStatusCode());
        assertEquals("bro.operations.total", action_counts_response.getBody().getName());
        assertEquals("backup_type", action_counts_response.getBody().getAvailableTags().get(0).getTag());
        assertEquals("action", action_counts_response.getBody().getAvailableTags().get(1).getTag());
        assertEquals("status", action_counts_response.getBody().getAvailableTags().get(2).getTag());
    }

    @Test
    public void check_endpoint_bro_disk_usage_bytes() {
        final ResponseEntity<PrometheusMetric> disk_usage_response = restTemplate.exchange(METRICS_URL + "bro.disk.usage.bytes", HttpMethod.GET, null, PrometheusMetric.class);
        assertEquals(HttpStatus.OK, disk_usage_response.getStatusCode());
        assertEquals("bro.disk.usage.bytes", disk_usage_response.getBody().getName());
        assertEquals("bytes", disk_usage_response.getBody().getBaseUnit());
        assertTrue(disk_usage_response.getBody().getAvailableTags()
                .stream()
                .filter(t -> t.getTag().equals("backup_type"))
                .findFirst().get()
                .getValues()
                .contains("DEFAULT"));
    }

}

