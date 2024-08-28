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

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Responsible for Rest API Filter Configuration.
 */
@Configuration
public class RestFilterConfiguration {

    /**
     * Adds DisableMimeSniffingFilter to v1 and v3 endpoints
     * @return DisableMimeSniffingFilter registration
     */
    @Bean
    public FilterRegistrationBean<DisableMimeSniffingFilter> sniffingFilterForV1RestApi() {
        final FilterRegistrationBean<DisableMimeSniffingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new DisableMimeSniffingFilter());
        registrationBean.addUrlPatterns("/v1/*", "/v3/*", "/backup-restore/v4/*");
        return registrationBean;
    }
}
