/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/

package com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

import org.springframework.http.HttpMethod;

/**
 * A class representing hypertext control
 */
public class HypertextControl {
    private String href;
    @JsonSerialize(using = HttpMethodSerializer.class)
    private HttpMethod method;

    /**
     * Default constructor, to be used by Jackson.
     */
    public HypertextControl() {
    }

    /**
     * Constructor used to build a hypertext control
     * @param href the relative URI to the resource
     * @param method the HTTP method
     */
    public HypertextControl(final String href, final HttpMethod method) {
        this.href = href;
        this.method = method;
    }

    /**
     * get reference
     * @return string href
     */
    public String getHref() {
        return href;
    }

    /**
     * get Method
     * @return HttpMethod returned
     */
    public HttpMethod getMethod() {
        return method;
    }
    // Internal serialized class
    private static class HttpMethodSerializer extends StdSerializer<HttpMethod> {
        private static final long serialVersionUID = 1L;

        // Default constructor
        HttpMethodSerializer() {
            super(HttpMethod.class);
        }

        @Override
        public void serialize(final HttpMethod value, final JsonGenerator gen,
                              final SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }
}
