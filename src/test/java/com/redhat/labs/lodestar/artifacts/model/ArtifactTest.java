package com.redhat.labs.lodestar.artifacts.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import com.redhat.labs.lodestar.artifacts.mock.ExternalApiWireMock;
import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.artifacts.service.ArtifactService;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(ExternalApiWireMock.class)
class ArtifactTest {

	@Inject
	ArtifactService service;

	@BeforeEach
	void setup() {
		service.purge();
		service.refresh();
	}

	@Test
	void testCountAllArtifacts() {
		assertEquals(2, Artifact.countAllArtifacts().getCount());
	}

	@Test
	void testCountArtifactsByEngagementUuid() {
		assertEquals(2, Artifact.countArtifactsByEngagementUuid("1111").getCount());
	}

	@Test
	void testPagedArtifacts() {
		assertEquals(1, Artifact.pagedArtifacts(0, 1, Sort.by("uuid")).size());
	}

	@Test
	void testPagedArtifactsByEngagementUuid() {
		assertEquals(1, Artifact.pagedArtifactsByEngagementUuid("1111", 0, 1, Sort.by("uuid")).size());
	}

	@Test
	void testFindAllByEngagementUuid() {
		assertEquals(2, Artifact.findAllByEngagementUuid("1111").size());
	}

	@Test
	void testFindByUuid() {

		List<Artifact> artifacts = Artifact.findAllByEngagementUuid("1111");
		String uuid = artifacts.get(0).getUuid();

		assertTrue(Artifact.findByUuid(uuid).isPresent());

	}

	@Test
	void testDeleteByUuid() {

		List<Artifact> artifacts = Artifact.findAllByEngagementUuid("1111");
		String uuid = artifacts.get(0).getUuid();

		Artifact.deleteByUuid(uuid);

		assertTrue(Artifact.findByUuid(uuid).isEmpty());

	}

	@Test
	void testRemoveAllArtifacts() {
		assertEquals(2, Artifact.removeAllArtifacts());
	}

}
