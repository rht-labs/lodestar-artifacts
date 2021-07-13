package com.redhat.labs.mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ExternalApiWireMock implements QuarkusTestResourceLifecycleManager {

    private WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {

        wireMockServer = new WireMockServer();
        wireMockServer.start();

        // get engagement projects
        String body = ResourceLoader.load("engagement-projects.json");

        stubFor(get(urlEqualTo("/api/v1/engagements/projects"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

        body = ResourceLoader.load("engagement-project-1.json");

        stubFor(get(urlEqualTo("/api/v1/engagements/projects/1111?mini=true"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

        body = ResourceLoader.load("engagement-project-2.json");

        stubFor(get(urlEqualTo("/api/v1/engagements/projects/2222?mini=true"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

//        stubFor(get(urlEqualTo("/api/v1/engagements/projects/2222"))
//                .willReturn(aResponse().withHeader("Content-Type", "application/json").withStatus(404)));

        // get artifacts.json

        body = ResourceLoader.load("project-1-artifacts-file.json");

        stubFor(get(urlEqualTo("/api/v4/projects/1/repository/files/artifacts.json?ref=master"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

        body = ResourceLoader.load("project-91-artifacts-file.json");
        
        stubFor(get(urlEqualTo("/api/v4/projects/91/repository/files/artifacts.json?ref=master"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
        
        stubFor(get(urlEqualTo("/api/v4/projects/92/repository/files/artifacts.json?ref=master"))
                .willReturn(aResponse().withStatus(404)));

        // update existing artifacts.json

        stubFor(put(urlEqualTo("/api/v4/projects/1/repository/files/artifacts.json"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withStatus(200)));

        stubFor(put(urlEqualTo("/api/v4/projects/2/repository/files/artifacts.json"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withStatus(200)));

        // set endpoint

        Map<String, String> config = new HashMap<>();
        config.put("gitlab.api/mp-rest/url", wireMockServer.baseUrl());
        config.put("engagement.api/mp-rest/url", wireMockServer.baseUrl());
        return config;

    }

    @Override
    public void stop() {

        if (null != wireMockServer) {
            wireMockServer.stop();
        }

    }

}
