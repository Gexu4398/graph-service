package com.singhand.sr.graphservice.bizservice.config;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition
public class SwaggerConfig {

  private static final String OAUTH_SCHEME_NAME = "oAuth";

  private static final String PROTOCOL_URL_FORMAT = "%s/realms/%s/protocol/openid-connect";

  private final String authServerUrl;

  private final String realm;

  @Autowired
  public SwaggerConfig(
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {

    this.authServerUrl = StrUtil.subBefore(jwkSetUri, "/realms", true);
    this.realm = StrUtil.subBetween(jwkSetUri, "realms/", "/");
  }

  @Bean
  public OpenAPI customOpenApi() {

    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes(OAUTH_SCHEME_NAME, createOAuthScheme()))
        .addSecurityItem(new SecurityRequirement().addList(OAUTH_SCHEME_NAME));
  }

  private SecurityScheme createOAuthScheme() {

    OAuthFlows flows = createOAuthFlows();

    return new SecurityScheme()
        .type(SecurityScheme.Type.OAUTH2)
        .flows(flows);
  }

  private OAuthFlows createOAuthFlows() {

    OAuthFlow flow = createAuthorizationCodeFlow();

    return new OAuthFlows()
        .authorizationCode(flow);
  }

  private OAuthFlow createAuthorizationCodeFlow() {

    final var protocolUrl = String.format(PROTOCOL_URL_FORMAT, authServerUrl, realm);

    return new OAuthFlow()
        .authorizationUrl(protocolUrl + "/auth")
        .tokenUrl(protocolUrl + "/token")
        .scopes(new Scopes().addString("openid", ""));
  }
}
