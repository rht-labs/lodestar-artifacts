package com.redhat.labs.lodestar.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.javers.core.ChangesByObject;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.ArtifactCount;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.GetListOptions;
import com.redhat.labs.lodestar.model.GetOptions;
import com.redhat.labs.lodestar.model.gitlab.File;
import com.redhat.labs.lodestar.rest.client.EngagementApiRestClient;
import com.redhat.labs.lodestar.rest.client.GitlabRestClient;

@ApplicationScoped
public class ArtifactService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactService.class);

    @ConfigProperty(name = "artifacts.file", defaultValue = "artifacts.json")
    private String artifactsFile;

    @ConfigProperty(name = "default.branch")
    String defaultBranch;

    @ConfigProperty(name = "default.commit.message")
    String defaultCommitMessage;

    @ConfigProperty(name = "default.author.name")
    String defaultAuthorName;

    @ConfigProperty(name = "default.author.email")
    String defaultAuthorEmail;

    @Inject
    @RestClient
    GitlabRestClient gitlabRestClient;

    @Inject
    @RestClient
    EngagementApiRestClient engagementRestClient;

    @Inject
    Jsonb jsonb;

    private static final Javers JAVERS = JaversBuilder.javers()
            .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE).build();

    /**
     * Remove all {@link Artifact}s from the database.
     */
    public void purge() {
        Artifact.removeAllArtifacts();
    }

    /**
     * Fetches all {@link Artifact}s from all projects in the configured Git group
     * and inserts into the database.
     */
    public void refresh() {

        engagementRestClient.getAllEngagementProjects().stream().map(Engagement::getProjectId)
                .map(this::getArtifactsFromGitlabByProjectId).flatMap(Collection::stream)
                .forEach(a -> {

                    // set uuid if missing
                    if (null == a.getUuid()) {
                        a.setUuid(UUID.randomUUID().toString());
                    }

                    // persist the artifact
                    createOrUpdateArtifact(a);

                });

    }

    /**
     * Returns a {@link List} of {@link Artifact}s for the given {@link File}. An
     * empty {@link List} is returned if {@link File} or its contents are null or
     * blank.
     * 
     * @param projectId
     * @return
     */
    List<Artifact> getArtifactsFromGitlabByProjectId(long projectId) {
        try {
            File file = gitlabRestClient.getFile(projectId, artifactsFile, defaultBranch);
            if(null == file.getContent() || file.getContent().isBlank()) {
                LOGGER.error("NO FILE DATA FROM GITLAB FOR PROJECT {}. THIS SHALL NOT STAND", projectId);
                return Collections.emptyList();
            }
            
            file.decodeFileAttributes();
            return Arrays.asList(jsonb.fromJson(file.getContent(), Artifact[].class));
            
        } catch(WebApplicationException wae) {
            if(wae.getResponse().getStatus() != 404) {
                throw wae;
            }
            return Collections.emptyList();
        }
    }

    /**
     * Processes the given {@link List} of {@link Artifact}s. The artifacts will be
     * split based on engagement uuid. Each {@link List} of engagement
     * {@link Artifact}s will be updated in both the database and Git.
     * 
     * @param artifacts
     * @param authorEmail
     * @param authorName
     */
    public void process(List<Artifact> artifacts, Optional<String> authorEmail, Optional<String> authorName) {

        // split by engagement uuid and process each resulting artifact list
        processArtifacts(artifacts).entrySet().parallelStream().forEach(e -> {

            // modify artifacts in db
            String changeLog = modifyArtifactsByEngagementUuid(e.getKey(), e.getValue());
            // create commit message
            String commitMessage = new StringBuilder("Changed Engagement Artifacts\n").append(changeLog).toString();

            // pull latest artifacts from db
            List<Artifact> latest = Artifact.findAllByEngagementUuid(e.getKey());
            // send to git to update file
            updateArtifactsFile(e.getKey(), latest, authorEmail, authorName, Optional.ofNullable(commitMessage));

        });

    }

    /**
     * Returns a {@link List} of {@link Artifact}s matching the specified
     * {@link GetListOptions}.
     * 
     * @param options
     * @return
     */
    public List<Artifact> getArtifacts(GetListOptions options) {
        
        if(options.getType().isPresent()) {
            return getArtifactsByType(options);
        }

        Optional<String> engagementUuid = options.getEngagementUuid();

        return engagementUuid.isPresent()
                ? Artifact.pagedArtifactsByEngagementUuid(engagementUuid.get(), options.getPage(),
                        options.getPageSize())
                : Artifact.pagedArtifacts(options.getPage(), options.getPageSize());

    }
    
    public List<Artifact> getArtifactsByType(GetListOptions options) {
        
        if(options.getEngagementUuid().isPresent()) {
            throw new WebApplicationException("Type and engagement together is not supported", 400);
        }

        return Artifact.pagedArtifactsByType(options.getType().get(), options.getPage(), options.getPageSize());

    }

    /**
     * Returns a {@link ArtifactCount} with the count of {@link Artifact}s matching
     * the specified {@link GetOptions}.
     * 
     * @param options
     * @return
     */
    public ArtifactCount countArtifacts(GetOptions options) {

        Optional<String> engagementUuid = options.getEngagementUuid();
        
        ArtifactCount count;
        
        if(options.getType().isPresent()) {
            count = Artifact.countArtifactsByType(options.getType().get());
        } else if(engagementUuid.isPresent()) {
            count = Artifact.countArtifactsByEngagementUuid(engagementUuid.get());
        } else {
            count = Artifact.countAllArtifacts();
        }
        
        return count;

    }

    /**
     * Returns a {@link Map} of {@link List}s for each {@link Artifact} for each
     * unique Engagement ID.
     * 
     * @param artifacts
     * @return
     */
    Map<String, List<Artifact>> processArtifacts(List<Artifact> artifacts) {
        return artifacts.stream().map(a -> {
            if (null == a.getUuid()) {
                a.setUuid(UUID.randomUUID().toString());
            }
            return a;
        }).collect(Collectors.groupingBy(Artifact::getEngagementUuid));
    }

    /**
     * Modifies the given {@link List} of {@link Artifact}s for the given engagement
     * UUID. Returns a {@link String} containing the change log summary.
     * 
     * @param engagementUuid
     * @param artifacts
     * @return
     */
    String modifyArtifactsByEngagementUuid(String engagementUuid, List<Artifact> artifacts) {

        StringBuilder changeLog = new StringBuilder();

        // compare incoming with database
        Diff diff = compareArtifactsWithDatabase(engagementUuid, artifacts);

        // group by object
        List<ChangesByObject> l = diff.groupByObject();

        // create/update/delete artifacts in database
        l.stream().filter(c -> c.getGlobalId().value().contains("Artifact")).forEach(cbo -> {

            // process the change
            processObjectChange(cbo, artifacts);

            changeLog.append(cbo.toString());

        });

        return changeLog.toString();

    }

    /**
     * Removes the {@link Artifact} from the database if the object has been
     * removed. Otherwise, creates or updates the {@link Artifact}.
     * 
     * @param cbo
     * @param incoming
     */
    void processObjectChange(ChangesByObject cbo, List<Artifact> incoming) {

        // get artifact uuid from global id
        String globalId = cbo.getGlobalId().value();
        String aUuid = globalId.substring(globalId.indexOf("/") + 1);

        if (!cbo.getObjectsRemoved().isEmpty()) {

            Artifact.deleteByUuid(aUuid);

        } else {

            // find artifact in incoming list and create or update in database
            incoming.stream().filter(a -> aUuid.equals(a.getUuid())).findAny().ifPresent(this::createOrUpdateArtifact);

        }

    }

    /**
     * Returns a {@link Diff} from comparing the provided {@link List} of
     * {@link Artifact}s with the current state in the database for the given
     * Engagement UUID.
     * 
     * @param engagementUuid
     * @param artifacts
     * @return
     */
    Diff compareArtifactsWithDatabase(String engagementUuid, List<Artifact> artifacts) {

        // get list from database by uuid
        List<Artifact> existing = Artifact.findAllByEngagementUuid(engagementUuid);

        // compare old and new lists
        return JAVERS.compareCollections(existing, artifacts, Artifact.class);

    }

    /**
     * Creates or updates the {@link Artifact} in the database.
     * 
     * @param artifact
     * @return
     */
    void createOrUpdateArtifact(Artifact artifact) {

        Optional<Artifact> persisted = Artifact.findByUuid(artifact.getUuid());
        if (persisted.isPresent()) {
            updateArtifact(artifact, persisted.get());
        } else {
            createArtifact(artifact);
        }

    }

    /**
     * Sets any required attributes on the {@link Artifact} and inserts into the
     * database.
     * 
     * @param artifact
     */
    void createArtifact(Artifact artifact) {

        String now = getNowAsZulu();

        if (null == artifact.getUuid()) {
            artifact.setUuid(UUID.randomUUID().toString());
        }
        artifact.setCreated(now);
        artifact.setModified(now);

        artifact.persist();

    }

    /**
     * Sets any required attributes on the {@link Artifact} and updates in the
     * database.
     * 
     * @param artifact
     */
    void updateArtifact(Artifact artifact, Artifact existing) {

        artifact.setId(existing.getId());
        artifact.setCreated(existing.getCreated());
        artifact.setModified(getNowAsZulu());

        artifact.update();

    }

    /**
     * Updates the artifacts file in GitLab with the given {@link List} of
     * {@link Artifact}s.
     * 
     * @param engagementUuid
     * @param artifacts
     * @param authorEmail
     * @param authorName
     * @param commitMessage
     */
    void updateArtifactsFile(String engagementUuid, List<Artifact> artifacts, Optional<String> authorEmail,
            Optional<String> authorName, Optional<String> commitMessage) {

        // find project by engagement
        Engagement project = engagementRestClient.getEngagementProjectByUuid(engagementUuid);

        // create json content
        String content = jsonb.toJson(artifacts);

        // create
        File file = createArtifactsFile(content, defaultBranch, authorName, authorEmail, commitMessage);

        // update in git
        gitlabRestClient.updateFile(project.getProjectId(), artifactsFile, file);

    }

    /**
     * Creates and returns a {@link File} with the given parameters and encodes the
     * content.
     * 
     * @param content
     * @param branch
     * @param authorName
     * @param authorEmail
     * @param commitMessage
     * @return
     */
    File createArtifactsFile(String content, String branch, Optional<String> authorName, Optional<String> authorEmail,
            Optional<String> commitMessage) {

        // create file
        File artifactFile = File.builder().filePath(artifactsFile).content(content)
                .authorEmail(authorEmail.orElse(defaultAuthorEmail)).authorName(authorName.orElse(defaultAuthorName))
                .branch(branch).commitMessage(commitMessage.orElse(defaultCommitMessage)).build();

        // encode before sending
        artifactFile.encodeFileAttributes();

        return artifactFile;

    }

    /**
     * Returns a {@link String} representing the current Zulu time.
     * 
     * @return
     */
    String getNowAsZulu() {
        return LocalDateTime.now(ZoneId.of("Z")).toString();
    }

}
