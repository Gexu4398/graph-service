package com.singhand.sr.graphservice.bizkeycloakmodel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {

  private String id;

  private String name;

  private String parentId;
}
