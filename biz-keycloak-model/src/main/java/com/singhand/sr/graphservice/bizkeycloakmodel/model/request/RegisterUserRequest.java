package com.singhand.sr.graphservice.bizkeycloakmodel.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterUserRequest {

  @NotBlank
  private String username;

  private String password;

  private String name;

  private String groupId;

  private String phoneNumber;

  private String picture;
}
