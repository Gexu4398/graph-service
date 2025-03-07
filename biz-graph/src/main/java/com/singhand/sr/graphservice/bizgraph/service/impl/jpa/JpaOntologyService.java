package com.singhand.sr.graphservice.bizgraph.service.impl.jpa;

import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.impl.neo4j.Neo4jOntologyService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.OntologyProperty;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyPropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class JpaOntologyService implements OntologyService {

  private final OntologyRepository ontologyRepository;

  private final Neo4jOntologyService neo4jOntologyService;

  private final OntologyPropertyRepository ontologyPropertyRepository;

  public JpaOntologyService(OntologyRepository ontologyRepository,
      Neo4jOntologyService neo4jOntologyService,
      OntologyPropertyRepository ontologyPropertyRepository) {

    this.ontologyRepository = ontologyRepository;
    this.neo4jOntologyService = neo4jOntologyService;
    this.ontologyPropertyRepository = ontologyPropertyRepository;
  }

  @Override
  public Optional<Ontology> getOntology(Long id) {

    return ontologyRepository.findById(id);
  }

  @Override
  public Ontology newOntology(String name, Long parentId) {

    final var existsOntology = ontologyRepository.existsByName(name);
    if (existsOntology) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体已存在");
    }

    if (null != parentId) {
      final var existsParent = ontologyRepository.existsById(parentId);
      if (!existsParent) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "父本体不存在");
      }
    }

    final var ontology = new Ontology();
    ontology.setName(name);

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
  public void newOntologyProperty(Ontology ontology, String key) {

    final var exists = ontologyPropertyRepository.existsByOntologyAndName(ontology, key);
    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体属性已存在");
    }

    final var property = new OntologyProperty();
    property.setName(key);
    ontology.addProperty(property);
    ontologyPropertyRepository.save(property);
  }

  @Override
  public Page<OntologyProperty> getProperties(Ontology ontology, Pageable pageable) {

    return ontologyPropertyRepository.findByOntology(ontology, pageable);
  }

  /**
   * 获取指定ID的Ontology子树
   *
   * @param id Ontology节点的唯一标识符，类型为Long
   * @return 返回以该ID节点为根的子树结构，封装为OntologyNode对象
   */
  public List<OntologyNode> getSubtree(Long id) {

    return neo4jOntologyService.getSubtree(id);
  }

  /**
   * 获取指定节点的所有子节点
   *
   * @param id 节点的唯一标识符
   * @return 包含所有子节点的列表
   */
  public List<OntologyNode> getAllSubtreeNodes(Long id) {

    return neo4jOntologyService.getAllSubtreeNodes(id);
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
