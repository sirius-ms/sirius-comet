/*
 * SIRIUS Nightsky API
 * REST API that provides the full functionality of SIRIUS and its web services as background service. It is intended as entry-point for scripting languages and software integration SDKs.This API is exposed by SIRIUS 6.0.0-SNAPSHOT
 *
 * The version of the OpenAPI document: 2.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package de.unijena.bioinf.ms.nightsky.sdk.api;

import org.junit.Test;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API tests for ActuatorApi
 */
@Ignore
public class ActuatorApiTest {

    private final ActuatorApi api = new ActuatorApi();

    
    /**
     * Actuator web endpoint &#39;health&#39;
     *
     * 
     */
    @Test
    public void healthTest()  {
        Object response = api.health();

        // TODO: test validations
    }
    
    /**
     * Actuator web endpoint &#39;shutdown&#39;
     *
     * 
     */
    @Test
    public void shutdownTest()  {
        Object response = api.shutdown();

        // TODO: test validations
    }
    
}
