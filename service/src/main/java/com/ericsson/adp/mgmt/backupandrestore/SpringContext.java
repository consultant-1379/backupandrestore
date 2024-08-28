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
package com.ericsson.adp.mgmt.backupandrestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Provides access to Spring Application Context for classes not managed by Spring
 */
@Component
public class SpringContext {

    private static Optional<ApplicationContext> context = Optional.empty();

    /**
     * Returns the Spring managed bean instance of the given class type if it exists.
     * Returns null otherwise.
     * @param beanClass to get instance for
     * @param <T> type of bean to return
     * @return beanInstance
     */
    public static <T> Optional<T> getBean(final Class<T> beanClass) {
        Optional<T> bean = Optional.empty();
        if (context.isPresent()) {
            bean = Optional.ofNullable(context.get().getBean(beanClass));
        }
        return bean;
    }

    @Autowired
    public synchronized void setApplicationContext(final ApplicationContext context) {
        SpringContext.context = Optional.ofNullable(context);
    }
}
