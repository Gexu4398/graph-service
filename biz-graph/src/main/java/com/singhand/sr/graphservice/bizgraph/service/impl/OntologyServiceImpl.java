package com.singhand.sr.graphservice.bizgraph.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdateOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyPropertyNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyRelationNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.response.OntologyPropertyItem;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.response.OntologyRelationNodeItem;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.response.OntologyTreeItem;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyNodeRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyPropertyNodeRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyRelationNodeRepository;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.cypherdsl.core.Cypher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OntologyServiceImpl implements OntologyService {

  private final OntologyNodeRepository ontologyNodeRepository;

  private final OntologyPropertyNodeRepository ontologyPropertyNodeRepository;

  private final OntologyRelationNodeRepository ontologyRelationNodeRepository;

  @Autowired
  public OntologyServiceImpl(OntologyNodeRepository ontologyNodeRepository,
      OntologyPropertyNodeRepository ontologyPropertyNodeRepository,
      OntologyRelationNodeRepository ontologyRelationNodeRepository) {

    this.ontologyNodeRepository = ontologyNodeRepository;
    this.ontologyPropertyNodeRepository = ontologyPropertyNodeRepository;
    this.ontologyRelationNodeRepository = ontologyRelationNodeRepository;
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
  public List<OntologyTreeItem> getOntologyTree() {

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

  @Override
  public OntologyNode newProperty(@Nonnull OntologyNode ontology,
      @Nonnull NewOntologyPropertyRequest request) {

    final var properties = getProperties(ontology);
    final var anyMatch = properties.stream().anyMatch(it -> it.getName().equals(request.getName()));
    if (anyMatch) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "属性已存在");
    }

    final var propertyNode = new OntologyPropertyNode();
    propertyNode.setName(request.getName());
    propertyNode.setType(request.getType());
    propertyNode.setMultiple(request.isMultiple());
    propertyNode.setDescription(request.getDescription());

    final var managedPropertyNode = ontologyPropertyNodeRepository.save(propertyNode);

    ontology.getProperties().add(managedPropertyNode);
    return ontologyNodeRepository.save(ontology);
  }

  @Override
  public void deleteProperties(@Nonnull OntologyNode ontology, @Nonnull Set<String> propertyIds) {

    final var properties = ontologyPropertyNodeRepository.findAllById(propertyIds);

    if (!CollUtil.containsAll(ontology.getProperties(), properties)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在");
    }

    detachProperty(ontology, properties);

    ontologyPropertyNodeRepository.deleteAll(properties);

    ontologyNodeRepository.save(ontology);
  }

  @Override
  public OntologyNode updateProperty(@Nonnull OntologyNode ontology,
      @Nonnull OntologyPropertyNode propertyNode, UpdateOntologyPropertyRequest request) {

    if (!ontology.getProperties().contains(propertyNode)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在");
    }

    if (StrUtil.equals(propertyNode.getName(), request.getName()) &&
        StrUtil.equals(propertyNode.getDescription(), request.getDescription())) {
      return ontology;
    }

    final var anyMatch = getProperties(ontology).stream()
        .anyMatch(it -> it.getName().equals(request.getName()));

    if (anyMatch) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "属性已存在");
    }

    propertyNode.setName(request.getName());
    propertyNode.setDescription(request.getDescription());

    ontologyPropertyNodeRepository.save(propertyNode);

    return ontologyNodeRepository.save(ontology);
  }

  @Override
  public OntologyRelationNodeItem newRelation(@Nonnull OntologyNode inOntology,
      @Nonnull String name, @Nonnull OntologyNode outOntology) {

    final var exists = ontologyRelationNodeRepository
        .existsRelationBetweenNodes(inOntology.getId(), outOntology.getName(), name);
    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "关系已存在");
    }

    final var relationNode = new OntologyRelationNode();
    relationNode.setName(name);
    relationNode.setTargetOntologyNode(outOntology);

    final var managedRelation = ontologyRelationNodeRepository.save(relationNode);

    inOntology.getRelations().add(managedRelation);

    final var managedInOntology = ontologyNodeRepository.save(inOntology);

    return OntologyRelationNodeItem.builder()
        .id(managedRelation.getId())
        .name(managedRelation.getName())
        .inOntology(managedInOntology)
        .outOntology(outOntology)
        .createdAt(managedRelation.getCreatedAt())
        .updatedAt(managedRelation.getUpdatedAt())
        .build();
  }

  @Override
  public void deleteRelation(@Nonnull OntologyNode inOntology, @Nonnull String name,
      @Nonnull OntologyNode outOntology) {

    final var relationNode = ontologyRelationNodeRepository
        .findRelationBetweenNodes(inOntology.getId(), outOntology.getId(), name)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));

    inOntology.getRelations().remove(relationNode);

    ontologyNodeRepository.save(inOntology);

    ontologyRelationNodeRepository.delete(relationNode);
  }

  @Override
  public OntologyRelationNodeItem updateRelation(@Nonnull OntologyNode inOntology,
      @Nonnull String name, @Nonnull OntologyNode outOntology, @Nonnull String newName) {

    final var relationNode = ontologyRelationNodeRepository
        .findRelationBetweenNodes(inOntology.getId(), outOntology.getId(), name)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));

    final var exists = ontologyRelationNodeRepository
        .existsRelationBetweenNodes(inOntology.getId(), outOntology.getId(), newName);

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "关系已存在");
    }

    relationNode.setName(newName);
    relationNode.setTargetOntologyNode(outOntology);
    final var managedRelation = ontologyRelationNodeRepository.save(relationNode);

    return OntologyRelationNodeItem.builder()
        .id(managedRelation.getId())
        .name(managedRelation.getName())
        .inOntology(inOntology)
        .outOntology(outOntology)
        .createdAt(managedRelation.getCreatedAt())
        .updatedAt(managedRelation.getUpdatedAt())
        .build();
  }

  @Override
  public Set<OntologyPropertyItem> getProperties(@Nonnull OntologyNode ontologyNode) {

    final var properties = new HashSet<OntologyPropertyItem>();

    if (CollUtil.isNotEmpty(ontologyNode.getParent().getProperties())) {
      final var propertyItems = ontologyNode.getParent().getProperties().stream()
          .map(it -> BeanUtil.copyProperties(it, OntologyPropertyItem.class))
          .collect(Collectors.toSet());
      properties.addAll(propertyItems);
    }

    addParents(ontologyNode, properties);

    return properties;
  }

  private void addParents(@Nonnull OntologyNode ontologyNode,
      @Nonnull Set<OntologyPropertyItem> properties) {

    if (null != ontologyNode.getParent()) {
      if (CollUtil.isNotEmpty(ontologyNode.getParent().getProperties())) {
        final var propertyItems = ontologyNode.getParent().getProperties().stream()
            .map(it -> {
              final var propertyItem = BeanUtil
                  .copyProperties(it, OntologyPropertyItem.class);
              propertyItem.setParent(true);
              return propertyItem;
            }).collect(Collectors.toSet());
        properties.addAll(propertyItems);
      }
      addParents(ontologyNode.getParent(), properties);
    }
  }

  private void detachProperty(@Nonnull OntologyNode ontologyNode,
      @Nonnull List<OntologyPropertyNode> properties) {

    if (CollUtil.isNotEmpty(ontologyNode.getProperties())) {
      properties.forEach(ontologyNode.getProperties()::remove);
    }
  }

  private List<OntologyTreeItem> buildTree(@Nonnull List<OntologyNode> allNodes) {

    final var rootNodes = allNodes.stream()
        .filter(node -> node.getParent() == null)
        .toList();

    return rootNodes.stream()
        .map(this::convertToTreeDTO)
        .collect(Collectors.toList());
  }

  private @Nonnull OntologyTreeItem convertToTreeDTO(@Nonnull OntologyNode node) {

    final var dto = new OntologyTreeItem();
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
