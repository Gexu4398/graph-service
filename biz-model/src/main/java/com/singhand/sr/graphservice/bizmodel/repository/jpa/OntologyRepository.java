package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface OntologyRepository extends BaseRepository<Ontology, Long> {

  Set<Ontology> findByNameIn(Collection<String> names);

  boolean existsByName(String name);

  Optional<Ontology> findByName(String name);
}
