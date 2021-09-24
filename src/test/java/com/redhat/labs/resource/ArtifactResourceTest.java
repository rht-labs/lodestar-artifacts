package com.redhat.labs.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.GetListOptions;
import com.redhat.labs.lodestar.service.ArtifactService;
import com.redhat.labs.mock.ExternalApiWireMock;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

@QuarkusTest
@QuarkusTestResource(ExternalApiWireMock.class)
class ArtifactResourceTest {
	
	@Inject
	ArtifactService service;

	@Inject
	Jsonb jsonb;

	@BeforeEach
	void setup() {
		service.purge();
		service.refresh();
	}

	@Test
	void testCountArtifactsAll() {

		given().when().get("/api/artifacts/count").then().statusCode(200).body("count", equalTo(2));

	}
	
	@Test
	void testRefresh() {
	    given().when().put("/api/artifacts/refresh").then().statusCode(202);
	    
	    assertEquals(2, service.getArtifacts(new GetListOptions()).size());
	}

	@Test
	void testCountArtifactsByEngagement() {

		given().queryParam("engagementUuid", "1111").when().get("/api/artifacts/count").then().statusCode(200)
				.body("count", equalTo(2));
	}

	@Test
	void testGetArtifactsAll() {

		given().when().get("/api/artifacts").then().statusCode(200).body("size()", equalTo(2));
	}

	@Test
	void testGetAllSorted() {
		given().queryParam("sort", "title|DESC,uuid|ASC").when().get("/api/artifacts")
				.then().statusCode(200).body("size()", equalTo(2)).body("[0].title", equalTo("Video One"));

		given().queryParam("sort", "title|ASC").when().get("/api/artifacts")
				.then().statusCode(200).body("size()", equalTo(2)).body("[0].title", equalTo("Demo One"));
	}

	@Test
	void testGetArtifactsAllPaged() {

		// first page
		JsonPath path = given().queryParam("page", -2).queryParam("pageSize", 1).when().get("/api/artifacts").then()
				.statusCode(200).extract().jsonPath();
		assertEquals(1, path.getList(".").size());

		// last page
		path = given().queryParam("page", 1).queryParam("pageSize", 1).when().get("/api/artifacts").then()
				.statusCode(200).extract().jsonPath();
		assertEquals(1, path.getList(".").size());

		// page with no results
		path = given().queryParam("page", 2).queryParam("pageSize", 1).when().get("/api/artifacts").then()
				.statusCode(200).extract().jsonPath();
		assertEquals(0, path.getList(".").size());

	}

	@Test
	void testGetArtifactsByEngagement() {

		JsonPath path = given().queryParam("engagementUuid", "1111").when().get("/api/artifacts").then()
				.statusCode(200).extract().jsonPath();
		assertEquals(2, path.getList(".").size());

	}

	@Test
	void testGetEngagementCounts() {
		given().when().get("/api/artifacts/engagements/count").then().statusCode(200).body("1111", equalTo(2));
	}

	@Test
	void testGetArtifactsByUnknownEngagement() {

		JsonPath path = given().queryParam("engagementUuid", "xxx").when().get("/api/artifacts").then()
				.statusCode(200).extract().jsonPath();
		assertEquals(0, path.getList(".").size());

	}

	@Test
	void testModifyArtifactsMissingArtifactId() {

		Artifact a = mockArtifact("1111");
		a.setTitle(null);
		String requestBody = jsonb.toJson(List.of(a));

		given().contentType(ContentType.JSON).body(requestBody).put("/api/artifacts/engagement/uuid/1111/na").then().statusCode(400);

	}

	@Test
	void testModifyArtifactsMissingDescription() {

		Artifact a = mockArtifact("1111");
		a.setDescription(null);
		String requestBody = jsonb.toJson(List.of(a));

		given().contentType(ContentType.JSON).body(requestBody).put("/api/artifacts/engagement/uuid/1111/na").then().statusCode(400);

	}

	@Test
	void testModifyArtifactsMissingType() {

		Artifact a = mockArtifact("1111");
		a.setType(null);
		String requestBody = jsonb.toJson(List.of(a));

		given().contentType(ContentType.JSON).body(requestBody).put("/api/artifacts/engagement/uuid/1111/na").then().statusCode(400);

	}

	@Test
	void testModifyArtifactsMissingLinkAddress() {

		Artifact a = mockArtifact("1111");
		a.setLinkAddress(null);
		String requestBody = jsonb.toJson(List.of(a));

		given().contentType(ContentType.JSON).body(requestBody).put("/api/artifacts/engagement/uuid/1111/na").then().statusCode(400);

	}

	@Test
	void testModifyArtifactsMissingEngagementUuid() {

		Artifact a = mockArtifact(null);
		String requestBody = jsonb.toJson(List.of(a));

		given().contentType(ContentType.JSON).body(requestBody).put("/api/artifacts/engagement/uuid/1111/na").then().statusCode(200);

	}
	
	@Test
    void testModifyArtifactsByEngagementUuid() {
	    
	 // get existing artifact
        GetListOptions options = new GetListOptions();
        options.setEngagementUuid("1111");
        List<Artifact> artifacts = service.getArtifacts(options);
        assertNotNull(artifacts);
        assertFalse(artifacts.isEmpty());
        Artifact modified = artifacts.get(0);
        modified.setDescription("UPDATED");

        // new artifacts
        Artifact a1 = mockArtifact("1111");
        Artifact a2 = mockArtifact("2222");

        String requestBody = jsonb.toJson(Arrays.asList(a1, a2, modified));

        given().contentType(ContentType.JSON).body(requestBody).put("/api/artifacts/engagement/uuid/1111/na").then().statusCode(200);
        
        JsonPath path = given().queryParam("engagementUuid", "1111").when().get("/api/artifacts").then().statusCode(200).extract().jsonPath();
        
        assertEquals(2, path.getList(".").size());

        String a0Type = path.getString("[0].type");
        String a0Desc = path.getString("[0].description");

        String a1Type = path.getString("[1].type");
        String a1Desc = path.getString("[1].description");

        boolean newTypeFound = "newType".equals(a0Type) || "newType".equals(a1Type);
        boolean descFound = "UPDATED".equals(a0Desc) || "UPDATED".equals(a1Desc);
        assertTrue(newTypeFound && descFound);
	}

	@Test
	void testGetAllTypes() {
		given().when().get("/api/artifacts/types").then().statusCode(200)
				.body("size()", equalTo(2))
				.body("[0]", equalTo("Demo"))
				.body("[1]", equalTo("Multimedia"));
	}

	@Test
	void testGetAllTypesCount() {
		given().when().get("/api/artifacts/types/count").then().statusCode(200)
				.body("size()", equalTo(2))
				.body("[0].type", equalTo("Demo"))
				.body("[0].count", equalTo(1))
				.body("[1].type", equalTo("Multimedia"))
				.body("[1].count", equalTo(1));
	}
	
	Artifact mockArtifact(String engagementUuid) {
		return Artifact.builder().type("newType").title("New Artifact").linkAddress("http://new-artifact")
				.description("a new artifact").engagementUuid(engagementUuid).build();
	}

}
