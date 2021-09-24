package com.redhat.labs.lodestar.rest.client;

import java.util.List;

import javax.ws.rs.*;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.model.Engagement;

@Produces("application/json")
@RegisterRestClient(configKey = "engagement.api")
@RegisterProvider(value = GitLabApiExceptionMapper.class, priority = 50)
@Path("/api/v2/engagements")
public interface EngagementApiRestClient {

    @GET
    @Path("{uuid}")
    Engagement getEngagementByUuid(@PathParam("uuid") String engagementUuid);

    @GET
    List<Engagement> getAllEngagements();

    @PUT
    @Path("{uuid}/artifacts/{count}")
    void updateEngagement(@PathParam("uuid") String engagementUuid, @PathParam("count") int count);

}
