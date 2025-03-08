package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationModel;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RelationModelRepository extends BaseRepository<RelationModel, Long> {

  Optional<RelationModel> findByName(String name);

  boolean existsByName(String name);
}
