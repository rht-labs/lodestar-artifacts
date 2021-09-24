package com.redhat.labs.lodestar.resource;

import java.util.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.ArtifactCount;
import com.redhat.labs.lodestar.model.GetListOptions;
import com.redhat.labs.lodestar.model.GetOptions;
import com.redhat.labs.lodestar.service.ArtifactService;

@Path("/api/artifacts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Artifacts", description = "Artifact API")
public class ArtifactResource {

    @Inject
    ArtifactService service;
    
    @PUT
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "The list of artifacts  has been processed."),
            @APIResponse(responseCode = "400", description = "Invalid list of artifacts provided.") })
    @Operation(summary = "Artifacts have been processed and persisted for engagement.")
    @Path("/engagement/uuid/{engagementUuid}/{region}")
    public Response processEngagementArtifacts(@Valid List<Artifact> artifacts, @PathParam(value="engagementUuid") String engagementUuid,
            @PathParam(value="region") String region, @QueryParam("authorEmail") Optional<String> authorEmail,
            @QueryParam("authorName") Optional<String> authorName) {

        service.updateArtifacts(engagementUuid, region, artifacts, authorEmail, authorName);
        return Response.ok(service.getArtifactsByEngagement(engagementUuid)).build();

    }

    @GET
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Artifacts matching the query options are returned.") })
    @Operation(summary = "List of Artifacts matching options is returned.")
    public Response getArtifacts(@BeanParam GetListOptions options) {

        List<Artifact> artifacts = service.getArtifacts(options);
        ArtifactCount count = service.countArtifacts(options);

        return Response.ok(artifacts).header("x-page", options.getPage()).header("x-per-page", options.getPageSize())
                .header("x-total-artifacts", count.getCount())
                .header("x-total-pages", (count.getCount() / options.getPageSize()) + 1).build();

    }

    @GET
    @Path("/count")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Count of artifacts matching the query options are returned.") })
    @Operation(summary = "Count of artifacts matching options is returned.")
    public ArtifactCount countArtifacts(@BeanParam GetOptions options) {
        return service.countArtifacts(options);
    }

    @GET
    @Path("/types/count")
    public List<ArtifactCount> countArtifactsByType(@QueryParam("regions") List<String> regions) {
        return service.getArtifactTypeSummary(regions);
    }

    @GET
    @Path("engagements/count")
    public Map<String, Long> getEngagementCounts() {
        return service.getEngagementCounts();
    }

    @GET
    @Path("/types")
    public Set<String> getAllTypes(@QueryParam("regions") List<String> regions) {
        List<ArtifactCount> counts = service.getArtifactTypeSummary(regions);
        Set<String> types = new TreeSet<>();
        counts.forEach(type -> types.add(type.getType()));

        return types;
    }

    @PUT
    @Path("/refresh")
    @APIResponses(value = {
            @APIResponse(responseCode = "202", description = "The request was accepted and will be processed.") })
    @Operation(summary = "Refreshes database with data in git, purging first")
    public Response refresh() {

        service.purge();
        long count = service.refresh();

        return Response.accepted().header("x-total-artifacts", count).build();

    }

}
