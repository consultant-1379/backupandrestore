/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.persist;

import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.ericsson.adp.mgmt.bro.api.fragment.FragmentInformation;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Wraps a FragmentInformation and a Version, composing them together to get a Versioned FragmentInformation (simply
 * making FragmentInformation Versioned<T> isn't possible because it would introduce a circular dependency between
 * the agent library and BRO. Simply making this "extends FragmentInformation implements Versioned" isn't possible
 * because FragmentInformation::getVersion() exists.
 *
 * This mostly doesn't matter, since we don't use the version information anyway for Fragments (any change to the information
 * in a Fragment is a GRPC version bump, which is a deprecation anyway), but it's a necessary workaround to satisfy the
 * type system, and it's worth trying to get it as right as possible (BRO may, for example at some point want to use a
 * slightly different representation of the Fragment object than the representation defined in the agent API, and at that
 * point having a strongly Versioned type will be useful)
 * */
@JsonDeserialize(using = PersistedFragmentInformation.Deserializer.class)
public class PersistedFragmentInformation implements Versioned<PersistedFragmentInformation> {

    private final FragmentInformation info;
    private Version<PersistedFragmentInformation> version;

    /**
     * Constructor PersistedFragment
     * @param info information for a fragment
     * @param version fragment information version
     */
    public PersistedFragmentInformation(final FragmentInformation info, final Version<PersistedFragmentInformation> version) {
        this.info = info;
        this.version = version;
    }

    /**
     * Constructor PersistedFragment using a persisted fragment
     * @param other persisted fragment information used to be duplicate
     */
    public PersistedFragmentInformation(final PersistedFragmentInformation other) {
        this.info = other.info;
        this.version = other.version;
    }

    /**
     * Retrieve fragment information
     * @return fragment information
     */
    public FragmentInformation getInfo() {
        return info;
    }

    @Override
    @JsonIgnore
    public Version<PersistedFragmentInformation> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<PersistedFragmentInformation> version) {
        this.version = version;
    }

    /**
     * A custom Deserializer for PersistedFragmentInformation objects, that simply wraps the default deserializer for
     * FragmentInformation types, and leaves the inner Version null (as the FileService will set it later)
     * */
    public static class Deserializer extends StdDeserializer<PersistedFragmentInformation> {

        /**
         * Constructor setting the version to null
         */
        public Deserializer() {
            this(null);
        }

        /**
         * Constructor using PersistedFragmentInformation
         * @param versionedclass Version Class
         */
        public Deserializer(final Class<?> versionedclass) {
            super(versionedclass);
        }

        @Override
        public PersistedFragmentInformation deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final FragmentInformation inner = jsonParser.readValueAs(FragmentInformation.class);
            return new PersistedFragmentInformation(inner, null);
        }
    }
}
