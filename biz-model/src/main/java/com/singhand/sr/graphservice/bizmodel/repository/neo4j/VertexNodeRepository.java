package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface VertexNodeRepository extends BaseNodeRepository<VertexNode, String> {

  Optional<VertexNode> findByNameAndType(@NotBlank String name, @NotBlank String type);

  boolean existsByNameAndType(@NotBlank String name, @NotBlank String type);

  Page<VertexNode> findByTypeIn(Set<String> types, Pageable pageable);

  Page<VertexNode> findByNameLike(String keyword, Pageable pageable);

  Page<VertexNode> findByNameLikeAndTypeIn(String keyword, Set<String> types, Pageable pageable);
}
