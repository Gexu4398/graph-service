package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.Edge;
import org.springframework.stereotype.Repository;

@Repository
public interface EdgeRepository extends BaseNodeRepository<Edge, Long> {

}
