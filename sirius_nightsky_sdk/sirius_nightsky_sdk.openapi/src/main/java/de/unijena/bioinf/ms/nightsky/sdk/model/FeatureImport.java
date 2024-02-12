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
import de.unijena.bioinf.ms.nightsky.sdk.model.BasicSpectrum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 
 */
@JsonPropertyOrder({
  FeatureImport.JSON_PROPERTY_NAME,
  FeatureImport.JSON_PROPERTY_FEATURE_ID,
  FeatureImport.JSON_PROPERTY_ION_MASS,
  FeatureImport.JSON_PROPERTY_ADDUCT,
  FeatureImport.JSON_PROPERTY_RT_START_SECONDS,
  FeatureImport.JSON_PROPERTY_RT_END_SECONDS,
  FeatureImport.JSON_PROPERTY_MERGED_MS1,
  FeatureImport.JSON_PROPERTY_MS1_SPECTRA,
  FeatureImport.JSON_PROPERTY_MS2_SPECTRA
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class FeatureImport {
  public static final String JSON_PROPERTY_NAME = "name";
  private String name;

  public static final String JSON_PROPERTY_FEATURE_ID = "featureId";
  private String featureId;

  public static final String JSON_PROPERTY_ION_MASS = "ionMass";
  private Double ionMass;

  public static final String JSON_PROPERTY_ADDUCT = "adduct";
  private String adduct;

  public static final String JSON_PROPERTY_RT_START_SECONDS = "rtStartSeconds";
  private Double rtStartSeconds;

  public static final String JSON_PROPERTY_RT_END_SECONDS = "rtEndSeconds";
  private Double rtEndSeconds;

  public static final String JSON_PROPERTY_MERGED_MS1 = "mergedMs1";
  private BasicSpectrum mergedMs1;

  public static final String JSON_PROPERTY_MS1_SPECTRA = "ms1Spectra";
  private List<BasicSpectrum> ms1Spectra = new ArrayList<>();

  public static final String JSON_PROPERTY_MS2_SPECTRA = "ms2Spectra";
  private List<BasicSpectrum> ms2Spectra = new ArrayList<>();

  public FeatureImport() {
  }

  public FeatureImport name(String name) {
    
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @javax.annotation.Nullable
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


  public FeatureImport featureId(String featureId) {
    
    this.featureId = featureId;
    return this;
  }

   /**
   * Get featureId
   * @return featureId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_FEATURE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getFeatureId() {
    return featureId;
  }


  @JsonProperty(JSON_PROPERTY_FEATURE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setFeatureId(String featureId) {
    this.featureId = featureId;
  }


  public FeatureImport ionMass(Double ionMass) {
    
    this.ionMass = ionMass;
    return this;
  }

   /**
   * Get ionMass
   * @return ionMass
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ION_MASS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public Double getIonMass() {
    return ionMass;
  }


  @JsonProperty(JSON_PROPERTY_ION_MASS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setIonMass(Double ionMass) {
    this.ionMass = ionMass;
  }


  public FeatureImport adduct(String adduct) {
    
    this.adduct = adduct;
    return this;
  }

   /**
   * Adduct of this feature. If not know specify [M+?]+ or [M+?]- to define the charge
   * @return adduct
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ADDUCT)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getAdduct() {
    return adduct;
  }


  @JsonProperty(JSON_PROPERTY_ADDUCT)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setAdduct(String adduct) {
    this.adduct = adduct;
  }


  public FeatureImport rtStartSeconds(Double rtStartSeconds) {
    
    this.rtStartSeconds = rtStartSeconds;
    return this;
  }

   /**
   * Get rtStartSeconds
   * @return rtStartSeconds
  **/
  @javax.annotation.Nullable
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


  public FeatureImport rtEndSeconds(Double rtEndSeconds) {
    
    this.rtEndSeconds = rtEndSeconds;
    return this;
  }

   /**
   * Get rtEndSeconds
   * @return rtEndSeconds
  **/
  @javax.annotation.Nullable
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


  public FeatureImport mergedMs1(BasicSpectrum mergedMs1) {
    
    this.mergedMs1 = mergedMs1;
    return this;
  }

   /**
   * Get mergedMs1
   * @return mergedMs1
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MERGED_MS1)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public BasicSpectrum getMergedMs1() {
    return mergedMs1;
  }


  @JsonProperty(JSON_PROPERTY_MERGED_MS1)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMergedMs1(BasicSpectrum mergedMs1) {
    this.mergedMs1 = mergedMs1;
  }


  public FeatureImport ms1Spectra(List<BasicSpectrum> ms1Spectra) {
    
    this.ms1Spectra = ms1Spectra;
    return this;
  }

  public FeatureImport addMs1SpectraItem(BasicSpectrum ms1SpectraItem) {
    if (this.ms1Spectra == null) {
      this.ms1Spectra = new ArrayList<>();
    }
    this.ms1Spectra.add(ms1SpectraItem);
    return this;
  }

   /**
   * Get ms1Spectra
   * @return ms1Spectra
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_MS1_SPECTRA)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public List<BasicSpectrum> getMs1Spectra() {
    return ms1Spectra;
  }


  @JsonProperty(JSON_PROPERTY_MS1_SPECTRA)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setMs1Spectra(List<BasicSpectrum> ms1Spectra) {
    this.ms1Spectra = ms1Spectra;
  }


  public FeatureImport ms2Spectra(List<BasicSpectrum> ms2Spectra) {
    
    this.ms2Spectra = ms2Spectra;
    return this;
  }

  public FeatureImport addMs2SpectraItem(BasicSpectrum ms2SpectraItem) {
    if (this.ms2Spectra == null) {
      this.ms2Spectra = new ArrayList<>();
    }
    this.ms2Spectra.add(ms2SpectraItem);
    return this;
  }

   /**
   * Get ms2Spectra
   * @return ms2Spectra
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_MS2_SPECTRA)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public List<BasicSpectrum> getMs2Spectra() {
    return ms2Spectra;
  }


  @JsonProperty(JSON_PROPERTY_MS2_SPECTRA)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setMs2Spectra(List<BasicSpectrum> ms2Spectra) {
    this.ms2Spectra = ms2Spectra;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeatureImport featureImport = (FeatureImport) o;
    return Objects.equals(this.name, featureImport.name) &&
        Objects.equals(this.featureId, featureImport.featureId) &&
        Objects.equals(this.ionMass, featureImport.ionMass) &&
        Objects.equals(this.adduct, featureImport.adduct) &&
        Objects.equals(this.rtStartSeconds, featureImport.rtStartSeconds) &&
        Objects.equals(this.rtEndSeconds, featureImport.rtEndSeconds) &&
        Objects.equals(this.mergedMs1, featureImport.mergedMs1) &&
        Objects.equals(this.ms1Spectra, featureImport.ms1Spectra) &&
        Objects.equals(this.ms2Spectra, featureImport.ms2Spectra);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, featureId, ionMass, adduct, rtStartSeconds, rtEndSeconds, mergedMs1, ms1Spectra, ms2Spectra);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeatureImport {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    featureId: ").append(toIndentedString(featureId)).append("\n");
    sb.append("    ionMass: ").append(toIndentedString(ionMass)).append("\n");
    sb.append("    adduct: ").append(toIndentedString(adduct)).append("\n");
    sb.append("    rtStartSeconds: ").append(toIndentedString(rtStartSeconds)).append("\n");
    sb.append("    rtEndSeconds: ").append(toIndentedString(rtEndSeconds)).append("\n");
    sb.append("    mergedMs1: ").append(toIndentedString(mergedMs1)).append("\n");
    sb.append("    ms1Spectra: ").append(toIndentedString(ms1Spectra)).append("\n");
    sb.append("    ms2Spectra: ").append(toIndentedString(ms2Spectra)).append("\n");
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

