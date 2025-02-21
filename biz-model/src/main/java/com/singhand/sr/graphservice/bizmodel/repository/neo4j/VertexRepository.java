package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.Vertex;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface VertexRepository extends BaseNodeRepository<Vertex, String> {

  Optional<Vertex> findByNameAndType(@NotBlank String name, @NotBlank String type);

  boolean existsByNameAndType(@NotBlank String name, @NotBlank String type);

  Page<Vertex> findByTypeIn(Set<String> types, Pageable pageable);

  Page<Vertex> findByNameLike(String keyword, Pageable pageable);

  Page<Vertex> findByNameLikeAndTypeIn(String keyword, Set<String> types, Pageable pageable);
}
