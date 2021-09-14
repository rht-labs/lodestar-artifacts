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
@Path("/api/v2/engagements")
public interface EngagementApiRestClient {

    @GET
    @Path("{uuid}")
    Engagement getEngagementByUuid(@PathParam("uuid") String engagementUuid);

    @GET
    List<Engagement> getAllEngagements();

}
