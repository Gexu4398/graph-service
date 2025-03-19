package com.singhand.sr.graphservice.bizgraph.service.impl.neo4j;

import cn.hutool.core.collection.CollUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdatePropertyRequest;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.EdgeRelation;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.VertexNodeRepository;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Neo4jVertexService类提供了对Neo4j数据库中VertexNode的操作服务。
 */
@Service
@Slf4j
@Transactional(transactionManager = "bizNeo4jTransactionManager")
public class Neo4jVertexService {

  private final VertexNodeRepository vertexNodeRepository;

  private final Neo4jVectorStore vectorStore;

  /**
   * 构造函数，初始化VertexNodeRepository和Neo4jVectorStore。
   *
   * @param vertexNodeRepository VertexNode的仓库
   * @param vectorStore          Neo4j的向量存储
   */
  @Autowired
  public Neo4jVertexService(VertexNodeRepository vertexNodeRepository,
      Neo4jVectorStore vectorStore) {

    this.vertexNodeRepository = vertexNodeRepository;
    this.vectorStore = vectorStore;
  }

  /**
   * 根据vertexId获取VertexNode。
   *
   * @param vertexId VertexNode的ID
   * @return 包含VertexNode的Optional对象
   */
  public Optional<VertexNode> getVertex(String vertexId) {

    return vertexNodeRepository.findById(vertexId);
  }

  /**
   * 创建新的VertexNode。
   *
   * @param vertex Vertex对象
   * @return 新创建的VertexNode
   */
  public VertexNode newVertex(@Nonnull Vertex vertex) {

    final var exists = vertexNodeRepository.findById(vertex.getID());
    if (exists.isPresent()) {
      return exists.get();
    }

    final var vertexNode = new VertexNode();
    vertexNode.setId(vertex.getID());
    vertexNode.setName(vertex.getName());
    vertexNode.setType(vertex.getType());
    vertexNode.setHierarchyLevel(vertex.getHierarchyLevel());
    final var managedvertexNode = vertexNodeRepository.save(vertexNode);
    addVertexToVectorStore(managedvertexNode);

    return managedvertexNode;
  }

  /**
   * 将VertexNode添加到向量存储中。
   *
   * @param vertexNode VertexNode对象
   */
  public void addVertexToVectorStore(@Nonnull VertexNode vertexNode) {

    try {
      vectorStore.delete(List.of(vertexNode.getId()));
    } catch (Exception e) {
      log.error(e.getMessage());
    }

    final var relationsInfo = new StringBuilder();
    for (EdgeRelation edge : vertexNode.getEdges()) {
      relationsInfo.append("关系类型: ").append(edge.getName())
          .append(", 目标节点id: ").append(edge.getVertexNode().getId())
          .append(", 目标节点名称: ").append(edge.getVertexNode().getName())
          .append(", 目标节点关系: ").append(edge.getVertexNode().getType())
          .append(", 属性: ").append(edge.getProperties().toString())
          .append("; ");
    }

    final var propertiesStr = vertexNode.getProperties().entrySet().stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining(", "));

    final var textBuilder = new StringBuilder();
    textBuilder.append("【id】：").append(vertexNode.getId()).append("; ")
        .append(" 【名称】：").append(vertexNode.getName()).append("; ")
        .append(" 【类型】：").append(vertexNode.getType()).append("; ");

    if (CollUtil.isNotEmpty(vertexNode.getProperties())) {
      textBuilder.append(" 【属性】：").append(propertiesStr).append("; ");
    }

    if (CollUtil.isNotEmpty(vertexNode.getEdges())) {
      textBuilder.append(" 【关系】：").append(relationsInfo);
    }

    final var text = textBuilder.toString();

    final var document = new Document(
        vertexNode.getId(),
        text,
        Map.of(
            "name", vertexNode.getName(),
            "type", vertexNode.getType(),
            "properties", propertiesStr,
            "relations", relationsInfo.toString()
        )
    );

    vectorStore.add(List.of(document));
  }

  /**
   * 更新向量存储中的VertexNode。
   *
   * @param id VertexNode的ID
   */
  public void updateVectorStore(@Nonnull String id) {

    final var vertexNode = getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    addVertexToVectorStore(vertexNode);
  }

  /**
   * 更新向量存储中的VertexNode。
   *
   * @param vertexNode VertexNode对象
   */
  public void updateVectorStore(@Nonnull VertexNode vertexNode) {

    addVertexToVectorStore(vertexNode);
  }

  /**
   * 删除VertexNode。
   *
   * @param vertexId VertexNode的ID
   */
  public void deleteVertex(String vertexId) {

    getVertex(vertexId).ifPresent(it -> {
      vertexNodeRepository.delete(it);
      vectorStore.delete(List.of(it.getId()));
    });
  }

  /**
   * 更新多个VertexNode的类型。
   *
   * @param vertexIds VertexNode的ID列表
   * @param newType   新的类型
   */
  public void updateVertices(List<String> vertexIds, String newType) {

    final var vertexNodes = vertexNodeRepository.findAllById(vertexIds);
    final var managedVertexNodes = new HashSet<>(vertexNodes);

    managedVertexNodes.forEach(it -> {
      it.setType(newType);
      final var managedVertexNode = vertexNodeRepository.save(it);
      updateVectorStore(managedVertexNode);
    });
  }

  /**
   * 更新VertexNode的名称。
   *
   * @param id   VertexNode的ID
   * @param name 新的名称
   */
  public void updateVertex(String id, String name) {

    getVertex(id).ifPresent(it -> {
      it.setName(name);
      final var managedVertexNode = vertexNodeRepository.save(it);
      updateVectorStore(managedVertexNode);
    });
  }

  /**
   * 为VertexNode添加新的属性。
   *
   * @param vertex  Vertex对象
   * @param request 新属性请求
   */
  public void newProperty(@Nonnull Vertex vertex, @Nonnull NewPropertyRequest request) {

    final var vertexNode = getVertex(vertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var values = vertexNode.getProperties()
        .computeIfAbsent(request.getKey(), v -> new HashSet<>());

    values.add(request.getValue());

    final var managedVertexNode = vertexNodeRepository.save(vertexNode);
    updateVectorStore(managedVertexNode);
  }

  /**
   * 更新VertexNode的属性。
   *
   * @param vertex  Vertex对象
   * @param request 更新属性请求
   */
  public void updateProperty(@Nonnull Vertex vertex, @Nonnull UpdatePropertyRequest request) {

    final var vertexNode = getVertex(vertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    vertexNode.getProperties().get(request.getKey()).remove(request.getOldValue());
    vertexNode.getProperties().get(request.getKey()).add(request.getNewValue());

    final var managedVertexNode = vertexNodeRepository.save(vertexNode);
    updateVectorStore(managedVertexNode);
  }

  /**
   * 删除VertexNode的属性值。
   *
   * @param vertex Vertex对象
   * @param key    属性键
   * @param value  属性值
   */
  public void deletePropertyValue(@Nonnull Vertex vertex, @Nonnull String key,
      @Nonnull String value) {

    final var vertexNode = getVertex(vertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    vertexNode.getProperties().get(key).remove(value);

    if (CollUtil.isEmpty(vertexNode.getProperties().get(key))) {
      vertexNode.getProperties().remove(key);
    }

    final var managedVertexNode = vertexNodeRepository.save(vertexNode);
    updateVectorStore(managedVertexNode);
  }

  /**
   * 为VertexNode添加新的边。
   *
   * @param name      边的名称
   * @param inVertex  入边的Vertex对象
   * @param outVertex 出边的Vertex对象
   */
  public void newEdge(@Nonnull String name, @Nonnull Vertex inVertex, @Nonnull Vertex outVertex) {

    final var inVertexNode = getVertex(inVertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var outVertexNode = getVertex(outVertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var edgeRelation = new EdgeRelation();
    edgeRelation.setName(name);
    edgeRelation.setVertexNode(outVertexNode);

    inVertexNode.getEdges().add(edgeRelation);

    final var managedInVertexNode = vertexNodeRepository.save(inVertexNode);
    updateVectorStore(managedInVertexNode);
  }

  /**
   * 删除VertexNode的边。
   *
   * @param name        边的名称
   * @param inVertexId  入边的VertexNode的ID
   * @param outVertexId 出边的VertexNode的ID
   */
  public void deleteEdge(@Nonnull String name, @Nonnull String inVertexId,
      @Nonnull String outVertexId) {

    final var inVertexNode = getVertex(inVertexId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var outVertexNode = getVertex(outVertexId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var edge = inVertexNode.getEdges().stream()
        .filter(it -> it.getName().equals(name)
            && it.getVertexNode().equals(outVertexNode))
        .findFirst()
        .orElse(null);

    if (null == edge) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在");
    }

    inVertexNode.getEdges().remove(edge);
    vertexNodeRepository.deleteRelation(inVertexNode.getId(), outVertexNode.getId(), name);

    final var managedInVertexNode = vertexNodeRepository.save(inVertexNode);
    updateVectorStore(managedInVertexNode);
  }

  /**
   * 更新VertexNode的边。
   *
   * @param oldName   旧的边的名称
   * @param newName   新的边的名称
   * @param inVertex  入边的Vertex对象
   * @param outVertex 出边的Vertex对象
   */
  public void updateEdge(@Nonnull String oldName, @Nonnull String newName, @Nonnull Vertex inVertex,
      @Nonnull Vertex outVertex) {

    final var inVertexNode = getVertex(inVertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var outVertexNode = getVertex(outVertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    inVertexNode.getEdges().stream()
        .filter(it -> it.getName().equals(oldName)
            && it.getVertexNode().equals(outVertexNode))
        .findFirst()
        .ifPresentOrElse(it -> {
          it.setName(newName);
          final var managedInVertexNode = vertexNodeRepository.save(inVertexNode);
          updateVectorStore(managedInVertexNode);
        }, () -> {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在");
        });
  }

  /**
   * 删除VertexNode的属性。
   *
   * @param vertex Vertex对象
   * @param key    属性键
   */
  public void deleteProperty(@Nonnull Vertex vertex, String key) {

    final var vertexNode = getVertex(vertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    vertexNode.getProperties().remove(key);
    final var managedVertexNode = vertexNodeRepository.save(vertexNode);
    updateVectorStore(managedVertexNode);
  }

  /**
   * 为VertexNode的边添加新的属性。
   *
   * @param name        边的名称
   * @param inVertexId  入边的VertexNode的ID
   * @param outVertexId 出边的VertexNode的ID
   * @param key         属性键
   * @param value       属性值
   */
  public void newEdgeProperty(String name, String inVertexId, String outVertexId, String key,
      String value) {

    final var inVertexNode = getVertex(inVertexId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var outVertexNode = getVertex(outVertexId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    inVertexNode.getEdges()
        .stream()
        .filter(it -> it.getName().equals(name)
            && it.getVertexNode().equals(outVertexNode))
        .findFirst()
        .ifPresentOrElse(it -> {
          final var values = it.getProperties().computeIfAbsent(key, v -> new HashSet<>());
          values.add(value);
          final var managedInVertexNode = vertexNodeRepository.save(inVertexNode);

          updateVectorStore(managedInVertexNode);
        }, () -> {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在");
        });
  }
}