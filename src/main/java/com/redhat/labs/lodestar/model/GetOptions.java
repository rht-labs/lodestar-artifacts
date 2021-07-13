package com.redhat.labs.lodestar.model;

import java.util.Optional;

import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetOptions {

    @Parameter(name = "engagementUuid", required = false, description = "return only artifacts for the given engagement uuid")
    @QueryParam("engagementUuid")
    private String engagementUuid;
    
    @Parameter(name = "type", required = false, description = "return only artifacts for the given type. Do not use with engagementUuid")
    @QueryParam("type")
    private String type;

    public Optional<String> getEngagementUuid() {
        return Optional.ofNullable(engagementUuid);
    }
    
    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

}
