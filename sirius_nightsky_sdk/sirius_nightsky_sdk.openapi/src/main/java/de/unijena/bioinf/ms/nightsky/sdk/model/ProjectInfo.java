/*
 * SIRIUS Nightsky API
 * REST API that provides the full functionality of SIRIUS and its web services as background service. It is intended as entry-point for scripting languages and software integration SDKs.This API is exposed by SIRIUS 6.0.0-SNAPSHOT
 *
 * The version of the OpenAPI document: 2.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package de.unijena.bioinf.ms.nightsky.sdk.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 
 */
@JsonPropertyOrder({
  ProjectInfo.JSON_PROPERTY_PROJECT_ID,
  ProjectInfo.JSON_PROPERTY_LOCATION,
  ProjectInfo.JSON_PROPERTY_DESCRIPTION,
  ProjectInfo.JSON_PROPERTY_COMPATIBLE,
  ProjectInfo.JSON_PROPERTY_NUM_OF_FEATURES,
  ProjectInfo.JSON_PROPERTY_NUM_OF_COMPOUNDS,
  ProjectInfo.JSON_PROPERTY_NUM_OF_BYTES
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class ProjectInfo {
  public static final String JSON_PROPERTY_PROJECT_ID = "projectId";
  private String projectId;

  public static final String JSON_PROPERTY_LOCATION = "location";
  private String location;

  public static final String JSON_PROPERTY_DESCRIPTION = "description";
  private String description;

  public static final String JSON_PROPERTY_COMPATIBLE = "compatible";
  private Boolean compatible;

  public static final String JSON_PROPERTY_NUM_OF_FEATURES = "numOfFeatures";
  private Integer numOfFeatures;

  public static final String JSON_PROPERTY_NUM_OF_COMPOUNDS = "numOfCompounds";
  private Integer numOfCompounds;

  public static final String JSON_PROPERTY_NUM_OF_BYTES = "numOfBytes";
  private Long numOfBytes;

  public ProjectInfo() {
  }

  public ProjectInfo projectId(String projectId) {
    
    this.projectId = projectId;
    return this;
  }

   /**
   * a user selected unique name of the project for easy access.
   * @return projectId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PROJECT_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getProjectId() {
    return projectId;
  }


  @JsonProperty(JSON_PROPERTY_PROJECT_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }


  public ProjectInfo location(String location) {
    
    this.location = location;
    return this;
  }

   /**
   * storage location of the project.
   * @return location
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_LOCATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getLocation() {
    return location;
  }


  @JsonProperty(JSON_PROPERTY_LOCATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setLocation(String location) {
    this.location = location;
  }


  public ProjectInfo description(String description) {
    
    this.description = description;
    return this;
  }

   /**
   * Description of this project.
   * @return description
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DESCRIPTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getDescription() {
    return description;
  }


  @JsonProperty(JSON_PROPERTY_DESCRIPTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDescription(String description) {
    this.description = description;
  }


  public ProjectInfo compatible(Boolean compatible) {
    
    this.compatible = compatible;
    return this;
  }

   /**
   * Indicates whether computed results (e.g. fingerprints, compounds classes) are compatible with the backend.  If true project is up-to-date and there are no restrictions regarding usage.  If false project is incompatible and therefore \&quot;read only\&quot; until the incompatible results have been removed. See updateProject endpoint for further information  If NULL the information has not been requested.
   * @return compatible
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_COMPATIBLE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isCompatible() {
    return compatible;
  }


  @JsonProperty(JSON_PROPERTY_COMPATIBLE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCompatible(Boolean compatible) {
    this.compatible = compatible;
  }


  public ProjectInfo numOfFeatures(Integer numOfFeatures) {
    
    this.numOfFeatures = numOfFeatures;
    return this;
  }

   /**
   * Number of features (aligned over runs) in this project. If NULL, information has not been requested (See OptField &#39;sizeInformation&#39;).
   * @return numOfFeatures
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NUM_OF_FEATURES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getNumOfFeatures() {
    return numOfFeatures;
  }


  @JsonProperty(JSON_PROPERTY_NUM_OF_FEATURES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNumOfFeatures(Integer numOfFeatures) {
    this.numOfFeatures = numOfFeatures;
  }


  public ProjectInfo numOfCompounds(Integer numOfCompounds) {
    
    this.numOfCompounds = numOfCompounds;
    return this;
  }

   /**
   * Number of compounds (group of ion identities) in this project. If NULL, Information has not been requested (See OptField &#39;sizeInformation&#39;) or might be unavailable for this project type.
   * @return numOfCompounds
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NUM_OF_COMPOUNDS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getNumOfCompounds() {
    return numOfCompounds;
  }


  @JsonProperty(JSON_PROPERTY_NUM_OF_COMPOUNDS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNumOfCompounds(Integer numOfCompounds) {
    this.numOfCompounds = numOfCompounds;
  }


  public ProjectInfo numOfBytes(Long numOfBytes) {
    
    this.numOfBytes = numOfBytes;
    return this;
  }

   /**
   * Size in Bytes this project consumes on disk If NULL, Information has not been requested (See OptField &#39;sizeInformation&#39;).
   * @return numOfBytes
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NUM_OF_BYTES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getNumOfBytes() {
    return numOfBytes;
  }


  @JsonProperty(JSON_PROPERTY_NUM_OF_BYTES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNumOfBytes(Long numOfBytes) {
    this.numOfBytes = numOfBytes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectInfo projectInfo = (ProjectInfo) o;
    return Objects.equals(this.projectId, projectInfo.projectId) &&
        Objects.equals(this.location, projectInfo.location) &&
        Objects.equals(this.description, projectInfo.description) &&
        Objects.equals(this.compatible, projectInfo.compatible) &&
        Objects.equals(this.numOfFeatures, projectInfo.numOfFeatures) &&
        Objects.equals(this.numOfCompounds, projectInfo.numOfCompounds) &&
        Objects.equals(this.numOfBytes, projectInfo.numOfBytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, location, description, compatible, numOfFeatures, numOfCompounds, numOfBytes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectInfo {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    compatible: ").append(toIndentedString(compatible)).append("\n");
    sb.append("    numOfFeatures: ").append(toIndentedString(numOfFeatures)).append("\n");
    sb.append("    numOfCompounds: ").append(toIndentedString(numOfCompounds)).append("\n");
    sb.append("    numOfBytes: ").append(toIndentedString(numOfBytes)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

