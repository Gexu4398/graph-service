package com.singhand.sr.graphservice.bizshell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "com.singhand.sr.graphservice")
@EnableCaching
public class BizShellApplication {

  public static void main(String[] args) {

    SpringApplication.run(BizShellApplication.class, args);
  }
}
