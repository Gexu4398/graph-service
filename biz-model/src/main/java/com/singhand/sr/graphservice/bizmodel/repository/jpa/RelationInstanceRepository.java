package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationInstance;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface RelationInstanceRepository extends BaseRepository<RelationInstance, Long> {

  Page<RelationInstance> findByInOntology(Ontology inOntology, Pageable pageable);

  Optional<RelationInstance> findByNameAndInOntologyAndOutOntology(String name, Ontology inOntology,
      Ontology outOntology);

  boolean existsByNameAndInOntologyAndOutOntology(String name, Ontology inOntology,
      Ontology outOntology);
}
