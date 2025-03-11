package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Property;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyRepository extends BaseRepository<Property, Long> {

  Optional<Property> findByVertexAndKey(Vertex vertex, String key);

  Optional<Property> findByEdgeAndKey(Edge edge, String key);
}
