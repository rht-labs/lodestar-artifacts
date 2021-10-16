package com.redhat.labs.lodestar.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.redhat.labs.lodestar.model.gitlab.Action;
import com.redhat.labs.lodestar.model.gitlab.Commit;
import io.quarkus.panache.common.Sort;
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
    String artifactsFile;

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

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
    public long refresh() {
        engagementRestClient.getAllEngagements().parallelStream().forEach(this::reloadFromGitlabByEngagement);
        return countArtifacts(new GetOptions()).getCount();
    }

    /**
     * Returns a {@link List} of {@link Artifact}s for the given {@link File}. An
     * empty {@link List} is returned if {@link File} or its contents are null or
     * blank.
     * 
     * @param engagement engagement to reload artifacts
     * @return
     */
    void reloadFromGitlabByEngagement(Engagement engagement) {
        if(engagement.getUuid() == null) {
            LOGGER.error("Engagement found with no uuid. Check description of project {}", engagement.getProjectId());
            return;
        }
        
        try {
            File file = gitlabRestClient.getFile(engagement.getProjectId(), artifactsFile, defaultBranch);
            
            if(null == file.getContent() || file.getContent().isBlank()) {
                LOGGER.error("IMPOSSIBLE. NO FILE DATA FROM GITLAB FOR PROJECT {}. THIS SHALL NOT STAND", engagement.getProjectId());
                return;
            }
            
            file.decodeFileAttributes();
            LOGGER.trace("Gitlab file {}", file.getContent());
            List<Artifact> artifacts = Arrays.asList(jsonb.fromJson(file.getContent(), Artifact[].class));
            
            artifacts.forEach(a -> {
                a.setEngagementUuid(engagement.getUuid());
             // set uuid if missing
                if (null == a.getUuid()) {
                    a.setUuid(UUID.randomUUID().toString());
                }
                
             // persist the artifact
                createOrUpdateArtifact(a);
            });
            
        } catch(WebApplicationException wae) {
            if(wae.getResponse().getStatus() != 404) {
                throw wae;
            }
            LOGGER.error("NO FILE DATA FROM GITLAB FOR PROJECT {}. THIS SHALL NOT STAND", engagement.getProjectId());
        }
    }
    
    public void updateArtifacts(String engagementUuid, String region, List<Artifact> requestArtifacts, Optional<String> authorEmail, Optional<String> authorName) {
        
        for(Artifact artifact : requestArtifacts) {
            if (null == artifact.getUuid()) {
                artifact.setUuid(UUID.randomUUID().toString());
            }
            
            if(artifact.getEngagementUuid() == null) {
                artifact.setEngagementUuid(engagementUuid);
            }
            artifact.setRegion(region);
        }
        
        List<Artifact> existing = Artifact.findAllByEngagementUuid(engagementUuid);
        
        Diff diff = JAVERS.compareCollections(existing, requestArtifacts, Artifact.class);
        
        if(diff.hasChanges()) {
            
            StringBuilder commitMessage = new StringBuilder(defaultCommitMessage);

            diff.groupByObject().stream().filter(c -> c.getGlobalId().value().contains("Artifact")).forEach(cbo -> {

                // process the change  create/update/delete artifacts in database
                processObjectChange(cbo, requestArtifacts);

                commitMessage.append(cbo.toString());
                updateArtifactsFile(engagementUuid, authorEmail.orElse(defaultAuthorEmail), authorName.orElse(defaultAuthorName), Optional.ofNullable(commitMessage.toString()));
                if(existing.size() != requestArtifacts.size()) {
                    engagementRestClient.updateEngagement(engagementUuid, requestArtifacts.size());
                }
            });
        }
        
    }

    /**
     * Returns a {@link List} of {@link Artifact}s matching the specified
     * {@link GetListOptions}.
     * 
     * @param options
     * @return
     */
    public List<Artifact> getArtifacts(GetListOptions options) {
        
        if(!options.getRegion().isEmpty() && options.getType().isPresent()) { //by region and type
            return Artifact.pagedArtifactsByRegionAndType(options.getType().orElse(""), options.getRegion(), options.getPage(),
                    options.getPageSize(), options.getQuerySort());
        }
        
        if(!options.getRegion().isEmpty()) { //by region
            return Artifact.pagedArtifactsByRegion(options.getRegion(), options.getPage(), options.getPageSize(), options.getQuerySort());
        }
        
        if(options.getType().isPresent()) { //by type
            checkEngagementUuid(options.getEngagementUuid());
            return Artifact.pagedArtifactsByType(options.getType().orElse(""), options.getPage(), options.getPageSize(), options.getQuerySort());
        }

        Optional<String> engagementUuid = options.getEngagementUuid();

        return engagementUuid.isPresent()
                ? Artifact.pagedArtifactsByEngagementUuid(engagementUuid.get(), options.getPage(),
                        options.getPageSize(), options.getQuerySort(Sort.descending("modified"))) //by uuid
                : Artifact.pagedArtifacts(options.getPage(), options.getPageSize(), options.getQuerySort(Sort.descending("modified").and("engagementUuid"))); //all

    }

    public List<Artifact> getArtifactsByEngagement(String engagementUuid) {
        return Artifact.pagedArtifactsByEngagementUuid(engagementUuid, 0, 1000, Sort.descending("modified"));
    }

    public List<ArtifactCount> getArtifactTypeSummary(List<String> regions) {
        return Artifact.countArtifactsForEachRegionAndType(regions);
    }
    
    private void checkEngagementUuid(Optional<String> engagementUuid) {

        if(engagementUuid.isPresent()) {
            throw new WebApplicationException("Type and engagement together is not supported", 400);
        }
    }

    /**
     * Returns a {@link ArtifactCount} with the count of {@link Artifact}s matching
     * the specified {@link GetOptions}.
     * 
     * @param options
     * @return
     */
    public ArtifactCount countArtifacts(GetOptions options) {
        String type = options.getType().orElse("");

        Optional<String> engagementUuid = options.getEngagementUuid();

        ArtifactCount count;

        if(!options.getRegion().isEmpty() && options.getType().isPresent()) {
            count = Artifact.countArtifactsByRegionAndType(type, options.getRegion());
        } else if(!options.getRegion().isEmpty()) {
            count = Artifact.countArtifactsByRegion(options.getRegion());
        } else if(options.getType().isPresent()) {
            count = Artifact.countArtifactsByType(type);
        } else if(engagementUuid.isPresent()) {
            count = Artifact.countArtifactsByEngagementUuid(engagementUuid.get());
        } else {
            count = Artifact.countAllArtifacts();
        }

        return count;

    }

    public Map<String, Long> getEngagementCounts() {
        Map<String, Long> countMap = new HashMap<>();
        Artifact.countArtifactsForEachEngagement().forEach(e -> countMap.put(e.getType(), e.getCount()));
        return countMap;
    }

    /**
     * Removes the {@link Artifact} from the database if the object has been
     * removed. Otherwise, creates or updates the {@link Artifact}.
     * 
     * @param cbo changes
     * @param incoming update artifacts
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
     * Creates or updates the {@link Artifact} in the database.
     * 
     * @param artifact
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
        
        if(artifact.getCreated() == null) { //refresh will have created dates
            artifact.setCreated(now);
        }
        
        if(artifact.getModified() == null) { //in the unusual case that modified is not set but create was already set
            artifact.setModified(artifact.getCreated());
        }

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
     * @param authorEmail
     * @param authorName
     * @param commitMessage
     */
    public void updateArtifactsFile(String engagementUuid, String authorEmail,
            String authorName, Optional<String> commitMessage) {

        // find project by engagement
        Engagement project = engagementRestClient.getEngagementByUuid(engagementUuid);

        List<Artifact> artifacts = Artifact.findAllByEngagementUuid(engagementUuid);
        String content = jsonb.toJson(artifacts);

        List<Action> actions = List.of(
                Action.builder().filePath(artifactsFile).content(content).build(),
                createLegacyEngagementAction(project.getProjectId(), content)
        );

        Commit commit = Commit.builder().commitMessage(commitMessage.orElse("Artifact Update")).branch(defaultBranch)
                .authorEmail(authorEmail).authorName(authorName).actions(actions).build();

        // update in git
        gitlabRestClient.createCommit(project.getProjectId(), commit);
    }

    Action createLegacyEngagementAction(long projectId, String artifactContent) {
        File f = gitlabRestClient.getFile(projectId, "engagement.json", defaultBranch);
        f.decodeFileAttributes();

        JsonElement element = gson.fromJson(f.getContent(), JsonElement.class);
        JsonObject engagement = element.getAsJsonObject();

        element = gson.fromJson(artifactContent, JsonElement.class);

        engagement.add("artifacts", element);
        JsonObject sorted = new JsonObject();
        engagement.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(es -> sorted.add(es.getKey(), es.getValue()));

        return Action.builder().filePath("engagement.json").content(gson.toJson(sorted)).build();

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
