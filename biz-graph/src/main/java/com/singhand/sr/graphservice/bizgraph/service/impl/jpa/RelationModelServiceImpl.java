package com.singhand.sr.graphservice.bizgraph.service.impl.jpa;

import com.singhand.sr.graphservice.bizgraph.service.RelationModelService;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RelationModelServiceImpl implements RelationModelService {

  private final RelationModelRepository relationModelRepository;

  @Autowired
  public RelationModelServiceImpl(RelationModelRepository relationModelRepository) {

    this.relationModelRepository = relationModelRepository;
  }
}
