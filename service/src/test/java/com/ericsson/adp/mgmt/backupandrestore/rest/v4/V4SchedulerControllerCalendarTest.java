/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.v4;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerFileService;
import com.ericsson.adp.mgmt.backupandrestore.rest.error.ErrorResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4CalendarEventRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4CalendarEventsResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;
import org.junit.Test;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.Collection;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_SCHEDULER_CALENDAR;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_BASE_URL;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class V4SchedulerControllerCalendarTest extends SystemTest {

    final String V4_SCHEDULER_URL = "backup-managers/backupManagerWithSchedulerInfo/configuration/scheduling";

    @Autowired
    private SchedulerFileService schedulerFileService;

    private enum Type {
        INVALID,
        VALID
    }

    /* construct invalid and valid datetime values collection
       currently, there is no way to construct multiple collections in one class.
       So use enum Type to separate two types of datetime.
       Values order : { Type of data, Time, Month, dayOfMonth, startTime, stopTime} */
    @Parameters
    public static Collection<Object[]> prepareDatetime() {
        return Arrays.asList(new Object[][] {
                // invalid startTime and stopTime
                { Type.INVALID, "19:10:00", 11, 11, "2022-05-22T08:00:00Z", "1022-05-22T08:00:00Z" },
                // invalid time
                { Type.INVALID, "26:65:65", 11, 11, "2022-05-22T08:00:00Z", "3022-05-22T08:00:00Z" },
                // invalid time
                { Type.INVALID, "", 11, 11, "2022-05-22T08:00:00Z", "3022-05-22T08:00:00Z" },
                // invalid time
                { Type.INVALID, "16:00", 11, 11, "2022-05-22T08:00:00Z", "3022-05-22T08:00:00Z" },
                // invalid time
                { Type.INVALID, "09:61:00Z", 11, 11, "2022-05-22T08:00:00Z", "3022-05-22T08:00:00Z" },
                // invalid stopTime
                { Type.INVALID, "19:10:00", 11, 11, "2022-05-22T08:00:00Z", "3022-15-32T08:00:00Z" },
                // invalid stopTime
                { Type.INVALID, "19:10:00", 11, 11, "2022-05-22T08:00:00Z", "3022-11-30T08:00:00Z+01:00" },
                // invalid stopTime (invalid leap year)
                { Type.INVALID, "19:10:00", 11, 11, "2022-05-22T08:00:00Z", "2025-02-29T28:00:00Z" },
                // invalid stopTime
                { Type.INVALID, "19:10:00", 11, 11, "2022-05-22T08:00:00Z", "2022-02-29T" },
                // invalid stopTime (earlier than start time)
                { Type.INVALID, "19:10:00", 11, 11, "2022-05-22T08:00:00Z", "2022-01-22T08:00:00Z" },
                // invalid stopTime (earlier than current time)
                { Type.INVALID, "19:10:00", 11, 11, "1990-05-22T08:00:00Z", "1990-06-22T08:00:00Z" },
                // invalid startTime
                { Type.INVALID, "19:10:00", 11, 11, "2022-15-32T08:00:00Z", "3022-05-22T08:00:00Z" },
                // invalid startTime
                { Type.INVALID, "19:10:00", 11, 11, "2022-11-22T28:61:00Z", "3022-05-22T08:00:00Z" },
                // invalid startTime
                { Type.INVALID, "19:10:00", 11, 11, "2022-11-22T18:00:00Z+01:00", "3022-05-22T08:00:00Z" },
                // invalid startTime
                { Type.INVALID, "19:10:00", 11, 11, "2022-11-22", "3022-05-22T08:00:00Z" },
                // invalid dayOfMonth
                { Type.INVALID, "19:10:00", 11, 32, "2022-05-22T08:00:00Z", "3022-05-22T08:00:00Z" },
                // invalid Month
                { Type.INVALID, "19:10:00", 13, 11, "2022-05-22T08:00:00Z", "3022-05-22T08:00:00Z" },
                // valid datetime
                { Type.VALID, "19:10:00", 11, 11, "2022-05-22T08:00:00Z", "3022-05-22T08:00:00Z" },
                // valid datetime (leap year)
                { Type.VALID, "19:10:00", 11, 11, "2016-02-29T08:00:00Z", "2028-02-29T08:00:00Z" },
                // valid datetime (without timezone)
                { Type.VALID, "19:10:00", 11, 11, "2022-05-22T08:00:00", "3022-05-22T08:00:00" }
        });
    }

    @Parameter // first data value (0) is default
    public /* NOT private */ Type inputType;

    @Parameter(1)
    public /* NOT private */ String inputTime;

    @Parameter(2)
    public /* NOT private */ int inputMonth;

    @Parameter(3)
    public /* NOT private */ int inputDayOfMonth;

    @Parameter(4)
    public /* NOT private */ String inputStartTime;

    @Parameter(5)
    public /* NOT private */ String inputStopTime;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();

    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Before
    public void setup() throws Exception {
        /* start spring boot application here as
          @RunWith(SpringRunner.class) of class IntegrationTest
          was replaced with @RunWith(Parameterized.class).
          Multiple @RunWith is not allowed. */
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void postCalenderEvent_request_validDatetimeCollection() {
        // select valid type datetime
        Assume.assumeTrue(inputType == Type.VALID);

        // read data from @Parameters prepareDatetime()
        final V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setDayOfMonth(inputDayOfMonth);
        request.setTime(inputTime);
        request.setMonth(inputMonth);
        request.setStartTime(inputStartTime);
        request.setStopTime(inputStopTime);
        HttpEntity<V4CalendarEventRequest> entity = new HttpEntity<>(request);
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    }

    @Test
    public void postCalenderEvent_request_invalidDatetimeCollection_rejectEvent() {
        // select invalid type datetime
        Assume.assumeTrue(inputType == Type.INVALID);

        // read data from @Parameters prepareDatetime()
        final V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setTime(inputTime);
        request.setMonth(inputMonth);
        request.setDayOfMonth(inputDayOfMonth);
        request.setStartTime(inputStartTime);
        request.setStopTime(inputStopTime);
        HttpEntity<V4CalendarEventRequest> entity = new HttpEntity<>(request);
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }
}

