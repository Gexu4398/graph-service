package com.singhand.sr.graphservice.bizmodel.model.reponse;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OperationResponse {

  private long id;

  private String status;

  private String exitCode;

  private String name;

  private Long startTime;

  private Long endTime;

  private Map<String, Object> ctx = new HashMap<>();

  private Map<String, Object> parameters = new HashMap<>();
}
