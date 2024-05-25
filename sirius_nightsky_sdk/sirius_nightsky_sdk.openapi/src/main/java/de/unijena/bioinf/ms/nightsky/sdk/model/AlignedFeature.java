/*
 * SIRIUS Nightsky API
 * REST API that provides the full functionality of SIRIUS and its web services as background service. It is intended as entry-point for scripting languages and software integration SDKs.This API is exposed by SIRIUS 6.0.0-SNAPSHOT
 *
 * The version of the OpenAPI document: 2.1
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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.unijena.bioinf.ms.nightsky.sdk.model.FeatureAnnotations;
import de.unijena.bioinf.ms.nightsky.sdk.model.MsData;
import java.util.LinkedHashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * The AlignedFeature contains the ID of a feature (aligned over runs) together with some read-only information  that might be displayed in some summary view.
 */
@JsonPropertyOrder({
  AlignedFeature.JSON_PROPERTY_ALIGNED_FEATURE_ID,
  AlignedFeature.JSON_PROPERTY_COMPOUND_ID,
  AlignedFeature.JSON_PROPERTY_NAME,
  AlignedFeature.JSON_PROPERTY_ION_MASS,
  AlignedFeature.JSON_PROPERTY_CHARGE,
  AlignedFeature.JSON_PROPERTY_DETECTED_ADDUCTS,
  AlignedFeature.JSON_PROPERTY_RT_START_SECONDS,
  AlignedFeature.JSON_PROPERTY_RT_END_SECONDS,
  AlignedFeature.JSON_PROPERTY_QUALITY,
  AlignedFeature.JSON_PROPERTY_MS_DATA,
  AlignedFeature.JSON_PROPERTY_TOP_ANNOTATIONS,
  AlignedFeature.JSON_PROPERTY_TOP_ANNOTATIONS_DE_NOVO,
  AlignedFeature.JSON_PROPERTY_COMPUTING
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class AlignedFeature {
  public static final String JSON_PROPERTY_ALIGNED_FEATURE_ID = "alignedFeatureId";
  private String alignedFeatureId;

  public static final String JSON_PROPERTY_COMPOUND_ID = "compoundId";
  private String compoundId;

  public static final String JSON_PROPERTY_NAME = "name";
  private String name;

  public static final String JSON_PROPERTY_ION_MASS = "ionMass";
  private Double ionMass;

  public static final String JSON_PROPERTY_CHARGE = "charge";
  private Integer charge;

  public static final String JSON_PROPERTY_DETECTED_ADDUCTS = "detectedAdducts";
  private Set<String> detectedAdducts = new LinkedHashSet<>();

  public static final String JSON_PROPERTY_RT_START_SECONDS = "rtStartSeconds";
  private Double rtStartSeconds;

  public static final String JSON_PROPERTY_RT_END_SECONDS = "rtEndSeconds";
  private Double rtEndSeconds;

  /**
   * Quality of this feature.
   */
  public enum QualityEnum {
    NOT_APPLICABLE("NOT_APPLICABLE"),
    
    LOWEST("LOWEST"),
    
    BAD("BAD"),
    
    DECENT("DECENT"),
    
    GOOD("GOOD");

    private String value;

    QualityEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static QualityEnum fromValue(String value) {
      for (QualityEnum b : QualityEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_QUALITY = "quality";
  private QualityEnum quality;

  public static final String JSON_PROPERTY_MS_DATA = "msData";
  private MsData msData;

  public static final String JSON_PROPERTY_TOP_ANNOTATIONS = "topAnnotations";
  private FeatureAnnotations topAnnotations;

  public static final String JSON_PROPERTY_TOP_ANNOTATIONS_DE_NOVO = "topAnnotationsDeNovo";
  private FeatureAnnotations topAnnotationsDeNovo;

  public static final String JSON_PROPERTY_COMPUTING = "computing";
  private Boolean computing;

  public AlignedFeature() {
  }

  public AlignedFeature alignedFeatureId(String alignedFeatureId) {
    
    this.alignedFeatureId = alignedFeatureId;
    return this;
  }

   /**
   * Get alignedFeatureId
   * @return alignedFeatureId
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ALIGNED_FEATURE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getAlignedFeatureId() {
    return alignedFeatureId;
  }


  @JsonProperty(JSON_PROPERTY_ALIGNED_FEATURE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAlignedFeatureId(String alignedFeatureId) {
    this.alignedFeatureId = alignedFeatureId;
  }


  public AlignedFeature compoundId(String compoundId) {
    
    this.compoundId = compoundId;
    return this;
  }

   /**
   * Get compoundId
   * @return compoundId
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_COMPOUND_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getCompoundId() {
    return compoundId;
  }


  @JsonProperty(JSON_PROPERTY_COMPOUND_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCompoundId(String compoundId) {
    this.compoundId = compoundId;
  }


  public AlignedFeature name(String name) {
    
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getName() {
    return name;
  }


  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setName(String name) {
    this.name = name;
  }


  public AlignedFeature ionMass(Double ionMass) {
    
    this.ionMass = ionMass;
    return this;
  }

   /**
   * Get ionMass
   * @return ionMass
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ION_MASS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getIonMass() {
    return ionMass;
  }


  @JsonProperty(JSON_PROPERTY_ION_MASS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setIonMass(Double ionMass) {
    this.ionMass = ionMass;
  }


  public AlignedFeature charge(Integer charge) {
    
    this.charge = charge;
    return this;
  }

   /**
   * Get charge
   * @return charge
  **/
  @jakarta.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_CHARGE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public Integer getCharge() {
    return charge;
  }


  @JsonProperty(JSON_PROPERTY_CHARGE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setCharge(Integer charge) {
    this.charge = charge;
  }


  public AlignedFeature detectedAdducts(Set<String> detectedAdducts) {
    
    this.detectedAdducts = detectedAdducts;
    return this;
  }

  public AlignedFeature addDetectedAdductsItem(String detectedAdductsItem) {
    if (this.detectedAdducts == null) {
      this.detectedAdducts = new LinkedHashSet<>();
    }
    this.detectedAdducts.add(detectedAdductsItem);
    return this;
  }

   /**
   * Get detectedAdducts
   * @return detectedAdducts
  **/
  @jakarta.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_DETECTED_ADDUCTS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public Set<String> getDetectedAdducts() {
    return detectedAdducts;
  }


  @JsonDeserialize(as = LinkedHashSet.class)
  @JsonProperty(JSON_PROPERTY_DETECTED_ADDUCTS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setDetectedAdducts(Set<String> detectedAdducts) {
    this.detectedAdducts = detectedAdducts;
  }


  public AlignedFeature rtStartSeconds(Double rtStartSeconds) {
    
    this.rtStartSeconds = rtStartSeconds;
    return this;
  }

   /**
   * Get rtStartSeconds
   * @return rtStartSeconds
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_RT_START_SECONDS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getRtStartSeconds() {
    return rtStartSeconds;
  }


  @JsonProperty(JSON_PROPERTY_RT_START_SECONDS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setRtStartSeconds(Double rtStartSeconds) {
    this.rtStartSeconds = rtStartSeconds;
  }


  public AlignedFeature rtEndSeconds(Double rtEndSeconds) {
    
    this.rtEndSeconds = rtEndSeconds;
    return this;
  }

   /**
   * Get rtEndSeconds
   * @return rtEndSeconds
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_RT_END_SECONDS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getRtEndSeconds() {
    return rtEndSeconds;
  }


  @JsonProperty(JSON_PROPERTY_RT_END_SECONDS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setRtEndSeconds(Double rtEndSeconds) {
    this.rtEndSeconds = rtEndSeconds;
  }


  public AlignedFeature quality(QualityEnum quality) {
    
    this.quality = quality;
    return this;
  }

   /**
   * Quality of this feature.
   * @return quality
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_QUALITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public QualityEnum getQuality() {
    return quality;
  }


  @JsonProperty(JSON_PROPERTY_QUALITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setQuality(QualityEnum quality) {
    this.quality = quality;
  }


  public AlignedFeature msData(MsData msData) {
    
    this.msData = msData;
    return this;
  }

   /**
   * Get msData
   * @return msData
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MS_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public MsData getMsData() {
    return msData;
  }


  @JsonProperty(JSON_PROPERTY_MS_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMsData(MsData msData) {
    this.msData = msData;
  }


  public AlignedFeature topAnnotations(FeatureAnnotations topAnnotations) {
    
    this.topAnnotations = topAnnotations;
    return this;
  }

   /**
   * Get topAnnotations
   * @return topAnnotations
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TOP_ANNOTATIONS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public FeatureAnnotations getTopAnnotations() {
    return topAnnotations;
  }


  @JsonProperty(JSON_PROPERTY_TOP_ANNOTATIONS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTopAnnotations(FeatureAnnotations topAnnotations) {
    this.topAnnotations = topAnnotations;
  }


  public AlignedFeature topAnnotationsDeNovo(FeatureAnnotations topAnnotationsDeNovo) {
    
    this.topAnnotationsDeNovo = topAnnotationsDeNovo;
    return this;
  }

   /**
   * Get topAnnotationsDeNovo
   * @return topAnnotationsDeNovo
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TOP_ANNOTATIONS_DE_NOVO)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public FeatureAnnotations getTopAnnotationsDeNovo() {
    return topAnnotationsDeNovo;
  }


  @JsonProperty(JSON_PROPERTY_TOP_ANNOTATIONS_DE_NOVO)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTopAnnotationsDeNovo(FeatureAnnotations topAnnotationsDeNovo) {
    this.topAnnotationsDeNovo = topAnnotationsDeNovo;
  }


  public AlignedFeature computing(Boolean computing) {
    
    this.computing = computing;
    return this;
  }

   /**
   * Write lock for this feature. If the feature is locked no write operations are possible.  True if any computation is modifying this feature or its results
   * @return computing
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_COMPUTING)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isComputing() {
    return computing;
  }


  @JsonProperty(JSON_PROPERTY_COMPUTING)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setComputing(Boolean computing) {
    this.computing = computing;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AlignedFeature alignedFeature = (AlignedFeature) o;
    return Objects.equals(this.alignedFeatureId, alignedFeature.alignedFeatureId) &&
        Objects.equals(this.compoundId, alignedFeature.compoundId) &&
        Objects.equals(this.name, alignedFeature.name) &&
        Objects.equals(this.ionMass, alignedFeature.ionMass) &&
        Objects.equals(this.charge, alignedFeature.charge) &&
        Objects.equals(this.detectedAdducts, alignedFeature.detectedAdducts) &&
        Objects.equals(this.rtStartSeconds, alignedFeature.rtStartSeconds) &&
        Objects.equals(this.rtEndSeconds, alignedFeature.rtEndSeconds) &&
        Objects.equals(this.quality, alignedFeature.quality) &&
        Objects.equals(this.msData, alignedFeature.msData) &&
        Objects.equals(this.topAnnotations, alignedFeature.topAnnotations) &&
        Objects.equals(this.topAnnotationsDeNovo, alignedFeature.topAnnotationsDeNovo) &&
        Objects.equals(this.computing, alignedFeature.computing);
  }

  @Override
  public int hashCode() {
    return Objects.hash(alignedFeatureId, compoundId, name, ionMass, charge, detectedAdducts, rtStartSeconds, rtEndSeconds, quality, msData, topAnnotations, topAnnotationsDeNovo, computing);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AlignedFeature {\n");
    sb.append("    alignedFeatureId: ").append(toIndentedString(alignedFeatureId)).append("\n");
    sb.append("    compoundId: ").append(toIndentedString(compoundId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    ionMass: ").append(toIndentedString(ionMass)).append("\n");
    sb.append("    charge: ").append(toIndentedString(charge)).append("\n");
    sb.append("    detectedAdducts: ").append(toIndentedString(detectedAdducts)).append("\n");
    sb.append("    rtStartSeconds: ").append(toIndentedString(rtStartSeconds)).append("\n");
    sb.append("    rtEndSeconds: ").append(toIndentedString(rtEndSeconds)).append("\n");
    sb.append("    quality: ").append(toIndentedString(quality)).append("\n");
    sb.append("    msData: ").append(toIndentedString(msData)).append("\n");
    sb.append("    topAnnotations: ").append(toIndentedString(topAnnotations)).append("\n");
    sb.append("    topAnnotationsDeNovo: ").append(toIndentedString(topAnnotationsDeNovo)).append("\n");
    sb.append("    computing: ").append(toIndentedString(computing)).append("\n");
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

