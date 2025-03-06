package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import org.springframework.stereotype.Repository;

@Repository
public interface VertexNodeRepository extends BaseNodeRepository<VertexNode, String> {

}
