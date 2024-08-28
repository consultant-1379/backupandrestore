/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.cminterface.nacm;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents NACM Role in CM.
 */
@JsonInclude(Include.NON_NULL)
public class NACMRole {

    private String name;
    private List<String> group = new ArrayList<>();
    private List<NACMRule> rule = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<String> getGroup() {
        return group;
    }

    public void setGroup(final List<String> group) {
        this.group = group;
    }

    public List<NACMRule> getRule() {
        return rule;
    }

    public void setRule(final List<NACMRule> rule) {
        this.rule = rule;
    }

}
