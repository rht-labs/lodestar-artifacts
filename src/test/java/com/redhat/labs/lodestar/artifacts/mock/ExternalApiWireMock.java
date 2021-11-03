package com.redhat.labs.lodestar.artifacts.mock;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ExternalApiWireMock implements QuarkusTestResourceLifecycleManager {

    private WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {

        wireMockServer = new WireMockServer();
        wireMockServer.start();

        // get engagement projects
        String body = ResourceLoader.load("engagement-projects.json");

        stubFor(get(urlEqualTo("/api/v2/engagements"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

        body = ResourceLoader.load("engagement-project-1.json");

        stubFor(get(urlEqualTo("/api/v2/engagements/1111"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

        body = ResourceLoader.load("engagement-project-2.json");

        stubFor(get(urlEqualTo("/api/v2/engagements/2222"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

//        stubFor(get(urlEqualTo("/api/v1/engagements/projects/2222"))
//                .willReturn(aResponse().withHeader("Content-Type", "application/json").withStatus(404)));

        // get artifacts.json

        body = ResourceLoader.load("project-1-artifacts-file.json");

        stubFor(get(urlEqualTo("/api/v4/projects/1/repository/files/engagement%2Fartifacts.json?ref=master"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

        body = ResourceLoader.load("project-91-artifacts-file.json");

        stubFor(get(urlEqualTo("/api/v4/projects/91/repository/files/engagement%2Fartifacts.json?ref=master"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
        
        stubFor(get(urlEqualTo("/api/v4/projects/92/repository/files/engagement%2Fartifacts.json?ref=master"))
                .willReturn(aResponse().withStatus(404)));

        body = ResourceLoader.loadGitlabFile("engagement-gitlab-file-1.json");

        stubFor(get(urlEqualTo("/api/v4/projects/1/repository/files/engagement.json?ref=master"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

        // update existing artifacts.json

        stubFor(post(urlMatching("/api/v4/projects/[1-2]/repository/commits"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withStatus(200)));

        stubFor(put(urlMatching("/api/v2/engagements/1111/artifacts/[0-9]")).willReturn(aResponse().withStatus(200)));

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
