package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.OntologyProperty;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OntologyService {

  Optional<Ontology> getOntology(Long id);

  Ontology newOntology(String name, Long parentId);

  Set<String> getAllSubOntologies(Set<String> names);

  List<OntologyNode> getTree(Long id);

  void newOntologyProperty(Ontology ontology, String key);

  Page<OntologyProperty> getProperties(Ontology ontology, Pageable pageable);
}
