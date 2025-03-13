package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationModel;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RelationModelRepository extends BaseRepository<RelationModel, Long> {

  Optional<RelationModel> findByName(String name);

  @Query("select r.name from RelationModel r")
  Set<String> findAllNames();

  boolean existsByName(String name);
}
