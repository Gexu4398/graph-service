package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VertexRepository extends BaseRepository<Vertex, String> {

  Optional<Vertex> findByNameAndType(String name, String type);

  boolean existsByNameAndType(String name, String type);
}
