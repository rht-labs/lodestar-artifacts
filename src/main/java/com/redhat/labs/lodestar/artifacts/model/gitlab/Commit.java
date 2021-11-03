package com.redhat.labs.lodestar.artifacts.model.gitlab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Commit {

    private String branch;
    private String commitMessage;
    private List<Action> actions;
    private String authorName;
    private String authorEmail;
}
