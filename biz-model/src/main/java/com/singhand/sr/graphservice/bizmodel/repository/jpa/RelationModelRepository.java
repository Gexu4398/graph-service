package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationModel;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface RelationModelRepository extends BaseRepository<RelationModel, Long> {

  Optional<RelationModel> findByName(String name);

  boolean existsByName(String name);
}
