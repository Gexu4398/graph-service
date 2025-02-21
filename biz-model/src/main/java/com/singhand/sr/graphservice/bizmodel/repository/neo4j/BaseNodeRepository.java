package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseNodeRepository<T, ID> extends Neo4jRepository<T, ID>,
    CypherdslConditionExecutor<T> {

}
