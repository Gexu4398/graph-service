package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface VertexRepository extends BaseRepository<Vertex, String> {

  Page<Vertex> findByDatasources_ID(Long ID, Pageable pageable);

  Set<Vertex> findByTypeIn(Collection<String> types);

  Optional<Vertex> findByNameAndType(String name, String type);

  boolean existsByNameAndType(String name, String type);
}
