package com.redhat.labs.lodestar.rest.client;

import javax.ws.rs.*;

import com.redhat.labs.lodestar.model.gitlab.Commit;
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

    @POST
    @Path("/projects/{id}/repository/commits")
    @Produces("application/json")
    void createCommit(@PathParam("id") long projectId, Commit commit);

    @GET
    @Path("/projects/{id}/repository/files/{file_path}")
    @Produces("application/json")
    File getFile(@PathParam("id") @Encoded Long projectId, @PathParam("file_path") @Encoded String filePath,
            @QueryParam("ref") @Encoded String ref);

}
