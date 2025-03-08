package com.singhand.sr.graphservice.bizservice.service;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DatasourceService {

  private final DatasourceRepository datasourceRepository;

  @Autowired
  public DatasourceService(DatasourceRepository datasourceRepository) {

    this.datasourceRepository = datasourceRepository;
  }

  public Datasource newDatasource(Datasource datasource) {

    return datasourceRepository.save(datasource);
  }
}
