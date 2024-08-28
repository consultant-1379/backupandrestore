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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.adp.mgmt.backupandrestore.productinfo.exception.MissingFieldsInConfigmapException;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.exception.UnableToRetrieveDataFromConfigmapException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.kubernetes.KubernetesService;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api.APIreadNamespacedConfigMapRequest;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

public class ProductInfoServiceTest {

    private static final String CONFIG_MAP_NAME = "configmap";
    private static final String NAMESPACE = "namespace";

    private ProductInfoService productInfoService;
    private CoreV1Api kubernetesApi;

    @Before
    public void setup() {
        productInfoService = new ProductInfoService();
        kubernetesApi = createMock(CoreV1Api.class);
        final KubernetesService kubernetesService = createMock(KubernetesService.class);
        expect(kubernetesService.getKubernetesApi()).andReturn(kubernetesApi);
        expect(kubernetesService.getOrchestratorNamespace()).andReturn(NAMESPACE);
        replay(kubernetesService);
        productInfoService.setKubernetesDetails(kubernetesService);
        productInfoService.setProductInfoConfigMapName(CONFIG_MAP_NAME);
        productInfoService.setProductMatchingConfigMapName(CONFIG_MAP_NAME);
    }

    @Test
    public void getAppProductInfo_configmapExists_populatedProductInfo() throws Exception {
        APIreadNamespacedConfigMapRequest expectedRequest = createMock(APIreadNamespacedConfigMapRequest.class);
        final V1ConfigMap configMap = new V1ConfigMap();
        configMap.setMetadata(getProductInfoTestMetadata());
        expect(kubernetesApi.readNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE)).andReturn(expectedRequest);
        expect(expectedRequest.pretty(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.execute()).andReturn(configMap);
        replay(kubernetesApi, expectedRequest);

        final SoftwareVersion productInfo = productInfoService.getAppProductInfo();
        assertEquals("a", productInfo.getProductName());
        assertEquals("b", productInfo.getProductNumber());
        assertEquals("c", productInfo.getProductRevision());
        assertEquals("d", productInfo.getDate());
        assertEquals("e", productInfo.getDescription());
        assertEquals("f", productInfo.getType());
        assertEquals("g", productInfo.getSemanticVersion());

    }

    @Test
    public void getAppProductInfo_configmapExistsAndHasProductNumberAnnotation_populatedProductInfo() throws Exception {
        APIreadNamespacedConfigMapRequest expectedRequest = createMock(APIreadNamespacedConfigMapRequest.class);
        final V1ConfigMap configMap = new V1ConfigMap();
        final V1ObjectMeta metadata = new V1ObjectMeta();

        final Map<String, String> annotations = new HashMap<>();
        annotations.put("ericsson.com/product-number", "b");
        metadata.setAnnotations(annotations);

        configMap.setMetadata(metadata);
        expect(kubernetesApi.readNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE)).andReturn(expectedRequest);
        expect(expectedRequest.pretty(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.execute()).andReturn(configMap);
        replay(kubernetesApi, expectedRequest);

        final SoftwareVersion productInfo = productInfoService.getAppProductInfo();
        assertEquals("", productInfo.getProductName());
        assertEquals("b", productInfo.getProductNumber());
        assertEquals("", productInfo.getProductRevision());
        assertEquals("", productInfo.getDate());
        assertEquals("", productInfo.getDescription());
        assertEquals("", productInfo.getType());
        assertEquals("0.0.0", productInfo.getSemanticVersion());
        assertNull(productInfoService.getSemVerMatchType());
        assertNull(productInfoService.getProductLowestAllowedVersion());
    }

    @Test(expected = MissingFieldsInConfigmapException.class)
    public void getAppProductInfo_configmapExistsButMissingProductNumberAnnotation_throwsException() throws Exception {
        APIreadNamespacedConfigMapRequest expectedRequest = createMock(APIreadNamespacedConfigMapRequest.class);
        final V1ConfigMap configMap = new V1ConfigMap();
        configMap.setMetadata(new V1ObjectMeta());
        configMap.getMetadata().setAnnotations(new HashMap<>());
        expect(kubernetesApi.readNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE)).andReturn(expectedRequest);
        expect(expectedRequest.pretty(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.execute()).andReturn(configMap);
        replay(kubernetesApi, expectedRequest);

        productInfoService.getAppProductInfo();
    }

    @Test(expected = MissingFieldsInConfigmapException.class)
    public void getAppProductInfo_configmapExistsButMissingAnnotation_throwsException() throws Exception {
        APIreadNamespacedConfigMapRequest expectedRequest = createMock(APIreadNamespacedConfigMapRequest.class);
        final V1ConfigMap configMap = new V1ConfigMap();
        configMap.setMetadata(new V1ObjectMeta());
        expect(kubernetesApi.readNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE)).andReturn(expectedRequest);
        expect(expectedRequest.pretty(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.execute()).andReturn(configMap);
        replay(kubernetesApi, expectedRequest);

        productInfoService.getAppProductInfo();
    }

    @Test(expected = ApiException.class)
    public void getAppProductInfo_apiThrowsException_throwsException() throws Exception {
        APIreadNamespacedConfigMapRequest expectedRequest = createMock(APIreadNamespacedConfigMapRequest.class);
        expect(kubernetesApi.readNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE)).andReturn(expectedRequest);
        expect(expectedRequest.pretty(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.execute()).andThrow(new ApiException());
        replay(kubernetesApi, expectedRequest);
        productInfoService.getAppProductInfo();
    }

    @Test(expected = UnableToRetrieveDataFromConfigmapException.class)
    public void getProductNumberList_configmapDoesNotExist_throwsException() throws Exception {
        APIreadNamespacedConfigMapRequest expectedRequest = createMock(APIreadNamespacedConfigMapRequest.class);
        expect(kubernetesApi.readNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE)).andReturn(expectedRequest);
        expect(expectedRequest.pretty(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.execute()).andThrow(new ApiException());
        replay(kubernetesApi, expectedRequest);
        productInfoService.getProductNumberList();
    }

    @Test
    public void getProductNumberList_configmapExistsButMissingProductNumberList_returnsListWithOneEntry() throws Exception {
        APIreadNamespacedConfigMapRequest expectedRequest = createMock(APIreadNamespacedConfigMapRequest.class);
        final V1ConfigMap configMap = new V1ConfigMap();
        configMap.setData(new HashMap<>());
        expect(kubernetesApi.readNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE)).andReturn(expectedRequest);
        expect(expectedRequest.pretty(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.execute()).andReturn(configMap).times(2);
        replay(kubernetesApi, expectedRequest);

        final List<String> resultList = productInfoService.getProductNumberList();
        assertEquals(1, resultList.size());
        assertEquals("", resultList.get(0));
    }

    @Test
    public void getProductNumberList_configmapExistsButMissingData_throwsException() throws Exception {
        final V1ConfigMap configMap = new V1ConfigMap();
        APIreadNamespacedConfigMapRequest expectedRequest = createMock(APIreadNamespacedConfigMapRequest.class);
        expect(expectedRequest.pretty(EasyMock.anyString())).andReturn(expectedRequest);
        expect(kubernetesApi.readNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE)).andReturn(expectedRequest);
        expect(expectedRequest.execute()).andReturn(configMap);
        replay(kubernetesApi, expectedRequest);

        productInfoService.getProductNumberList();
    }

    @Test
    public void getProductNumberList_productNumberListWithCommaSeparatedValues_returnsListWithOneEntry() throws Exception {
        APIreadNamespacedConfigMapRequest expectedRequest = createMock(APIreadNamespacedConfigMapRequest.class);
        final V1ConfigMap configMap = new V1ConfigMap();
        final Map<String, String> dataEntry = new HashMap<>();
        final String productNumber = "ADP 000 000 BRO, ADP 000 010 BRO, ADP 020 BRO";
        dataEntry.put("productNumberList", productNumber);
        configMap.setData(dataEntry);
        expect(kubernetesApi.readNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE)).andReturn(expectedRequest);
        expect(expectedRequest.pretty(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.execute()).andReturn(configMap).times(2);
        replay(kubernetesApi, expectedRequest);

        final List<String> resultList = productInfoService.getProductNumberList();
        assertEquals(3, resultList.size());
        assertEquals("ADP 000 000 BRO", resultList.get(0));
    }

    private V1ObjectMeta getProductInfoTestMetadata() {
        final V1ObjectMeta metadata = new V1ObjectMeta();

        final Map<String, String> annotations = new HashMap<>();
        annotations.put("ericsson.com/product-name", "a");
        annotations.put("ericsson.com/product-number", "b");
        annotations.put("ericsson.com/product-revision", "c");
        annotations.put("ericsson.com/production-date", "d");
        annotations.put("ericsson.com/description", "e");
        annotations.put("ericsson.com/type", "f");
        annotations.put("ericsson.com/semantic-version", "g");

        metadata.setAnnotations(annotations);
        return metadata;
    }
}
