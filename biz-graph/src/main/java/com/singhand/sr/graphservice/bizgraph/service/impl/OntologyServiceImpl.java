package com.singhand.sr.graphservice.bizgraph.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.model.NewOntologyRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.dto.OntologyTreeDTO;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyNodeRepository;
import jakarta.annotation.Nonnull;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.neo4j.cypherdsl.core.Cypher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OntologyServiceImpl implements OntologyService {

  private final OntologyNodeRepository ontologyNodeRepository;

  private final Neo4jTemplate neo4jTemplate;

  @Autowired
  public OntologyServiceImpl(OntologyNodeRepository ontologyNodeRepository,
      Neo4jTemplate neo4jTemplate) {

    this.ontologyNodeRepository = ontologyNodeRepository;
    this.neo4jTemplate = neo4jTemplate;
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

    final var ontologyNode = Cypher.node("OntologyNode").named("n");

    final var hasChildRel = Cypher.anyNode()
        .relationshipTo(ontologyNode, "HAS_CHILD")
        .named("r");

    final var noParentCondition = Cypher.not(Cypher.exists(hasChildRel));

    final var statement = Cypher.match(ontologyNode)
        .where(noParentCondition)
        .returning(ontologyNode)
        .build();

    // todo 待完善，无法查询到子集内容
    final var rootNodes = neo4jTemplate.findAll(statement, OntologyNode.class);

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

    // 递归处理子节点
    if (CollUtil.isNotEmpty(node.getChildOntologies())) {
      final var children = node.getChildOntologies().stream()
          .sorted(Comparator.comparing(OntologyNode::getCreatedAt))
          .map(this::convertToTreeDTO)
          .collect(Collectors.toList());
      dto.setChildren(children);
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
