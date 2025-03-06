package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import java.util.Optional;
import java.util.Set;

public interface OntologyService {

  Optional<Ontology> getOntology(Long id);

  Ontology newOntology(String name, Long parentId);

  Set<String> getAllSubOntologies(Set<String> names);
}
