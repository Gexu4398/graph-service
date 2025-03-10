package com.singhand.sr.graphservice.bizgraph.service.impl.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyRelation;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyNodeRepository;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@Transactional(transactionManager = "bizNeo4jTransactionManager")
public class Neo4jOntologyService {

  private final OntologyNodeRepository ontologyNodeRepository;

  @Autowired
  public Neo4jOntologyService(OntologyNodeRepository ontologyNodeRepository) {

    this.ontologyNodeRepository = ontologyNodeRepository;
  }

  public Optional<OntologyNode> getOntology(Long id) {

    return ontologyNodeRepository.findById(id);
  }

  public void newOntology(@Nonnull Ontology ontology, Long parentId) {

    final var ontologyNode = new OntologyNode();
    ontologyNode.setId(ontology.getID());
    ontologyNode.setName(ontology.getName());

    final var managedOntologyNode = ontologyNodeRepository.save(ontologyNode);

    if (null != parentId) {
      final var parent = getOntology(parentId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "父级本体不存在"));
      parent.getChildren().add(managedOntologyNode);
      ontologyNodeRepository.save(parent);
    }
  }

  public List<OntologyNode> getSubtreeNodesByIds(Set<Long> ids) {

    return ontologyNodeRepository.findSubtreeNodesByIds(ids);
  }

  public List<OntologyNode> getSubtree(Long id) {

    return ontologyNodeRepository.findSubtree(id);
  }

  public List<OntologyNode> buildOntologyTree() {

    return ontologyNodeRepository.findAllSubtreeNodes();
  }

  public void updateOntology(@Nonnull Ontology ontology) {

    final var ontologyNode = getOntology(ontology.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    ontologyNode.setName(ontology.getName());

    ontologyNodeRepository.save(ontologyNode);
  }

  public void deleteOntology(Long id) {

    ontologyNodeRepository.findById(id)
        .ifPresent(it -> ontologyNodeRepository.deleteOntologyAndChildren(it.getId()));
  }

  public void newRelation(@Nonnull String name, @Nonnull Ontology inOntology,
      @Nonnull Ontology outOntology) {

    final var inOntologyNode = getOntology(inOntology.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var outOntologyNode = getOntology(outOntology.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var relation = new OntologyRelation();
    relation.setName(name);
    relation.setOntologyNode(outOntologyNode);

    inOntologyNode.getRelations().add(relation);

    ontologyNodeRepository.save(inOntologyNode);
  }

  public void updateRelation(@Nonnull String oldName, @Nonnull String newName,
      @Nonnull Ontology inOntology, @Nonnull Ontology outOntology) {

    final var inOntologyNode = getOntology(inOntology.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var outOntologyNode = getOntology(outOntology.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    inOntologyNode.getRelations().stream()
        .filter(it -> it.getName().equals(oldName) &&
            it.getOntologyNode().equals(outOntologyNode))
        .findFirst()
        .ifPresentOrElse(it -> {
          it.setName(newName);
          ontologyNodeRepository.save(inOntologyNode);
        }, () -> {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在");
        });
  }

  public void deleteRelation(@Nonnull String name, @Nonnull Ontology inOntology,
      @Nonnull Ontology outOntology) {

    final var inOntologyNode = getOntology(inOntology.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var outOntologyNode = getOntology(outOntology.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var relation = inOntologyNode.getRelations().stream()
        .filter(it -> it.getName().equals(name) &&
            it.getOntologyNode().equals(outOntologyNode))
        .findFirst()
        .orElse(null);

    if (null == relation) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在");
    }

    inOntologyNode.getRelations().remove(relation);

    ontologyNodeRepository.deleteRelation(inOntology.getID(), outOntology.getID(), name);

    ontologyNodeRepository.save(inOntologyNode);
  }
}
