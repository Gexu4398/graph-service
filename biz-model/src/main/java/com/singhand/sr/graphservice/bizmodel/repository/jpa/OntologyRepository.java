package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyRepository extends BaseRepository<Ontology, Long> {

  Set<Ontology> findByParent_ID(Long ID);

  Set<Ontology> findByNameIn(Collection<String> names);

  boolean existsByName(String name);

  Optional<Ontology> findByName(String name);

  @Query("select o.name from Ontology o")
  Set<String> findAllNames();
}
