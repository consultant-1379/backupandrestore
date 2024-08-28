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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.ScheduledEvent;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventRequestOrResponse;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents a Periodic Event
 */

public class PeriodicEvent extends ScheduledEvent implements Versioned<PeriodicEvent> {
    private static final Logger log = LogManager.getLogger(PeriodicEvent.class);

    //Limits specified in yang model
    private static final Integer MIN_NUM = 0;
    private static final Integer MAX_NUM = 65535;
    private static final String LOG_MESSAGE = "Invalid value specified for %s.";
    private static final IntPredicate validUInt16 = maxbackups -> (MIN_NUM <= maxbackups) && (maxbackups <= MAX_NUM);
    @JsonIgnore
    private Consumer<PeriodicEvent> persistFunction;

    @JsonIgnore
    private final JsonService localJsonService = new JsonService();

    private int days;
    private int hours;
    private int minutes;
    private int weeks;

    @JsonIgnore
    private Version<PeriodicEvent> version;

    /**
     * Constructor, needed when reading the json file to object
     */
    public PeriodicEvent() {
    }

    /**
     * Represents a Periodic Event
     * @param backupManagerId of the backupManager whose backup needs to be scheduled
     * @param persistFunction to persist periodic event
     */
    public PeriodicEvent(final String backupManagerId, final Consumer<PeriodicEvent> persistFunction) {
        setBackupManagerId(backupManagerId);
        this.persistFunction = persistFunction;
    }



    public int getDays() {
        return days;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getWeeks() {
        return weeks;
    }

    public void setDays(final int days) {
        this.days = days;
    }

    public void setHours(final int hours) {
        this.hours = hours;
    }

    public void setMinutes(final int minutes) {
        this.minutes = minutes;
    }

    public void setWeeks(final int weeks) {
        this.weeks = weeks;
    }

    /**
     * returns json representation of periodic event.
     * @return Response.
     */
    public PeriodicEventRequestOrResponse toResponse() {
        final PeriodicEventRequestOrResponse event = new PeriodicEventRequestOrResponse();
        validateAndSet(getEventId(), event::setEventId);
        validateAndSet(hours, event::setHours);
        validateAndSet(minutes, event::setMinutes);
        validateAndSet(days, event::setDays);
        validateAndSet(weeks, event::setWeeks);

        if (startTime != null) {
            validateAndSet(startTime, event::setStartTime);
        }

        if (stopTime != null) {
            validateAndSet(stopTime, event::setStopTime);
        }

        return event;
    }

    /**
     * Merge with default behavior of setting any null numeric values to 0
     * @param other - the request carrying the values to be merged
     * */
    public void mergeWith(final PeriodicEventRequestOrResponse other) {
        mergeWith(other, true);
    }

    /**
     * Merge the information in the PeriodicEventRequestOrResponse with this event, swapping
     * this events member variables with the requests member variables in all cases where
     * the request includes a non-null value. Validity testing of the values being merged
     * (minimum period, end time comes after start time etc) are the responsibility of the
     * caller
     * @param other - the request carrying the values to be merged
     * @param defaultOverwrite - if true, null numeric values in "other" will be treated as "0"
     * */
    public synchronized void mergeWith(final PeriodicEventRequestOrResponse other, final boolean defaultOverwrite) {
        validateAndSet(other.getHours(), this::setHours, "hours", defaultOverwrite);
        validateAndSet(other.getMinutes(), this::setMinutes, "minutes", defaultOverwrite);
        validateAndSet(other.getDays(), this::setDays, "days", defaultOverwrite);
        validateAndSet(other.getWeeks(), this::setWeeks, "weeks", defaultOverwrite);
        validateAndSet(other.getStartTime(), this::setStartTime, "start time");
        validateAndSet(other.getStopTime(), this::setStopTime, "end time");
    }

    private void validateAndSet(final Integer value, final IntConsumer setter, final String message, final boolean useDefault) {
        if (value != null) {
            if (validUInt16.test(value)) {
                setter.accept(value);
            } else {
                log.error(String.format(LOG_MESSAGE, message));
                throw new UnprocessableEntityException(String.format(LOG_MESSAGE, message));
            }
        } else if (useDefault) {
            setter.accept(0);
        }
    }

    private void validateAndSet(final String value, final Consumer<String> setter, final String message) {
        if (value != null) {
            try {
                if (value.isEmpty() && message.contains("end time")) {
                    setter.accept(null);
                } else {
                    setter.accept(value);
                }
            } catch (final Exception e) {
                log.error(String.format(LOG_MESSAGE, message));
                throw new UnprocessableEntityException(String.format(LOG_MESSAGE, message));
            }
        }
    }

    /**
     * Converts the PeriodicEvent object to string
     * This methods removes the timezone info in the startTime and stopTime JSON fields
     * The timezone information is stored in a separate block object.
     * This block is then appended to the PeriodicEvent's JSON object.
     * @return the JSON string to be stored in its json file
     */
    @JsonIgnore
    @Override
    public String toJson() {
        final Optional<PeriodicEvent> deepCopy = localJsonService.parseJsonString(JsonService.toJsonString(this), PeriodicEvent.class);
        if (deepCopy.isPresent()) {
            return toJson(deepCopy.get());
        }
        return "";
    }

    /**
     * Persist periodic event
     */
    public void persist() {
        persistFunction.accept(this);
    }

    /**
     * Return the period of this event in seconds
     * @return event period, in seconds
     * */
    public long periodInSeconds() {
        return Duration.ofDays(weeks * 7L)
                .plus(Duration.ofDays(days))
                .plus(Duration.ofHours(hours))
                .plus(Duration.ofMinutes(minutes)).getSeconds();
    }

    private <T> void validateAndSet(final T value, final Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    @Override
    @JsonIgnore
    public Version<PeriodicEvent> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<PeriodicEvent> version) {
        this.version = version;
    }

    public void setPersistFunction(final Consumer<PeriodicEvent> persistFunction) {
        this.persistFunction = persistFunction;
    }
}
