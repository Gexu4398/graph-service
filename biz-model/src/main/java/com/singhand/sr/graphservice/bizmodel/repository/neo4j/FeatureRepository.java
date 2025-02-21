package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.Feature;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureRepository extends BaseNodeRepository<Feature, Long> {

}
