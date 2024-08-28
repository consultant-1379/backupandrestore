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
package com.ericsson.adp.mgmt.backupandrestore.ssl;

/**
 * Enum to represent the various keystores in the BRO codebase
 * */
public enum KeyStoreAlias {
    BRO,
    CMM_REST,
    CMM_NOTIF,
    PM,
    KAFKA,
    LT,
    REDIS,
    OSMN;

    /**
     * Retrieves the keyStoreAlias by the alias
     * @param alias String representing the keyStoreAlias
     * @return exception if an no defined alias is requested
     */
    public static KeyStoreAlias fromString(final String alias) {
        for (final KeyStoreAlias storeAlias : KeyStoreAlias.values()) {
            if (storeAlias.name().equals(alias)) {
                return storeAlias;
            }
        }
        throw new IllegalArgumentException("No constant " + KeyStoreAlias.class.getCanonicalName() + "." + alias);
    }
}
