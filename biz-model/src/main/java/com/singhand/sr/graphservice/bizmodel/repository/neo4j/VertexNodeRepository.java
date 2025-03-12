package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VertexNodeRepository extends BaseNodeRepository<VertexNode, String> {

  Optional<VertexNode> findByNameAndType(String name, String type);

  Set<VertexNode> findByTypeIn(Collection<String> types);

  @Query("""
      MATCH (a:Vertex)-[r:CONNECTED_TO {name: $name}]->(b:Vertex)
      WHERE a.id = $inId AND b.id = $outId
      DELETE r
      """)
  void deleteRelation(String inId, String outId, String name);

  @Query("""
      MATCH (a:Vertex)-[r:CONNECTED_TO {name: $name}]->(b:Vertex)
      WHERE a.id = $inId AND b.id = $outId
      RETURN COUNT(r) > 0
      """)
  boolean existsRelation(String inId, String outId, String name);
}
