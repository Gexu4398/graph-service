package com.singhand.sr.graphservice.bizkeycloakmodel.model.request;

import com.singhand.sr.graphservice.bizkeycloakmodel.validator.NotSuperAdminRole;
import lombok.Data;

@Data
public class RenameRoleRequest {

  @NotSuperAdminRole
  private String newRoleName;
}
