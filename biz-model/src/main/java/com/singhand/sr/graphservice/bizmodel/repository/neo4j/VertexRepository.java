package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.Vertex;
import org.springframework.stereotype.Repository;

@Repository
public interface VertexRepository extends BaseRepository<Vertex, String> {

}
