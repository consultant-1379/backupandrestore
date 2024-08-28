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
package com.ericsson.adp.mgmt.backupandrestore.rest;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * Disables content type sniffing on endpoints it is registered to.
 */
public class DisableMimeSniffingFilter implements Filter {
    public static final String NOSNIFF_HEADER_KEY = "X-Content-Type-Options";
    public static final String NOSNIFF_HEADER_VALUE = "nosniff";

    @Override
    public void doFilter(final ServletRequest servletRequest,
                         final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) servletRequest;
        final HttpServletResponse res = (HttpServletResponse) servletResponse;
        res.addHeader(NOSNIFF_HEADER_KEY, NOSNIFF_HEADER_VALUE);
        filterChain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        //Stateless filter so destroy not needed
    }
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        //Stateless filter so init not needed
    }

}
