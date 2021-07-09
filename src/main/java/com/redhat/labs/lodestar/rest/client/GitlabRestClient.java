package com.redhat.labs.lodestar.rest.client;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.apache.http.NoHttpResponseException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.model.gitlab.File;

@Retry(maxRetries = 5, delay = 1200, retryOn = NoHttpResponseException.class, abortOn = WebApplicationException.class)
@Path("/api/v4")
@RegisterRestClient(configKey = "gitlab.api")
@RegisterClientHeaders(GitlabTokenFactory.class)
@RegisterProvider(value = GitLabApiExceptionMapper.class, priority = 50)
@Produces("application/json")
public interface GitlabRestClient {

    /*
     * Files
     */

    @PUT
    @Path("/projects/{id}/repository/files/{file_path}")
    @Produces("application/json")
    File updateFile(@PathParam("id") @Encoded Long projectId, @PathParam("file_path") @Encoded String filePath,
            File file);

    @GET
    @Path("/projects/{id}/repository/files/{file_path}")
    @Produces("application/json")
    File getFile(@PathParam("id") @Encoded Long projectId, @PathParam("file_path") @Encoded String filePath,
            @QueryParam("ref") @Encoded String ref);

}
