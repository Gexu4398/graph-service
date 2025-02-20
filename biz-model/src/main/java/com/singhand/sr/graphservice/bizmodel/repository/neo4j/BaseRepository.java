package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends Neo4jRepository<T, ID> {

}
