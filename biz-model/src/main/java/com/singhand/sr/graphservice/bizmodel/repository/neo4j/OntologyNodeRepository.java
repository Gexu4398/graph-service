package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import java.util.Collection;
import java.util.List;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyNodeRepository extends BaseNodeRepository<OntologyNode, Long> {

  @Query("""
          MATCH (parent:Ontology {id: $id})-[r:CHILD_OF*0..]->(node:Ontology)
          RETURN collect(DISTINCT node) AS allNodes
      """)
  List<OntologyNode> findAllSubtreeNodes(Long id);

  @Query("""
          UNWIND $ids AS currentId
          MATCH (parent:Ontology {id: currentId})-[r:CHILD_OF*0..]->(node:Ontology)
          RETURN collect(DISTINCT node) AS allNodes
      """)
  List<OntologyNode> findSubtreeNodesByIds(Collection<Long> ids);

  @Query("""
          MATCH (parent:Ontology {id: $id})-[r:CHILD_OF*]->(child:Ontology)
          RETURN parent, collect(child) AS children
      """)
  OntologyNode findSubtree(Long id);
}
