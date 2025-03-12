package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Evidence;
import com.singhand.sr.graphservice.bizmodel.model.jpa.PropertyValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface EvidenceRepository extends BaseRepository<Evidence, Long> {

  Page<Evidence> findByPropertyValue(PropertyValue propertyValue, Pageable pageable);

  Page<Evidence> findByEdge(Edge edge, Pageable pageable);
}
