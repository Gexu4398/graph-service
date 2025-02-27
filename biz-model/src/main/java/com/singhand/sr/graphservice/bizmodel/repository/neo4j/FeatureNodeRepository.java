package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.FeatureNode;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureNodeRepository extends BaseNodeRepository<FeatureNode, String> {

}
