package com.singhand.sr.graphservice.bizkeycloakmodel.service;

import com.singhand.sr.graphservice.bizkeycloakmodel.model.EventEntity;
import com.singhand.sr.graphservice.bizkeycloakmodel.repository.EventEntityRepository;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class KeycloakEventService {

  private final Set<String> EVENT_TYPE_ALL = Set.of("LOGIN", "LOGOUT");

  private final String EVENT_TYPE_LOGIN = "LOGIN";

  private final String EVENT_TYPE_LOGOUT = "LOGOUT";

  private final EventEntityRepository eventEntityRepository;

  @Autowired
  public KeycloakEventService(EventEntityRepository eventEntityRepository) {

    this.eventEntityRepository = eventEntityRepository;
  }

  public Page<EventEntity> getUserLoginAndLogoutEvents(Pageable pageable) {

    return eventEntityRepository.findByTypeIn(EVENT_TYPE_ALL, pageable);
  }

  public Page<EventEntity> getUserLoginEvents(Pageable pageable) {

    return eventEntityRepository.findByType(EVENT_TYPE_LOGIN, pageable);
  }

  public Page<EventEntity> getUserLogoutEvents(Pageable pageable) {

    return eventEntityRepository.findByType(EVENT_TYPE_LOGOUT, pageable);
  }
}
