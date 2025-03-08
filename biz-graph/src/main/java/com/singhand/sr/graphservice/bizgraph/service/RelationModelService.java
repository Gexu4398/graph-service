package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationModel;
import java.util.List;
import java.util.Optional;

public interface RelationModelService {

  Optional<RelationModel> getRelationModel(Long id);

  List<RelationModel> getRelationModels(String keyword);

  RelationModel newRelationModel(String name);

  RelationModel updateRelationModel(Long id, String name);

  void deleteRelationModel(Long id);
}
