package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import java.util.Collection;
import java.util.List;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyNodeRepository extends BaseNodeRepository<OntologyNode, Long> {

  @Query("""
          UNWIND $ids AS currentId
          MATCH (parent:Ontology {id: currentId})-[r:CHILD_OF*0..]->(node:Ontology)
          RETURN collect(DISTINCT node) AS allNodes
      """)
  List<OntologyNode> findSubtreeNodesByIds(Collection<Long> ids);

  @Query("""
      MATCH (root:Ontology)
      WHERE root.id = $id
      CALL apoc.path.subgraphAll(root, {
           relationshipFilter: 'CHILD_OF>',
           bfs: true
      })
      YIELD nodes, relationships
      RETURN root, nodes, relationships
      """)
  List<OntologyNode> findSubtree(Long id);

  @Query("""
      MATCH (root:Ontology)
      WHERE NOT EXISTS(()-[:CHILD_OF]->(root))
      CALL apoc.path.subgraphAll(root, {
           relationshipFilter: 'CHILD_OF>',
           bfs: true
      })
      YIELD nodes, relationships
      RETURN root, nodes, relationships
      """)
  List<OntologyNode> findAllSubtreeNodes();

  @Query("""
      MATCH (o:Ontology)-[:CHILD_OF*]->(child)
      WHERE id(o) = $id
      DETACH DELETE child, o
      """)
  void deleteOntologyAndChildren(Long id);

  @Query("""
      MATCH (a:Ontology)-[r:CONNECTED_TO {name: $name}]->(b:Ontology)
      WHERE a.id = $inId AND b.id = $outId
      RETURN COUNT(r) > 0
      """)
  boolean existsRelation(Long inId, Long outId, String name);

  @Query("""
      MATCH (a:Ontology)-[r:CONNECTED_TO {name: $name}]->(b:Ontology)
      WHERE a.id = $inId AND b.id = $outId
      DELETE r
      """)
  void deleteRelation(Long inId, Long outId, String name);
}
