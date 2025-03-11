package com.singhand.sr.graphservice.bizgraph.service.impl.jpa;

import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.DeletePropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdateOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.impl.neo4j.Neo4jOntologyService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.OntologyProperty;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology_;
import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationInstance;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyPropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationInstanceRepository;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class JpaOntologyService implements OntologyService {

  private final OntologyRepository ontologyRepository;

  private final Neo4jOntologyService neo4jOntologyService;

  private final OntologyPropertyRepository ontologyPropertyRepository;

  private final RelationInstanceRepository relationInstanceRepository;

  public JpaOntologyService(OntologyRepository ontologyRepository,
      Neo4jOntologyService neo4jOntologyService,
      OntologyPropertyRepository ontologyPropertyRepository,
      RelationInstanceRepository relationInstanceRepository) {

    this.ontologyRepository = ontologyRepository;
    this.neo4jOntologyService = neo4jOntologyService;
    this.ontologyPropertyRepository = ontologyPropertyRepository;
    this.relationInstanceRepository = relationInstanceRepository;
  }

  @Override
  public Optional<Ontology> getOntology(Long id) {

    return ontologyRepository.findById(id);
  }

  @Override
  public Page<Ontology> getOntologies(String keyword, Pageable pageable) {

    return ontologyRepository.findAll(Specification.where(nameLike(keyword)), pageable);
  }

  private static @Nonnull Specification<Ontology> nameLike(String keyword) {

    return (root, query, criteriaBuilder) -> {
      if (StrUtil.isBlank(keyword)) {
        return criteriaBuilder.and();
      }
      return criteriaBuilder.like(root.get(Ontology_.NAME), "%" + keyword.trim() + "%");
    };
  }

  @Override
  public Ontology newOntology(String name, Long parentId) {

    final var existsOntology = ontologyRepository.existsByName(name);
    if (existsOntology) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体已存在");
    }

    final var ontology = new Ontology();
    ontology.setName(name);
    if (null != parentId) {
      final var parent = ontologyRepository.findById(parentId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "父本体不存在"));
      parent.addChild(ontology);
    }

    final var managedOntology = ontologyRepository.save(ontology);
    neo4jOntologyService.newOntology(managedOntology, parentId);
    return managedOntology;
  }

  @Override
  public Set<String> getAllSubOntologies(Set<String> names) {

    final var ontologies = ontologyRepository.findByNameIn(names);

    final var ids = ontologies.stream()
        .map(Ontology::getID)
        .collect(Collectors.toSet());

    final var ontologyNodes = getSubtreeNodesByIds(ids);

    return ontologyNodes
        .stream()
        .map(OntologyNode::getName)
        .collect(Collectors.toSet());
  }

  @Override
  public List<OntologyNode> getTree(Long id) {

    if (null == id) {
      return neo4jOntologyService.buildOntologyTree();
    }

    return neo4jOntologyService.getSubtree(id);
  }

  @Override
  public void newOntologyProperty(@Nonnull Ontology ontology,
      @Nonnull NewOntologyPropertyRequest request) {

    final var exists = ontologyPropertyRepository
        .existsByOntologyAndName(ontology, request.getName());
    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体属性已存在");
    }

    final var property = new OntologyProperty();
    property.setName(request.getName());
    property.setType(request.getType());
    ontology.addProperty(property);
    ontologyPropertyRepository.save(property);
  }

  @Override
  public Page<OntologyProperty> getProperties(Ontology ontology, Pageable pageable) {

    return ontologyPropertyRepository.findByOntology(ontology, pageable);
  }

  @Override
  public Optional<OntologyProperty> getProperty(Ontology ontology, String key) {

    return ontologyPropertyRepository.findByOntologyAndName(ontology, key);
  }

  @Override
  public Ontology updateOntology(Ontology ontology, String name) {

    final var exists = ontologyRepository.existsByName(name);

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体已存在");
    }

    ontology.setName(name);

    final var managedOntology = ontologyRepository.save(ontology);

    neo4jOntologyService.updateOntology(managedOntology);

    return managedOntology;
  }

  @Override
  public void deleteOntology(Long id) {

    getOntology(id).ifPresent(it -> {
      final var children = new HashSet<>(it.getChildren());
      children.forEach(child -> deleteOntology(child.getID()));

      it.detachRelations();
      it.detachChildren();

      ontologyRepository.delete(it);

      neo4jOntologyService.deleteOntology(id);
    });
  }

  @Override
  public void updateOntologyProperty(@Nonnull Ontology ontology,
      @Nonnull UpdateOntologyPropertyRequest request) {

    final var ontologyProperty = ontologyPropertyRepository
        .findByOntologyAndName(ontology, request.getOldName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体属性不存在"));

    final var exists = ontologyPropertyRepository
        .existsByOntologyAndName(ontology, request.getNewName());

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体属性已存在");
    }

    ontologyProperty.setName(request.getNewName());
    ontologyProperty.setType(request.getType());

    ontologyPropertyRepository.save(ontologyProperty);
  }

  @Override
  public void deleteOntologyProperty(Long id, String propertyName) {

    final var ontology = getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var ontologyProperty = ontologyPropertyRepository
        .findByOntologyAndName(ontology, propertyName)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体属性不存在"));

    ontology.removeProperty(ontologyProperty);

    ontologyPropertyRepository.delete(ontologyProperty);

    ontologyRepository.save(ontology);
  }

  @Override
  public void deleteOntologyProperties(@Nonnull Ontology ontology,
      @Nonnull DeletePropertyRequest request) {

    final var properties = ontologyPropertyRepository.findAllById(request.getPropertyIds());

    properties.forEach(it -> deleteOntologyProperty(ontology.getID(), it.getName()));
  }

  @Override
  public RelationInstance newRelation(@Nonnull String name, @Nonnull Ontology inOntology,
      @Nonnull Ontology outOntology) {

    final var exists = relationInstanceRepository
        .existsByNameAndInOntologyAndOutOntology(name, inOntology, outOntology);

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "关系已存在");
    }

    final var relationInstance = new RelationInstance();
    relationInstance.setName(name);
    relationInstance.setInOntology(inOntology);
    relationInstance.setOutOntology(outOntology);

    final var managedRelationInstance = relationInstanceRepository.save(relationInstance);

    inOntology.getActiveRelations().add(managedRelationInstance);
    outOntology.getPassiveRelations().add(managedRelationInstance);

    neo4jOntologyService.newRelation(name, inOntology, outOntology);

    return managedRelationInstance;
  }

  @Override
  public RelationInstance updateRelation(String oldName, String newName, Ontology inOntology,
      Ontology outOntology) {

    final var relationInstance = relationInstanceRepository
        .findByNameAndInOntologyAndOutOntology(oldName, inOntology, outOntology)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));

    final var exists = relationInstanceRepository
        .existsByNameAndInOntologyAndOutOntology(newName, inOntology, outOntology);

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "关系已存在");
    }

    relationInstance.setName(newName);

    neo4jOntologyService.updateRelation(oldName, newName, inOntology, outOntology);

    return relationInstanceRepository.save(relationInstance);
  }

  @Override
  public void deleteRelation(String name, Ontology inOntology, Ontology outOntology) {

    final var relationInstance = relationInstanceRepository
        .findByNameAndInOntologyAndOutOntology(name, inOntology, outOntology)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));

    relationInstance.detachOntologies();

    relationInstanceRepository.delete(relationInstance);

    neo4jOntologyService.deleteRelation(name, inOntology, outOntology);
  }

  @Override
  public Page<RelationInstance> getRelations(Ontology ontology, Pageable pageable) {

    return relationInstanceRepository.findByInOntology(ontology, pageable);
  }

  /**
   * 根据给定的节点ID列表获取子树节点
   *
   * @param ids 节点ID列表
   * @return 包含所有子树节点的列表
   */
  public List<OntologyNode> getSubtreeNodesByIds(Set<Long> ids) {

    return neo4jOntologyService.getSubtreeNodesByIds(ids);
  }
}
