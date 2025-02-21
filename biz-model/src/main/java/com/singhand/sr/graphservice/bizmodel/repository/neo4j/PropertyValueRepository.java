package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.PropertyValue;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyValueRepository extends BaseNodeRepository<PropertyValue, Long> {

}
