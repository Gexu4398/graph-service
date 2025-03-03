package com.singhand.sr.graphservice.bizgraph.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.model.NewOntologyRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.dto.OntologyTreeDTO;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyNodeRepository;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Override
  public void deleteOntology(OntologyNode ontologyNode) {

    detachParent(ontologyNode);
    clearChild(ontologyNode);

    ontologyNodeRepository.delete(ontologyNode);
  }

  @Override
  public List<OntologyTreeDTO> getOntologyTree() {

    final var ontologyNodes = ontologyNodeRepository.findAll();

    if (CollUtil.isEmpty(ontologyNodes)) {
      return List.of();
    }

    return buildTree(ontologyNodes);
  }

  @Override
  public Page<OntologyNode> getOntologies(String keyword, Pageable pageable) {

    final var ontologyNode = Cypher.node("OntologyNode").named("ontologyNode");

    final var name = ontologyNode.property("name");

    var condition = Cypher.noCondition();
    if (StrUtil.isNotBlank(keyword)) {
      condition = name.contains(Cypher.literalOf(keyword));
    }

    return ontologyNodeRepository.findAll(condition, pageable);
  }

  private List<OntologyTreeDTO> buildTree(@Nonnull List<OntologyNode> allNodes) {

    final var rootNodes = allNodes.stream()
        .filter(node -> node.getParent() == null)
        .toList();

    return rootNodes.stream()
        .map(this::convertToTreeDTO)
        .collect(Collectors.toList());
  }

  private @Nonnull OntologyTreeDTO convertToTreeDTO(@Nonnull OntologyNode node) {

    final var dto = new OntologyTreeDTO();
    dto.setId(node.getId());
    dto.setName(node.getName());
    dto.setCreatedAt(node.getCreatedAt());
    dto.setUpdatedAt(node.getUpdatedAt());

    for (OntologyNode child : node.getChildOntologies()) {
      dto.getChildOntologies().add(convertToTreeDTO(child));
    }

    return dto;
  }

  private void clearChild(@Nonnull OntologyNode ontologyNode) {

    final var children = new HashSet<>(ontologyNode.getChildOntologies());
    children.forEach(it -> {
      it.getParent().getChildOntologies().remove(it);
      it.setParent(null);
    });
  }

  private void detachParent(@Nonnull OntologyNode ontologyNode) {

    if (null != ontologyNode.getParent()) {
      ontologyNode.getParent().getChildOntologies().remove(ontologyNode);
      ontologyNodeRepository.save(ontologyNode.getParent());
      ontologyNode.setParent(null);
    }
  }
}
