package com.redhat.labs.lodestar.artifacts.model;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactCount {

    @Builder.Default
    private Long count = 0l;
    private String type;
    @Builder.Default
    private Optional<String> engagementId = Optional.empty();

}
