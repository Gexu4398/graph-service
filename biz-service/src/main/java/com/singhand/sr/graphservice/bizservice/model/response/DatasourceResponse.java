package com.singhand.sr.graphservice.bizservice.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DatasourceResponse extends Datasource {

  @JsonProperty(access = Access.READ_ONLY)
  private String text;

  @JsonProperty(access = Access.READ_ONLY)
  private String html;
}
