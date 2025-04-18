package com.singhand.sr.graphservice.bizgraph.service.impl.jpa;

import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.service.RelationModelService;
import com.singhand.sr.graphservice.bizgraph.service.impl.neo4j.Neo4jRelationModelService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationModel;
import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationModel_;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationModelRepository;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JpaRelationModelServiceImpl implements RelationModelService {

  private final RelationModelRepository relationModelRepository;

  private final Neo4jRelationModelService neo4jRelationModelService;

  @Autowired
  public JpaRelationModelServiceImpl(RelationModelRepository relationModelRepository,
                                     Neo4jRelationModelService neo4jRelationModelService) {

    this.relationModelRepository = relationModelRepository;
    this.neo4jRelationModelService = neo4jRelationModelService;
  }

  @Override
  public Optional<RelationModel> getRelationModel(Long id) {

    return relationModelRepository.findById(id);
  }

  @Override
  public List<RelationModel> getRelationModels(String keyword) {

    return relationModelRepository.findAll(Specification.where(nameLike(keyword)));
  }

  @Override
  public RelationModel newRelationModel(String name) {

    final var exists = relationModelRepository.existsByName(name);

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "关系模型已存在");
    }

    final var relationModel = new RelationModel();
    relationModel.setName(name);

    final var managedRelationModel = relationModelRepository.save(relationModel);
    neo4jRelationModelService.newRelationModel(managedRelationModel);
    return managedRelationModel;
  }

  @Override
  public RelationModel updateRelationModel(Long id, String name) {

    final var relationModel = getRelationModel(id)
        .orElseThrow(() -> new RuntimeException("关系模型不存在"));

    if (relationModel.getName().equals(name)) {
      return relationModel;
    }

    final var exists = relationModelRepository.existsByName(name);

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "关系模型已存在");
    }

    relationModel.setName(name);

    final var managedRelationModel = relationModelRepository.save(relationModel);
    neo4jRelationModelService.updateRelationModel(managedRelationModel);
    return managedRelationModel;
  }

  @Override
  public void deleteRelationModel(Long id) {

    final var relationModel = getRelationModel(id)
        .orElseThrow(() -> new RuntimeException("关系模型不存在"));

    relationModelRepository.delete(relationModel);
    neo4jRelationModelService.deleteRelationModel(id);
  }

  private static @Nonnull Specification<RelationModel> nameLike(String keyword) {

    return (root, query, criteriaBuilder) -> {
      if (StrUtil.isBlank(keyword)) {
        return criteriaBuilder.and();
      }
      return criteriaBuilder.like(root.get(RelationModel_.NAME), "%" + keyword.trim() + "%");
    };
  }
}
