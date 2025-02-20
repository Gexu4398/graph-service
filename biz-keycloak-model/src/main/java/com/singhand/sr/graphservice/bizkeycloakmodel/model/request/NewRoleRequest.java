package com.singhand.sr.graphservice.bizkeycloakmodel.model.request;

import com.singhand.sr.graphservice.bizkeycloakmodel.validator.NotSuperAdminRole;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewRoleRequest {

  @NotSuperAdminRole
  private String name;

  private List<String> scopes;
}
