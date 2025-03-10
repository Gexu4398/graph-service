package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.Set;

@Repository
public interface VertexNodeRepository extends BaseNodeRepository<VertexNode, String> {

  Set<VertexNode> findByTypeIn(Collection<String> types);
}
