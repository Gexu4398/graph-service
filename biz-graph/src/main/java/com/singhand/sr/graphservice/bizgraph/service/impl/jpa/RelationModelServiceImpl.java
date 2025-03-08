package com.singhand.sr.graphservice.bizgraph.service.impl.jpa;

import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.service.RelationModelService;
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
public class RelationModelServiceImpl implements RelationModelService {

  private final RelationModelRepository relationModelRepository;

  @Autowired
  public RelationModelServiceImpl(RelationModelRepository relationModelRepository) {

    this.relationModelRepository = relationModelRepository;
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

    return relationModelRepository.save(relationModel);
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

    return relationModelRepository.save(relationModel);
  }

  @Override
  public void deleteRelationModel(Long id) {

    final var relationModel = getRelationModel(id)
        .orElseThrow(() -> new RuntimeException("关系模型不存在"));

    relationModelRepository.delete(relationModel);
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
