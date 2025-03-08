package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationInstance;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface RelationInstanceRepository extends BaseRepository<RelationInstance, Long> {

  Optional<RelationInstance> findByNameAndInOntologyAndOutOntology(String name, Ontology inOntology,
      Ontology outOntology);

  boolean existsByNameAndInOntologyAndOutOntology(String name, Ontology inOntology,
      Ontology outOntology);
}
