package com.singhand.sr.graphservice.bizservice;

import com.github.javafaker.Faker;
import com.singhand.sr.graphservice.testenvironments.UnitTestEnvironment;
import com.singhand.sr.graphservice.testenvironments.helper.DataHelper;
import jakarta.persistence.EntityManager;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.servlet.MockMvc;

public class BaseTestEnvironment extends UnitTestEnvironment {

  protected Faker faker = new Faker(Locale.CHINA);

  @Autowired
  protected DataHelper dataHelper;

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  @Qualifier("keycloakEntityManager")
  protected EntityManager keycloakEntityManager;
}
