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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * Utility class to perform DateTime related operations.
 *
 */
public class DateTimeUtils {

    public static final String TIMESTAMP_PATTERN = "-\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[0-9A-Za-z:+-\\[\\]\\.]*\\.tar\\.gz";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd()
            .optionalStart().appendOffset("+HH", "Z").optionalEnd()
            .toFormatter();

    private DateTimeUtils() {
        throw new IllegalStateException("Instantiating DateTimeUtils Utility class");
    }

    /**
     * Format instance of OffsetDateTime to String as per Yang Date Time defined in EOI model
     *
     * @param instance
     *            OffsetDateTime object
     * @return string representation of the OffsetDateTime formatted with ISO_OFFSET_DATE_TIME
     */
    public static String convertToString(final OffsetDateTime instance) {
        return instance.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Format instance of LocalDateTime to String
     *
     * @param instance
     *            LocalDateTime object
     * @return string representation of the LocalDateTime formatted with ISO_LOCAL_DATE_TIME
     */
    public static String convertToString(final LocalDateTime instance) {
        return instance.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Convert an OffsetDateTime String to LocalDatetime String
     * @param offsetDateTime string representation of the OffsetDateTime formatted with ISO_OFFSET_DATE_TIME
     * @return string representation of the LocalDateTime formatted with ISO_LOCAL_DATE_TIME
     */
    public static String getLocalDateTime(final String offsetDateTime) {
        final LocalDateTime localDateTime = parseToOffsetDateTime(offsetDateTime).toLocalDateTime();
        return convertToString(localDateTime);
    }

    /**
     * Format a String timestamp into an OffsetDateTime in ISO_OFFSET_DATE_TIME format
     * If the timestamp does not have a timezone component, the system's default timezone will be used.
     * @param timestamp the timestamp to be parsed
     * @return string representation of the timestamp formatted with ISO_OFFSET_DATE_TIME
     */
    public static String convertToISOOffsetDateTime(final String timestamp) {
        final OffsetDateTime offsetDateTime = parseToOffsetDateTime(timestamp);
        return convertToString(offsetDateTime);
    }

    /**
     * Parse String timestamp to OffsetDateTime object
     *
     * @param timestamp
     *            String value of timestamp
     * @return OffsetDateTime object parsed from string timestamp
     */
    public static OffsetDateTime parseToOffsetDateTime(final String timestamp) {
        OffsetDateTime dateTime = null;
        try {
            dateTime = OffsetDateTime.parse(timestamp, DATE_TIME_FORMATTER);
        } catch (final DateTimeParseException e) {
            final LocalDateTime parsedDateTime = LocalDateTime.parse(timestamp);
            // Due to Daylight saving, between [01:00 27th March, 02:00 30th October] the offset is "+01:00"
            // In the rest time the offset is "Z"
            final ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(parsedDateTime);
            dateTime = OffsetDateTime.of(parsedDateTime, zoneOffset);
        }
        return dateTime;
    }

    /**
     * Parse String timestamp to LocalDateTime object
     *
     * @param timestamp
     *            String value of timestamp
     * @return LocalDateTime object parsed from string timestamp
     */
    public static LocalDateTime parseToLocalDateTime(final String timestamp) {
        return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Creates an OffsetDateTime timestamp representation from a LocalDateTime/OffsetDateTime timestamp and a new specified offset.
     * <ul>
     * <li> For LocalDateTime timestamps, the new offset is appended to the timestamps.
     * <li> For OffsetDateTime timestamps, the existing offset is ignored and replaced with the new offset.
     * </ul>
     * NOTE: Caution when removing support for OffsetDateTime timestamps of this method.
     * Removing the OffsetDateTime support will cause an upgrade failure from BRO versions [6.4.0,6.5.0,6.6.0] to a later version.
     * The OffsetDateTime support is intended to parse the periodic/calendar events that were created in these versions.
     * These versions incorrectly persisted the startTime and stopTime timestamps with an offset in addition to the timezone block.
     * @param timeStamp the LocalDateTime or OffsetDateTime string timestamp
     * @param offset the new time zone offset to be added to the timestamp
     * @return  string representation of the OffsetDateTime formatted with ISO_OFFSET_DATE_TIME
     */
    public static String offsetDateTimeFrom(final String timeStamp, final ZoneOffset offset) {
        try {
            return DateTimeUtils.convertToString(OffsetDateTime.of(DateTimeUtils.parseToLocalDateTime(timeStamp), offset));
        } catch (final DateTimeParseException e) {
            // The exception could be due to a timezone component present on the timestamp,
            // If so, try to ignore this timezone and replace it with the offset
            return changeOffset(timeStamp, offset);
        }
    }

    /**
     * Updates the offset of an OffsetDateTime timestamp
     * @param offsetDateTimeStamp the OffsetDateTime timestamp
     * @param offset the new time zone offset of the timestamp
     * @return string representation of the OffsetDateTime formatted with ISO_OFFSET_DATE_TIME
     */
    private static String changeOffset(final String offsetDateTimeStamp, final ZoneOffset offset) {
        final LocalDateTime localDateTime = OffsetDateTime.parse(offsetDateTimeStamp, DATE_TIME_FORMATTER).toLocalDateTime();
        return  DateTimeUtils.convertToString(OffsetDateTime.of(localDateTime, offset));
    }

}
