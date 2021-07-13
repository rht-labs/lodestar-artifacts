package com.redhat.labs.lodestar.model;

import javax.json.bind.annotation.JsonbProperty;

import lombok.Data;

@Data
public class Engagement {

    private String uuid;
    
    @JsonbProperty("project_id")
    private long projectId;

}
