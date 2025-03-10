package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizgraph.model.request.DeletePropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdateOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.OntologyProperty;
import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationInstance;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OntologyService {

  Optional<Ontology> getOntology(Long id);

  Page<Ontology> getOntologies(String keyword, Pageable pageable);

  Ontology newOntology(String name, Long parentId);

  Set<String> getAllSubOntologies(Set<String> names);

  List<OntologyNode> getTree(Long id);

  void newOntologyProperty(Ontology ontology, NewOntologyPropertyRequest request);

  Page<OntologyProperty> getProperties(Ontology ontology, Pageable pageable);

  Ontology updateOntology(Ontology ontology, String name);

  void deleteOntology(Long id);

  void updateOntologyProperty(Ontology ontology, UpdateOntologyPropertyRequest request);

  void deleteOntologyProperty(Long id, String propertyName);

  void deleteOntologyProperties(Ontology ontology, DeletePropertyRequest request);

  RelationInstance newRelation(String name, Ontology inOntology, Ontology outOntology);

  RelationInstance updateRelation(String oldName, String newName, Ontology inOntology, Ontology outOntology);

  void deleteRelation(String name, Ontology inOntology, Ontology outOntology);

  Page<RelationInstance> getRelations(Ontology ontology, Pageable pageable);
}
