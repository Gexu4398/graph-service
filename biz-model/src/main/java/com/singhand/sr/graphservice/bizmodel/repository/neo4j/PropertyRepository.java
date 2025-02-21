package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.Property;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyRepository extends BaseNodeRepository<Property, Long> {

}
