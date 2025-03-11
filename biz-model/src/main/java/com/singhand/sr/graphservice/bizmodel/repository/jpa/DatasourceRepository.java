package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.model.jpa.dto.DatasourceDto;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasourceRepository extends BaseRepository<Datasource, Long> {

  Optional<DatasourceDto> findDatasourceDtoByID(Long ID);

  Optional<Datasource> findFirstByTitle(String title);
}
