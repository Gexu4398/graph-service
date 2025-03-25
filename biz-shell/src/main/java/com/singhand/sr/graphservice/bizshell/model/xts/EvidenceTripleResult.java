package com.singhand.sr.graphservice.bizshell.model.xts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;

@Setter
@Getter
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvidenceTripleResult extends SerializableBean {

  @Serial
  private static final long serialVersionUID = 6732656419863824060L;

//  @Setter
//  @Getter
//  private transient List<KvEvidenceRelationBean> kvEvidenceRelationBeans = new ArrayList<>();

  private String elementId;

  @JsonIgnore
  private transient String fromKbId;

//  @Setter
//  @Getter
//  private boolean store = true;

  private @Length(max = 1000) String subjectLabel;

  @JsonProperty("subjectID")
  private @Length(max = 1000) String subjectId;

  @JsonProperty("subjectVertexId")
  private Long subjectInnerId;

  private @NotBlank
  @Length(max = 1000) String predicateName;

  @JsonProperty("objectID")
  private @Length(max = 1000) String objectId;

  private @Length(max = 1000) String objectLabel;

  @JsonProperty("objectVertexId")
  private Long objectInnerId;

  private KnowledgeType knowledgeType;

//  @Setter
//  @Getter
//  @JsonProperty("additionProperties")
//  private Map<String, Object> knowledgeFeature;

  private Object objectName;

  private @Length(max = 1000) String objectType;

  private @Length(max = 1000) String subjectName;

  private @Length(max = 1000) String subjectType;

//  @Setter
//  @Getter
//  private Long gmt_create;

//  @Setter
//  @Getter
//  private Long gmt_modified;

  private @Length(max = 1000) String subjectOriginWord;

  private @Length(max = 1000) String predicateNameOriginWord;

  private @Length(max = 1000) String objectNameOriginword;

  /**
   * @deprecated
   */
//  @Deprecated
//  private boolean subjectLinked;

  /**
   * @deprecated
   */
//  @Deprecated
//  private boolean objectLinked;

//  private boolean isAction;

//  @Setter
//  @Getter
//  private @Length(
//      max = 1000
//  ) String docId;

//  @Setter
//  @Getter
//  private Double importance = (double) 1.0F;

//  @Setter
//  @Getter
//  private @Length(max = 1000) String version;

//  @Setter
//  @Getter
//  private Double score;

//  @Setter
//  @Getter
//  private boolean verified;

  private Long subjectLastVerifyTime;

  private Long objectLastVerifyTime;

  public EvidenceTripleResult() {

  }
}
