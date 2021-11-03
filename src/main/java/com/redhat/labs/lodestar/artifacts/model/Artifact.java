package com.redhat.labs.lodestar.artifacts.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.validation.constraints.NotBlank;

import com.mongodb.client.model.Filters;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.panache.common.Sort;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Sorts.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeName("Artifact")
@EqualsAndHashCode(callSuper = true)
public class Artifact extends PanacheMongoEntityBase {

    private static final String MODIFIED = "modified";

    @BsonId
    @DiffIgnore
    @JsonbTransient
    private ObjectId id;

    @Id
    private String uuid;
    @DiffIgnore
    private String created;
    @DiffIgnore
    @JsonbProperty(value = "updated")
    private String modified;

    private String engagementUuid;
    @NotBlank
    private String title;
    @NotBlank
    private String description;
    @NotBlank
    private String type;
    @NotBlank
    private String linkAddress;
    
    private String region;

    /**
     * Returns an {@link ArtifactCount} containing the count for the total number of
     * {@link Artifact}s in the database.
     * 
     * @return
     */
    public static ArtifactCount countAllArtifacts() {
        return ArtifactCount.builder().count(count()).build();
    }

    /**
     * Returns an {@link ArtifactCount} containing the count for the number of
     * {@link Artifact} matching the engagement uuid.
     * 
     * @param engagementUuid - uuid
     * @return count - number of artifacts for engagement
     */
    public static ArtifactCount countArtifactsByEngagementUuid(String engagementUuid) {
        return ArtifactCount.builder().count(count("engagementUuid", engagementUuid)).build();
    }
    
    /**
     * Returns an {@link ArtifactCount} containing the count for the number of
     * {@link Artifact} matching the type.
     * 
     * @param type - type of artifact
     * @return count
     */
    public static ArtifactCount countArtifactsByType(String type) {
        return ArtifactCount.builder().count(count("type", type)).build();
    }
    
    public static ArtifactCount countArtifactsByRegion(List<String> regions) {
        return ArtifactCount.builder().count(count("region in ?1", regions)).build();
    }
    
    public static ArtifactCount countArtifactsByRegionAndType(String type, List<String> regions) {
        return ArtifactCount.builder().count(count("{ $and: [ {'type':?1}, {'region':{'$in':[?2]}} ] }", type, regions)).build();
    }

    public static List<ArtifactCount> countArtifactsForEachRegionAndType(List<String> regions) {
        String count = "count";
        List<Bson> bson = new ArrayList<>();

        if(!regions.isEmpty()) {
            bson.add(match(Filters.in("region", regions)));
        }

        bson.add(group("$type", sum(count, 1)));
        bson.add(project(fields(include(count), computed("type", "$_id"))));
        bson.add(sort(orderBy(descending(count), ascending("type"))));

        return mongoCollection().aggregate(bson, ArtifactCount.class).into(new ArrayList<>());
    }

    public static List<ArtifactCount> countArtifactsForEachEngagement() {
        String count = "count";
        List<Bson> bson = new ArrayList<>();
        bson.add(group("$engagementUuid", sum(count, 1)));
        bson.add(project(fields(include(count), computed("type", "$_id"))));
        bson.add(sort(orderBy(descending(count), ascending("type"))));

        return mongoCollection().aggregate(bson, ArtifactCount.class).into(new ArrayList<>());
    }

    /**
     * Returns {@link List} of {@link Artifact}s sorted descending on modified
     * timestamp using the page specified.
     * 
     * @param page
     * @param pageSize
     * @return
     */
    public static List<Artifact> pagedArtifacts(int page, int pageSize, Sort sort) {
        return findAll(sort).page(page, pageSize).list();
    }
    
    /**
     * Returns {@link List} of {@link Artifact}s sorted descending on modified
     * timestamp using the page specified where type is specified.
     * 
     * @param page
     * @param pageSize
     * @return
     */
    public static List<Artifact> pagedArtifactsByType(String type, int page, int pageSize, Sort sort) {
        return find("type", sort, type).page(page, pageSize).list();
    }
    
    public static List<Artifact> pagedArtifactsByRegion(List<String> regions, int page, int pageSize, Sort sort) {
        return find("region in ?1", sort, regions).page(page, pageSize).list();
    }
    
    public static List<Artifact> pagedArtifactsByRegionAndType(String type, List<String> regions, int page, int pageSize, Sort sort) {
        //not sure why the commented query doesn't work but keep seeing this error - no viable alternative at input 'type='
        //return find("type = ?1 and region in ?2", Sort.descending(MODIFIED), type, regions).page(page, pageSize).list();
        return find("{ $and: [ {'type':?1}, {'region':{'$in':[?2]}} ] }", sort, type, regions).page(page, pageSize).list();
    }

    /**
     * Returns a {@link List} of {@link Artifact}s for the given engagement uuid,
     * sorted descending by modified timestamp using the page specified.
     * 
     * @param engagementUuid
     * @param page
     * @param pageSize
     * @return
     */
    public static List<Artifact> pagedArtifactsByEngagementUuid(String engagementUuid, int page, int pageSize, Sort sort) {
        return find("engagementUuid", sort, engagementUuid).page(page, pageSize).list();
    }

    /**
     * Returns a {@link List} containing all {@link Artifact}s that match the given
     * engagement uuid.
     * 
     * @param engagementUuid
     * @return
     */
    public static List<Artifact> findAllByEngagementUuid(String engagementUuid) {
        return list("engagementUuid", engagementUuid);
    }

    /**
     * Returns and {@link Optional} containing the {@link Artifact} that matches the
     * given uuid. Otherwise, and empty {@link Optional} is returned.
     * 
     * @param uuid
     * @return
     */
    public static Optional<Artifact> findByUuid(String uuid) {
        return find("uuid", uuid).singleResultOptional();
    }

    /**
     * Returns the number of {@link Artifact}s delete with the given uuid.
     * 
     * @param uuid
     * @return
     */
    public static long deleteByUuid(String uuid) {
        return Artifact.delete("uuid", uuid);
    }

    /**
     * Removes all {@link Artifact}s from the database.
     */
    public static long removeAllArtifacts() {
        return deleteAll();
    }

}
