package com.redhat.labs.lodestar.artifacts.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;

import com.redhat.labs.lodestar.artifacts.mock.ExternalApiWireMock;
import com.redhat.labs.lodestar.artifacts.mock.ResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.artifacts.model.Artifact;
import com.redhat.labs.lodestar.artifacts.model.ArtifactCount;
import com.redhat.labs.lodestar.artifacts.model.GetListOptions;
import com.redhat.labs.lodestar.artifacts.model.GetOptions;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(ExternalApiWireMock.class)
class ArtifactServiceTest {

    @Inject
    ArtifactService artifactService;

    @Inject
    Jsonb jsonb;

    @BeforeEach
    void setUp() {
        artifactService.purge();
        artifactService.refresh();
    }

    @Test
    void testPopulatedDB() {
        artifactService.purge();


        ArtifactCount count = artifactService.countArtifacts(new GetListOptions());
        assertEquals(0, count.getCount());

        artifactService.checkDBPopulated();
        count = artifactService.countArtifacts(new GetListOptions());
        assertEquals(2, count.getCount());

        artifactService.checkDBPopulated();
        count = artifactService.countArtifacts(new GetListOptions());
        assertEquals(2, count.getCount());

    }

    @Test
    void testRefresh() {

        // then
        GetListOptions options = new GetListOptions();
        options.setEngagementUuid("1111");
        List<Artifact> artifacts = artifactService.getArtifacts(options);
        assertNotNull(artifacts);
        assertEquals(2, artifacts.size());

    }

    @Test
    void testModifyByEngagementIdUpdateInGit() {

        // given
        String engagementUuid = "1111";

        // one new
        Artifact newOne = Artifact.builder().engagementUuid(engagementUuid).description("a new artifact")
                .linkAddress("http://a-new-one").title("New One").type("typeOne").region("na").build();

        // modify one
        String json = ResourceLoader.load("project-1-artifacts.json");
        List<Artifact> artifacts = jsonb.fromJson(json, new ArrayList<Artifact>() {
            private static final long serialVersionUID = 1L;
        }.getClass().getGenericSuperclass());

        Artifact modifyOne = artifacts.get(0);
        modifyOne.setDescription("Updated");

        // when
        artifactService.updateArtifacts(engagementUuid, "na", Arrays.asList(newOne, modifyOne), Optional.empty(), Optional.empty());

        // then
        GetListOptions options = new GetListOptions();
        options.setEngagementUuid(engagementUuid);
        List<Artifact> updated = artifactService.getArtifacts(options);

        assertNotNull(updated);
        assertEquals(2, updated.size());

        updated.forEach(a -> {
            if (a.getType().equals("Demo")) {
                assertEquals("Updated", a.getDescription());
            } else if (a.getType().equals("typeOne")) {
                assertNotNull(a.getUuid());
            }
        });

    }

    @Test
    void testGetArtifactsNoOptions() {

        // given
        GetListOptions options = new GetListOptions();

        // when
        List<Artifact> artifacts = artifactService.getArtifacts(options);

        // then
        assertNotNull(artifacts);
        assertEquals(2, artifacts.size());

    }

    @Test
    void testGetArtifactsByEngagement() {

        // given
        GetListOptions options = new GetListOptions();
        options.setEngagementUuid("1111");

        // when
        List<Artifact> artifacts = artifactService.getArtifacts(options);

        // then
        assertNotNull(artifacts);
        assertEquals(2, artifacts.size());

    }
    
    @Test
    void testGetArtifactsByType() {

        // given
        GetListOptions options = new GetListOptions();
        options.setType("Demo");

        // when
        List<Artifact> artifacts = artifactService.getArtifacts(options);

        // then
        assertNotNull(artifacts);
        assertEquals(1, artifacts.size());

    }
    
    @Test
    void testGetArtifactsByRegion() {
    	 // given
        GetListOptions options = new GetListOptions();
        options.setRegion(Collections.singletonList("na"));

        // when
        List<Artifact> artifacts = artifactService.getArtifacts(options);

        // then
        assertNotNull(artifacts);
        assertEquals(2, artifacts.size());
    }
    
    @Test
    void testGetArtifactsByRegionAndType() {
    	 // given
        GetListOptions options = new GetListOptions();
        options.setRegion(Collections.singletonList("na"));
        options.setType("Multimedia");

        // when
        List<Artifact> artifacts = artifactService.getArtifacts(options);

        // then
        assertNotNull(artifacts);
        assertEquals(1, artifacts.size());
    }
    
    @Test
    void testGetArtifactsByTypeException() {

        // given
        GetListOptions options = new GetListOptions();
        options.setType("Demo");
        options.setEngagementUuid("1111");
        
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> artifactService.getArtifacts(options));

        assertEquals(400, ex.getResponse().getStatus());

    }

    @Test
    void testGetArtifactsPaging() {

        // given
        GetListOptions options = new GetListOptions();
        options.setPage(0);
        options.setPageSize(1);

        // when
        List<Artifact> artifacts = artifactService.getArtifacts(options);

        // then
        assertNotNull(artifacts);
        assertEquals(1, artifacts.size());

        // given
        options.setPage(1);

        // when
        artifacts = artifactService.getArtifacts(options);

        // then
        assertNotNull(artifacts);
        assertEquals(1, artifacts.size());

    }

    @Test
    void testCountArtifactsNoOptions() {

        // given
        GetOptions options = new GetOptions();

        // when
        ArtifactCount count = artifactService.countArtifacts(options);

        // then
        assertNotNull(count);
        assertEquals(2, count.getCount());

    }
    
    @Test
    void testCountArtifactsByType() {

        // given
        GetOptions options = new GetOptions(null, "Demo", null);

        // when
        ArtifactCount count = artifactService.countArtifacts(options);

        // then
        assertNotNull(count);
        assertEquals(1, count.getCount());

    }
    
    @Test
    void testCountArtifactsByRegion() {

        // given
        GetOptions options = new GetOptions(null, "Demo", null);

        // when
        ArtifactCount count = artifactService.countArtifacts(options);

        // then
        assertNotNull(count);
        assertEquals(1, count.getCount());

    }
    
    @Test
    void testCountArtifactsByRegionAndType() {

        // given
        GetOptions options = new GetOptions(null, "Demo", null);
        options.setRegion(Collections.singletonList("na"));
        options.setType("Multimedia");

        // when
        ArtifactCount count = artifactService.countArtifacts(options);

        // then
        assertNotNull(count);
        assertEquals(1, count.getCount());

    }

    @Test
    void testCountArtifactsByEngagement() {

        // given
        GetOptions options = new GetOptions("1111", null, null);
        options.setRegion(Collections.singletonList("na"));

        // when
        ArtifactCount count = artifactService.countArtifacts(options);

        // then
        assertNotNull(count);
        assertEquals(2, count.getCount());

    }

}
