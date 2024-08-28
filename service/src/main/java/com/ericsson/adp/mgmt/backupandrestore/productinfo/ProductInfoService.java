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
package com.ericsson.adp.mgmt.backupandrestore.productinfo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ericsson.adp.mgmt.backupandrestore.productinfo.exception.MissingFieldsInConfigmapException;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.exception.UnableToRetrieveDataFromConfigmapException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.kubernetes.KubernetesService;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;

/**
 * Service for handling Ericsson product information
 */
@Service
@Primary
public class ProductInfoService {

    private static final String PRODUCT_NAME_KEY = "ericsson.com/product-name";
    private static final String PRODUCT_NUMBER_KEY = "ericsson.com/product-number";
    private static final String PRODUCT_REVISION_KEY = "ericsson.com/product-revision";
    private static final String PRODUCT_MATCH_LIST_KEY = "productNumberList";
    private static final String DATE_KEY = "ericsson.com/production-date";
    private static final String DESCRIPTION_KEY = "ericsson.com/description";
    private static final String TYPE_KEY = "ericsson.com/type";
    private static final String APP_PRODUCT_INFO_ID = "APPLICATION_INFO";
    private static final String SEMANTIC_VERSION_KEY = "ericsson.com/semantic-version";

    private CoreV1Api kubernetesApi;
    private String productInfoConfigMapName;
    private String namespace;
    private String orchestratorProductName;
    private String orchestratorProductNumber;
    private String orchestratorProductRevision;
    private String productMatchingConfigMapName;
    private String productMatchType;
    private String semVerMatchType;
    private String productLowestAllowedVersion;
    private String selectedMatchType;

    /**
     * Retrieves the application product information from the kubernetes api
     *
     * @return application product information
     * @throws ApiException if any kubernetes related failure occurs
     */
    public SoftwareVersion getAppProductInfo() throws ApiException {
        try {
            return createAppProductInfo(
                    getConfigMap(productInfoConfigMapName).getMetadata().getAnnotations());
        } catch (final NullPointerException e) {
            throw new MissingFieldsInConfigmapException("Missing annotations in product info configmap");
        }
    }

    /**
     * Retrieves the matching criteria to validate product info
     * @return product matching criteria
     */
    public String getProductMatchType() {
        return this.productMatchType;
    }

    public String getSemVerMatchType() {
        return this.semVerMatchType;
    }

    public String getProductLowestAllowedVersion() {
        return this.productLowestAllowedVersion;
    }

    public String getSelectedMatchType() {
        return this.selectedMatchType;
    }

    /**
     * Retrieves product number list for product info check
     * @return product number list
     */
    public List<String> getProductNumberList() {
        try {
            return Arrays.asList(
                    getConfigMap(productMatchingConfigMapName).getData()
                    .getOrDefault(PRODUCT_MATCH_LIST_KEY, "")
                    .split(","));
        } catch (final ApiException e) {
            throw new UnableToRetrieveDataFromConfigmapException("Unable to retrieve product number list from BRO configmap");
        } catch (final NullPointerException e) {
            throw new MissingFieldsInConfigmapException("Missing product number list in BRO configmap");
        }
    }

    private V1ConfigMap getConfigMap(final String configMapName) throws ApiException {
        return kubernetesApi.readNamespacedConfigMap(
                configMapName, namespace)
                .pretty(null)
                .execute();
    }

    /**
     * Retrieves the orchestrator's product information
     *
     * @return orchestrator's product information
     */
    public SoftwareVersion getOrchestratorProductInfo() {
        final SoftwareVersion orchestratorProductInfo = new SoftwareVersion();
        orchestratorProductInfo.setProductName(orchestratorProductName);
        orchestratorProductInfo.setProductNumber(orchestratorProductNumber);
        orchestratorProductInfo.setProductRevision(orchestratorProductRevision);
        orchestratorProductInfo.setDate("");
        orchestratorProductInfo.setDescription("");
        orchestratorProductInfo.setType("");
        orchestratorProductInfo.setAgentId(APP_PRODUCT_INFO_ID);
        return orchestratorProductInfo;
    }

    private SoftwareVersion createAppProductInfo(final Map<String, String> annotations) {
        final String productNumber = annotations.get(PRODUCT_NUMBER_KEY);

        if (productNumber == null) {
            throw new MissingFieldsInConfigmapException("Missing product number annotation in product info configmap");
        }

        final SoftwareVersion productInfo = new SoftwareVersion();

        productInfo.setProductName(annotations.getOrDefault(PRODUCT_NAME_KEY, ""));
        productInfo.setProductNumber(productNumber);
        productInfo.setProductRevision(annotations.getOrDefault(PRODUCT_REVISION_KEY, ""));
        productInfo.setDate(annotations.getOrDefault(DATE_KEY, ""));
        productInfo.setDescription(annotations.getOrDefault(DESCRIPTION_KEY, ""));
        productInfo.setType(annotations.getOrDefault(TYPE_KEY, ""));
        productInfo.setAgentId(APP_PRODUCT_INFO_ID);
        productInfo.setSemanticVersion(annotations.getOrDefault(SEMANTIC_VERSION_KEY, "0.0.0"));
        return productInfo;
    }

    /**
     * Set the properties required for making Kubernetes api requests
     *
     * @param kubernetesService The KubernetesService to retrieve the kubernetes details from
     */
    @Autowired
    public void setKubernetesDetails(final KubernetesService kubernetesService) {
        kubernetesApi = kubernetesService.getKubernetesApi();
        namespace = kubernetesService.getOrchestratorNamespace();
    }

    @Value("${kubernetes.app.product.info.configmap.name:}")
    public void setProductInfoConfigMapName(final String productInfoConfigMapName) {
        this.productInfoConfigMapName = productInfoConfigMapName;
    }

    @Value("${kubernetes.app.product.check.configmap.name:}")
    public void setProductMatchingConfigMapName(final String productMatchingConfigMapName) {
        this.productMatchingConfigMapName = productMatchingConfigMapName;
    }

    @Value("${bro.product.name}")
    public void setOrchestratorProductName(final String orchestratorProductName) {
        this.orchestratorProductName = orchestratorProductName;
    }

    @Value("${bro.product.number}")
    public void setOrchestratorProductNumber(final String orchestratorProductNumber) {
        this.orchestratorProductNumber = orchestratorProductNumber;
    }

    @Value("${bro.product.revision}")
    public void setOrchestratorProductRevision(final String orchestratorProductRevision) {
        this.orchestratorProductRevision = orchestratorProductRevision;
    }

    @Value("${bro.productMatchType}")
    public void setProductMatchType(final String productMatchType) {
        this.productMatchType = productMatchType;
    }

    @Value("${bro.semVerMatchType}")
    public void setSemVerMatchType(final String semVerMatchType) {
        this.semVerMatchType = semVerMatchType;
    }

    @Value("${bro.selectedMatchType}")
    public void setSelectedMatchType(final String selectedMatchType) {
        this.selectedMatchType = selectedMatchType;
    }

    @Value("${bro.productLowestAllowedVersion}")
    public void setProductLowestAllowedVersion(final String productLowestAllowedVersion) {
        this.productLowestAllowedVersion = productLowestAllowedVersion;
    }
}
