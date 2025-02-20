package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizservice.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "获取上传或下载地址")
@RestController
@RequestMapping("s3")
@Slf4j
public class S3Controller {

  private final MinioService minioService;

  private final Pattern objectNamePattern = Pattern.compile(
      "^/[^/]+/(?<path>[^/]+)/(?<date>[^/]+)/(?<name>[^/]+)$");

  @Autowired
  public S3Controller(MinioService minioService) {

    this.minioService = minioService;
  }

  @GetMapping(value = "putURL")
  @SneakyThrows
  @Operation(summary = "获取上传地址")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<String> uploadPreSignedURL(@RequestParam String name,
      @RequestParam String path,
      @RequestParam(defaultValue = "false") boolean keepName,
      @RequestHeader Map<String, String> headers) {

    final var legalHeaders = headers.entrySet().stream()
        .filter(it -> it.getKey().startsWith("x-amz-meta-"))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    final var response = ResponseEntity.ok();

    final var encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
    legalHeaders.put("x-amz-meta-origin-filename", encodedName);
    response.header("x-amz-meta-origin-filename", encodedName);

    final var url = minioService.putPreSignedURL(name, path, legalHeaders, keepName);
    return response.body(url);
  }

  @GetMapping("getURL")
  @Operation(summary = "获取下载地址")
  @PreAuthorize("isAuthenticated()")
  @SneakyThrows
  public String downloadPreSignedURL(@RequestParam String name) {

    final var matcher = objectNamePattern.matcher(name);
    if (!matcher.matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    final var path = matcher.group("path");
    final var date = matcher.group("date");
    final var filename = matcher.group("name");

    return minioService.getPreSignedURL(
        String.format("%s/%s/%s", path, date, filename));
  }
}
