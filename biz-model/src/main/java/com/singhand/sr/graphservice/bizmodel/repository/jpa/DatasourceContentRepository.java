package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.DatasourceContent;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasourceContentRepository extends BaseRepository<DatasourceContent, Long> {

  Optional<DatasourceContent> findByDatasource_ID(Long ID);
}
