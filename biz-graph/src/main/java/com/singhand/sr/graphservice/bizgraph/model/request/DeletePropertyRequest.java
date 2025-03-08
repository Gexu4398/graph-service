package com.singhand.sr.graphservice.bizgraph.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Set;

@Data
public class DeletePropertyRequest {

  @NotNull(message = "属性ID不能为空")
  private Set<Long> propertyIds;
}
