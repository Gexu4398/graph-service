package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.OntologyProperty;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyPropertyRepository extends BaseRepository<OntologyProperty, Long> {

  Page<OntologyProperty> findByOntology(Ontology ontology, Pageable pageable);

  Optional<OntologyProperty> findByOntologyAndName(Ontology ontology, String name);

  Optional<OntologyProperty> findByOntology_NameAndName(String name, String name1);

  Set<OntologyProperty> findByOntology_Name(String name);

  boolean existsByOntology_NameAndName(String ontologyName, String name);

  boolean existsByOntologyAndName(Ontology ontology, String name);
}
