package com.singhand.sr.graphservice.bizmodel.model.jpa.dto;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import java.io.Serializable;
import java.util.Calendar;
import lombok.Value;

/**
 * DTO for {@link Datasource}
 */
@Value
public class DatasourceDto implements Serializable {

  Long ID;

  String title;

  String source;

  String description;

  String url;

  String status;

  String creator;

  Double confidence;

  Calendar createdAt;

  Calendar updatedAt;
}