package com.singhand.sr.graphservice.bizgraph.service.impl;

import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.model.NewOntologyRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyNodeRepository;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OntologyServiceImpl implements OntologyService {

  private final OntologyNodeRepository ontologyNodeRepository;

  @Autowired
  public OntologyServiceImpl(OntologyNodeRepository ontologyNodeRepository) {

    this.ontologyNodeRepository = ontologyNodeRepository;
  }

  @Override
  public OntologyNode getOntology(String id) {

    return ontologyNodeRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));
  }

  @Override
  public OntologyNode newOntology(@Nonnull NewOntologyRequest request) {

    final var ontologyNode = new OntologyNode();
    ontologyNode.setName(request.getName());

    final var managedOntologyNode = ontologyNodeRepository.save(ontologyNode);

    if (StrUtil.isNotBlank(request.getParentId())) {
      final var parent = ontologyNodeRepository.findById(request.getParentId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "父本体不存在"));

      managedOntologyNode.setParent(parent);
      parent.getChildOntologies().add(managedOntologyNode);
      ontologyNodeRepository.save(parent);
    }

    return ontologyNodeRepository.save(managedOntologyNode);
  }

  @Override
  public OntologyNode updateOntology(@Nonnull OntologyNode ontologyNode,
      @Nonnull NewOntologyRequest request) {

    ontologyNode.setName(request.getName());

    detachParent(ontologyNode);

    if (StrUtil.isNotBlank(request.getParentId())) {
      final var parent = ontologyNodeRepository.findById(request.getParentId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "父本体不存在"));

      ontologyNode.setParent(parent);
      parent.getChildOntologies().add(ontologyNode);
      ontologyNodeRepository.save(parent);
    }

    return ontologyNodeRepository.save(ontologyNode);
  }

  private void detachParent(@Nonnull OntologyNode ontologyNode) {

    if (null != ontologyNode.getParent()) {
      ontologyNode.getParent().getChildOntologies().remove(ontologyNode);
      ontologyNodeRepository.save(ontologyNode.getParent());
      ontologyNode.setParent(null);
    }
  }
}
