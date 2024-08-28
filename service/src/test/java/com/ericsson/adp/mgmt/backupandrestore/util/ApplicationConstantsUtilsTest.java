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
package com.ericsson.adp.mgmt.backupandrestore.util;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.Test;

public class ApplicationConstantsUtilsTest {

    @Test(expected = IllegalAccessException.class)
    public void ApplicationConstant_CallingConstructor_InvalidInvocation() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException{
          Constructor<ApplicationConstantsUtils> constructor = ApplicationConstantsUtils.class.getDeclaredConstructor();
          assertTrue(Modifier.isPrivate(constructor.getModifiers()));
          constructor.newInstance();
    }

}
