package com.redhat.labs.lodestar.rest.client;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.model.Engagement;

@Produces("application/json")
@RegisterRestClient(configKey = "engagement.api")
@RegisterProvider(value = GitLabApiExceptionMapper.class, priority = 50)
public interface EngagementApiRestClient {

    @GET
    @Path("/api/v1/engagements/projects/{uuid}")
    Engagement getEngagementProjectByUuid(@PathParam("uuid") String engagementUuid, @QueryParam("mini") boolean mini);

    @GET
    @Path("/api/v1/engagements/projects")
    List<Engagement> getAllEngagementProjects();

}
