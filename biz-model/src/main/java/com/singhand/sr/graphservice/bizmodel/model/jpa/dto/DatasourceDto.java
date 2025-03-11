package com.singhand.sr.graphservice.bizmodel.model.jpa.dto;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Calendar;
import lombok.Value;

/**
 * DTO for {@link Datasource}
 */
@Value
public class DatasourceDto implements Serializable {

  Long ID;

  @NotBlank(message = "标题为空")
  String title;

  @NotBlank(message = "来源类型为空")
  String sourceType;

  @NotBlank(message = "内容类型为空")
  String contentType;

  String description;

  String url;

  String creator;

  Calendar createdAt;

  Calendar updatedAt;
}