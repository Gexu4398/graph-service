package com.singhand.sr.graphservice.bizgraph.service.impl.jpa;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.DeletePropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdateOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.impl.neo4j.Neo4jOntologyService;
import com.singhand.sr.graphservice.bizmodel.config.TxSynchronizationManager;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.OntologyProperty;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology_;
import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationInstance;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyPropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationInstanceRepository;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class JpaOntologyService implements OntologyService {

  private final OntologyRepository ontologyRepository;

  private final Neo4jOntologyService neo4jOntologyService;

  private final OntologyPropertyRepository ontologyPropertyRepository;

  private final RelationInstanceRepository relationInstanceRepository;

  private final PlatformTransactionManager bizTransactionManager;

  private final TxSynchronizationManager txSyncManager;

  private static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

  public JpaOntologyService(OntologyRepository ontologyRepository,
      Neo4jOntologyService neo4jOntologyService,
      OntologyPropertyRepository ontologyPropertyRepository,
      RelationInstanceRepository relationInstanceRepository,
      @Qualifier("bizTransactionManager") PlatformTransactionManager bizTransactionManager,
      TxSynchronizationManager txSyncManager) {

    this.ontologyRepository = ontologyRepository;
    this.neo4jOntologyService = neo4jOntologyService;
    this.ontologyPropertyRepository = ontologyPropertyRepository;
    this.relationInstanceRepository = relationInstanceRepository;
    this.bizTransactionManager = bizTransactionManager;
    this.txSyncManager = txSyncManager;
  }

  @Override
  public Optional<Ontology> getOntology(Long id) {

    return ontologyRepository.findById(id);
  }

  @Override
  public Page<Ontology> getOntologies(String keyword, Pageable pageable) {

    return ontologyRepository.findAll(Specification.where(nameLike(keyword)), pageable);
  }

  private static @Nonnull Specification<Ontology> nameLike(String keyword) {

    return (root, query, criteriaBuilder) -> {
      if (StrUtil.isBlank(keyword)) {
        return criteriaBuilder.and();
      }
      return criteriaBuilder.like(root.get(Ontology_.NAME), "%" + keyword.trim() + "%");
    };
  }

  @Override
  public Ontology newOntology(String name, Long parentId) {

    final var existsOntology = ontologyRepository.existsByName(name);
    if (existsOntology) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体已存在");
    }

    final var ontology = new Ontology();
    ontology.setName(name);
    if (null != parentId) {
      final var parent = ontologyRepository.findById(parentId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "父本体不存在"));
      parent.addChild(ontology);

      inheritPropertiesFromParent(parent, ontology);
    }
    final var managedOntology = ontologyRepository.save(ontology);
    txSyncManager.executeAfterCommit(
        () -> neo4jOntologyService.newOntology(managedOntology, parentId));
    return managedOntology;
  }

  @Override
  public Set<String> getAllSubOntologies(Set<String> names) {

    final var ontologies = ontologyRepository.findByNameIn(names);

    final var ids = ontologies.stream()
        .map(Ontology::getID)
        .collect(Collectors.toSet());

    final var ontologyNodes = getSubtreeNodesByIds(ids);

    return ontologyNodes
        .stream()
        .map(OntologyNode::getName)
        .collect(Collectors.toSet());
  }

  @Override
  public List<OntologyNode> getTree(Long id) {

    if (null == id) {
      return neo4jOntologyService.buildOntologyTree();
    }

    return neo4jOntologyService.getSubtree(id);
  }

  @Override
  public void newOntologyProperty(@Nonnull Ontology ontology,
      @Nonnull NewOntologyPropertyRequest request) {

    final var exists = ontologyPropertyRepository
        .existsByOntologyAndName(ontology, request.getName());
    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体属性已存在");
    }

    final var property = new OntologyProperty();
    property.setName(request.getName());
    property.setType(request.getType());
    property.setMultiValue(request.isMultiValue());
    property.setInherited(request.isInherited());
    ontology.addProperty(property);
    ontologyPropertyRepository.save(property);
  }

  @Override
  public Page<OntologyProperty> getProperties(Ontology ontology, Pageable pageable) {

    return ontologyPropertyRepository.findByOntology(ontology, pageable);
  }

  @Override
  public Optional<OntologyProperty> getProperty(Ontology ontology, String key) {

    return ontologyPropertyRepository.findByOntologyAndName(ontology, key);
  }

  @Override
  public Ontology updateOntology(Ontology ontology, String name) {

    final var exists = ontologyRepository.existsByName(name);

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体已存在");
    }

    ontology.setName(name);

    final var managedOntology = ontologyRepository.save(ontology);

    txSyncManager.executeAfterCommit(() -> neo4jOntologyService.updateOntology(managedOntology));

    return managedOntology;
  }

  @Override
  public void deleteOntology(Long id) {

    getOntology(id).ifPresent(it -> {
      final var children = new HashSet<>(it.getChildren());
      children.forEach(child -> deleteOntology(child.getID()));

      it.detachRelations();
      it.detachChildren();

      ontologyRepository.delete(it);

      txSyncManager.executeAfterCommit(() -> neo4jOntologyService.deleteOntology(id));
    });
  }

  @Override
  public void updateOntologyProperty(@Nonnull Ontology ontology,
      @Nonnull UpdateOntologyPropertyRequest request) {

    final var ontologyProperty = ontologyPropertyRepository
        .findByOntologyAndName(ontology, request.getOldName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体属性不存在"));

    if (ontologyProperty.isInherited()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "继承的属性不允许修改");
    }

    final var exists = ontologyPropertyRepository
        .existsByOntologyAndName(ontology, request.getNewName());

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体属性已存在");
    }

    ontologyProperty.setName(request.getNewName());
    ontologyProperty.setType(request.getType());
    ontologyProperty.setMultiValue(request.isMultiValue());

    ontologyPropertyRepository.save(ontologyProperty);

    CompletableFuture.runAsync(() ->
            cascadeUpdatePropertyFromChildren(ontology, request.getOldName(),
                request.getNewName(), request.getType(), request.isMultiValue()), VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("级联更新子类中继承的属性任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  @Override
  public void deleteOntologyProperty(Long id, String propertyName) {

    final var ontology = getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var ontologyProperty = ontologyPropertyRepository
        .findByOntologyAndName(ontology, propertyName)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体属性不存在"));

    if (ontologyProperty.isInherited()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "继承的属性不允许删除");
    }

    ontology.removeProperty(ontologyProperty);
    ontologyPropertyRepository.delete(ontologyProperty);
    ontologyRepository.save(ontology);

    CompletableFuture.runAsync(() ->
            cascadeDeletePropertyFromChildren(ontology, propertyName), VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("级联删除子类中继承的属性任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  @Override
  public void deleteOntologyProperties(@Nonnull Ontology ontology,
      @Nonnull DeletePropertyRequest request) {

    final var properties = ontologyPropertyRepository.findAllById(request.getPropertyIds());

    properties.forEach(it -> deleteOntologyProperty(ontology.getID(), it.getName()));
  }

  @Override
  public RelationInstance newRelation(@Nonnull String name, @Nonnull Ontology inOntology,
      @Nonnull Ontology outOntology) {

    final var exists = relationInstanceRepository
        .existsByNameAndInOntologyAndOutOntology(name, inOntology, outOntology);

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "关系已存在");
    }

    final var relationInstance = new RelationInstance();
    relationInstance.setName(name);
    relationInstance.setInOntology(inOntology);
    relationInstance.setOutOntology(outOntology);

    final var managedRelationInstance = relationInstanceRepository.save(relationInstance);

    inOntology.getActiveRelations().add(managedRelationInstance);
    outOntology.getPassiveRelations().add(managedRelationInstance);

    txSyncManager.executeAfterCommit(
        () -> neo4jOntologyService.newRelation(name, inOntology, outOntology));

    return managedRelationInstance;
  }

  @Override
  public RelationInstance updateRelation(String oldName, String newName, Ontology inOntology,
      Ontology outOntology) {

    final var relationInstance = relationInstanceRepository
        .findByNameAndInOntologyAndOutOntology(oldName, inOntology, outOntology)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));

    final var exists = relationInstanceRepository
        .existsByNameAndInOntologyAndOutOntology(newName, inOntology, outOntology);

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "关系已存在");
    }

    relationInstance.setName(newName);

    txSyncManager.executeAfterCommit(
        () -> neo4jOntologyService.updateRelation(oldName, newName, inOntology, outOntology));

    return relationInstanceRepository.save(relationInstance);
  }

  @Override
  public void deleteRelation(String name, Ontology inOntology, Ontology outOntology) {

    final var relationInstance = relationInstanceRepository
        .findByNameAndInOntologyAndOutOntology(name, inOntology, outOntology)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));

    relationInstance.detachOntologies();

    relationInstanceRepository.delete(relationInstance);

    txSyncManager.executeAfterCommit(
        () -> neo4jOntologyService.deleteRelation(name, inOntology, outOntology));
  }

  @Override
  public Page<RelationInstance> getRelations(Ontology ontology, Pageable pageable) {

    return relationInstanceRepository.findByInOntology(ontology, pageable);
  }

  /**
   * 根据给定的节点ID列表获取子树节点
   *
   * @param ids 节点ID列表
   * @return 包含所有子树节点的列表
   */
  public List<OntologyNode> getSubtreeNodesByIds(Set<Long> ids) {

    return neo4jOntologyService.getSubtreeNodesByIds(ids);
  }

  /**
   * 从父本体继承属性到子本体
   *
   * @param parent 父本体
   * @param child  子本体
   */
  private void inheritPropertiesFromParent(@Nonnull Ontology parent, @Nonnull Ontology child) {

    parent.getProperties()
        .forEach(it -> {
          final var request = new NewOntologyPropertyRequest();
          request.setName(it.getName());
          request.setType(it.getType());
          request.setMultiValue(it.isMultiValue());
          request.setInherited(true);
          newOntologyProperty(child, request);
        });
  }

  /**
   * 级联更新子类中继承的属性
   *
   * @param parent     父本体
   * @param oldName    旧属性名
   * @param newName    新属性名
   * @param type       更新后的类型
   * @param multiValue 是否为多值属性
   */
  private void cascadeUpdatePropertyFromChildren(@Nonnull Ontology parent, String oldName,
      String newName, String type, boolean multiValue) {

    Queue<Long> queue = new LinkedList<>();
    queue.add(parent.getID());

    final var transaction = new TransactionTemplate(bizTransactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transaction.execute(status -> {
      while (CollUtil.isNotEmpty(queue)) {
        final var parentId = queue.poll();
        final var children = ontologyRepository.findByParent_ID(parentId);

        children.forEach(child ->
            ontologyPropertyRepository.findByOntologyAndName(child, oldName)
                .ifPresent(property -> {
                  if (property.isInherited()) {
                    property.setName(newName);
                    property.setType(type);
                    property.setMultiValue(multiValue);
                    ontologyPropertyRepository.save(property);
                    queue.add(child.getID());
                  }
                }));
      }
      return true;
    });
  }

  /**
   * 级联删除子类中继承的属性
   *
   * @param parent       父本体
   * @param propertyName 要删除的属性名
   */
  private void cascadeDeletePropertyFromChildren(@Nonnull Ontology parent, String propertyName) {

    Queue<Long> queue = new LinkedList<>();
    queue.add(parent.getID());

    final var transaction = new TransactionTemplate(bizTransactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transaction.execute(status -> {
      while (CollUtil.isNotEmpty(queue)) {
        final var parentId = queue.poll();
        final var children = ontologyRepository.findByParent_ID(parentId);

        children.forEach(child ->
            ontologyPropertyRepository.findByOntologyAndName(child, propertyName)
                .ifPresent(property -> {
                  if (property.isInherited()) {
                    child.removeProperty(property);
                    ontologyPropertyRepository.delete(property);
                    ontologyRepository.save(child);
                    queue.add(child.getID());
                  }
                }));
      }
      return true;
    });
  }
}
