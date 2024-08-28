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
package com.ericsson.adp.mgmt.bro.api.grpc;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;


import static org.junit.Assert.*;

public class GRPCConfTest {

    private static void setEnv(String name, String value) throws RuntimeException {
        // Uses reflection to change the unmodifiable map returned by System.getenv()
        // This is needed because we need to mock the a System class and PowerMock
        // cannot be used due to incompatibility with coverage tool JaCoCo
        Class[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for (Class cl : classes) {
            if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                try {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    if (value == null) {
                        map.remove(name);
                    } else {
                        map.put(name, value);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to set environment variable", e);
                }
            }
        }
    }


    @Test
    public void defaultValue() {
        setEnv("TEST_VAR", null);

        Assert.assertEquals(1024, GRPCConfig.getIntEnvOrDefault("TEST_VAR", 1024));
    }

    @Test
    public void validValue() {
        setEnv("TEST_VAR", "512");

        assertEquals(512, GRPCConfig.getIntEnvOrDefault("TEST_VAR", 1024));
    }

    @Test
    public void invalidValue() {
        setEnv("TEST_VAR", "1024s");

        Throwable expectedException = null;
        try {
            GRPCConfig.getIntEnvOrDefault("TEST_VAR", 512);
        } catch (Throwable e){
            expectedException = e;
        }

        assertNotNull(expectedException);
    }

}
