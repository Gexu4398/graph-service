package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.OntologyProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyPropertyRepository extends BaseRepository<OntologyProperty, Long> {

  Page<OntologyProperty> findByOntology(Ontology ontology, Pageable pageable);

  boolean existsByOntologyAndName(Ontology ontology, String name);
}
