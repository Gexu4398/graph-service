package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.model.jpa.dto.DatasourceDto;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DatasourceRepository extends BaseRepository<Datasource, Long> {

  Optional<DatasourceDto> findDatasourceDtoByID(Long ID);
}
