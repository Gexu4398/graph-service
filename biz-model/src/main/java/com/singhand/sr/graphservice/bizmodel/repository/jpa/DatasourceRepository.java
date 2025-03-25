package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasourceRepository extends BaseRepository<Datasource, Long> {

  Optional<Datasource> findFirstByTitle(String title);
}
