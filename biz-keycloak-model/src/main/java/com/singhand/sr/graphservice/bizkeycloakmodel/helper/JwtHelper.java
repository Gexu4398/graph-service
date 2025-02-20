package com.singhand.sr.graphservice.bizkeycloakmodel.helper;

import cn.hutool.core.util.StrUtil;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class JwtHelper {

  public static String getUsername() throws Exception {

    return getJwt().getClaimAsString("preferred_username");
  }

  public static String getUserId() throws Exception {

    return getJwt().getClaimAsString("sub");
  }

  public static String getName() throws Exception {

    return Optional.ofNullable(getJwt().getClaimAsString("name")).orElse("");
  }

  public static Set<String> getScopes() throws Exception {

    return new HashSet<>(StrUtil.split(getJwt().getClaimAsString("scope"), " "));
  }

  private static Jwt getJwt() throws Exception {

    final var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwt) {
      return (Jwt) jwt.getCredentials();
    } else if (authentication instanceof UsernamePasswordAuthenticationToken up) {
      return Jwt.withTokenValue("token")
          .header("alg", JwsAlgorithms.RS256)
          .claim("name", up.getName())
          .claim("preferred_username", up.getName())
          .claim("scope", StrUtil.join(" ",
              up.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()))
          .claim("sub", up.getName())
          .build();
    }
    throw new Exception("未知的 Token 类型");
  }
}
