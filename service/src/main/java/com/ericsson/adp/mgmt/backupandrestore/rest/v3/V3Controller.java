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
package com.ericsson.adp.mgmt.backupandrestore.rest.v3;

import com.ericsson.adp.mgmt.backupandrestore.rest.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Parent of controllers for v3 endpoints.
 */
@RequestMapping("v3")
public abstract class V3Controller extends BaseController {

}
