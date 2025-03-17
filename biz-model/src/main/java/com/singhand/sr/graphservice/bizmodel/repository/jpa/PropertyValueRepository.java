package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.PropertyValue;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyValueRepository extends BaseRepository<PropertyValue, Long> {

  Set<PropertyValue> findByProperty_Vertex_ID(String ID);

  Optional<PropertyValue> findByProperty_Vertex_IDAndProperty_KeyAndMd5(String ID, String key,
      String md5);

  Collection<PropertyValue> findDistinctByProperty_Vertex(Vertex vertex);
}
