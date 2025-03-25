package com.singhand.sr.graphservice.bizgraph.service.impl.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationModel;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.RelationModelNode;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.RelationModelNodeRepository;
import jakarta.annotation.Nonnull;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@Transactional(transactionManager = "bizNeo4jTransactionManager")
public class Neo4jRelationModelService {

  private final RelationModelNodeRepository relationModelNodeRepository;

  @Autowired
  public Neo4jRelationModelService(RelationModelNodeRepository relationModelNodeRepository) {

    this.relationModelNodeRepository = relationModelNodeRepository;
  }

  /**
   * 根据id获取RelationModel
   *
   * @param id RelationModel id
   * @return RelationModel
   */
  public Optional<RelationModelNode> getRelationModel(Long id) {

    return relationModelNodeRepository.findById(id);
  }

  public RelationModelNode newRelationModel(@Nonnull RelationModel relationModel) {

    final var exists = getRelationModel(relationModel.getID());
    if (exists.isPresent()) {
      return exists.get();
    }

    final var node = new RelationModelNode();
    node.setId(relationModel.getID());
    node.setName(relationModel.getName());
    return relationModelNodeRepository.save(node);
  }

  public void updateRelationModel(@Nonnull RelationModel relationModel) {

    final var node = getRelationModel(relationModel.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系模型不存在"));

    node.setName(relationModel.getName());

    relationModelNodeRepository.save(node);
  }

  public void deleteRelationModel(Long id) {

    final var node = getRelationModel(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系模型不存在"));
    relationModelNodeRepository.delete(node);
  }
}
