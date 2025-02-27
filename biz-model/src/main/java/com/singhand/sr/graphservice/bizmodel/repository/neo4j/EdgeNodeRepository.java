package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.EdgeNode;
import org.springframework.stereotype.Repository;

@Repository
public interface EdgeNodeRepository extends BaseNodeRepository<EdgeNode, String> {

}
