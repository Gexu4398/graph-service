package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.DatasourceContent;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DatasourceContentRepository extends BaseRepository<DatasourceContent, Long> {

  Optional<DatasourceContent> findByDatasource_ID(Long ID);
}
