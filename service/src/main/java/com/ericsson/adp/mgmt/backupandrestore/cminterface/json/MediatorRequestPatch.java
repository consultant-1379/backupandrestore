/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.NumberUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mediator Patch notification
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediatorRequestPatch {
    public static final String PERIODIC_EVENT_PATH_REGEX_ID = "(.*?)(\\d+)(/scheduler)(/periodic-event/)(-?\\d+)(/id)";
    public static final String PERIODIC_EVENT_PATH_REGEX_SKIP_ONLY_ID = "(.*?)(\\d+)(/scheduler)(/periodic-event/)(-?\\d+)(((?!/id).*)+)";
    public static final String SFTP_SERVER_PATH_REGEX = "(.*?)(\\d+)(/sftp-server)((/.*)+)";
    public static final String SFTP_SERVER_PATH_REGEX_NO_INDEX = "(.*?)(\\d+)(/sftp-server)((.*)+)";
    public static final String SFTP_SERVER_PATH_REGEX_SFTP_NAME = "(.*?)(\\d+)(/sftp-server/)(-?\\d+)(/name)";
    public static final String SFTP_SERVER_PATH_REGEX_SKIP_ONLY_NAME = "(.*?)(\\d+)(/sftp-server/)(-?\\d+)(((?!/name).*)+)";
    private static final String RELATIVE_HOST_KEY_PATH_REGEX = "(/sftp-server/\\d+/.+/ssh-host-keys/local-definition/host-key)";
    private static final String SFTP_SERVER_HOST_KEY_PATH_REGEX = "(.*?)(\\d+)" + RELATIVE_HOST_KEY_PATH_REGEX + "((/.*)+)";
    private static final String SEPARATOR = "/";
    private static final String PATH_REGEX = "(.*?)(\\d+)((/.*)+)";
    private static final String HOUSEKEEPING_PATH_REGEX = "(.*?)(\\d+)(/housekeeping)((/.*)+)";
    private static final String SCHEDULER_PATH_REGEX = "(.*?)(\\d+)(/scheduler)((/.*)+)";
    private static final String PERIODIC_EVENT_PATH_REGEX = "(.*?)(\\d+)(/scheduler)(/periodic-event)((/.*)+)";

    @JsonProperty("op")
    private String operations;
    @JsonProperty("path")
    private String path;
    @JsonProperty("value")
    private Object value;

    @JsonProperty("op")
    public String getOp() {
        return operations;
    }

    @JsonProperty("op")
    public void setOp(final String operations) {
        this.operations = operations;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("path")
    public void setPath(final String path) {
        this.path = path;
    }

    @JsonProperty("value")
    public Object getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(final Object value) {
        this.value = value;
    }

    /**
     * Recover the backup manager index from the patch path
     * -1 will be returned when the index is not provided in the path of the CMM notification patch.
     *
     * @return an index indicating the backupManager
     */
    public int getBackupManagerIndex() {
        final Matcher matcher = getPathMatcher(PATH_REGEX);
        if (matcher.matches()) {
            return Integer.valueOf(matcher.group(2));
        }
        return -1;
    }

    /**
     * Recover the periodic event index from the patch path
     * @return an index indicating the periodic event
     */
    public int getPeriodicEventIndex() {
        return getElementIndex(PERIODIC_EVENT_PATH_REGEX, 5);
    }

    /**
     * Get the sftpServerIndex from the patch
     * @return an index referencing the sftpServer
     */
    public int getSftpServerIndex() {
        return getElementIndex(SFTP_SERVER_PATH_REGEX, 4);
    }

    /**
     * Get the sftpServerHostKeyIndex from the patch
     * @return an index referencing the sftpServer host key
     */
    public int getSftpServerHostKeyIndex() {
        return getElementIndex(SFTP_SERVER_HOST_KEY_PATH_REGEX, 4);
    }

    /**
     * Parse an element index from a path
     * @param pathRegex the regex pattern used to capture the index of the element
     * @param expectedCaptureGroup the expected capture group containing the index of the element
     * @return index of the element
     */
    public int getElementIndex(final String pathRegex, final int expectedCaptureGroup) {
        final Matcher matcher = getPathMatcher(pathRegex);
        Integer elementIndex;
        if (matcher.matches()) {
            final String[] elements = matcher.group(expectedCaptureGroup).split(SEPARATOR);
            try {
                elementIndex = Integer.valueOf(elements[1]);
            } catch (final NumberFormatException e) {
                elementIndex = -1;
            }
            return elementIndex;
        }
        return -1;
    }

    /**
     * Retrieve the element to be modified
     * @return the element name to be modified
     */
    public String getUpdatedElement() {
        final Matcher housekeeping = getPathMatcher(HOUSEKEEPING_PATH_REGEX);
        final Matcher scheduler = getPathMatcher(SCHEDULER_PATH_REGEX);
        final Matcher event = getPathMatcher(PERIODIC_EVENT_PATH_REGEX);
        final Matcher sftpServer = getPathMatcher(SFTP_SERVER_PATH_REGEX);
        if (housekeeping.matches()) {
            final String[] elements = housekeeping.group(4).split(SEPARATOR);
            return elements[1];
        } else if (event.matches()) {
            final String[] elements = event.group(5).split(SEPARATOR);
            if (elements.length == 3) {
                return elements[2];
            }
            return event.group(4).substring(1);
        } else if (scheduler.matches()) {
            final String[] elements = scheduler.group(4).split(SEPARATOR);
            return elements[1];
        } else if (sftpServer.matches()) {
            final String[] elements = sftpServer.group(4).split(SEPARATOR);
            final String lastElement = elements[elements.length - 1];
            if (NumberUtils.isNumber(lastElement)) {
                return elements[elements.length - 2];
            }
            return lastElement;
        }
        return "";
    }

    private Matcher getPathMatcher(final String regex) {
        final Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(getPath());
    }

    @Override
    public String toString() {
        return "Patch [operations=" + operations + ", path=" + path + ", value=" + value + "]";
    }

}
