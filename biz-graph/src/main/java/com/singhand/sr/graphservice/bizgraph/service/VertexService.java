package com.singhand.sr.graphservice.bizgraph.service;

import cn.hutool.crypto.digest.MD5;
import com.singhand.sr.graphservice.bizgraph.model.EventItem;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEdgeRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEvidenceRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdateEdgeRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdatePropertyRequest;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Evidence;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Property;
import com.singhand.sr.graphservice.bizmodel.model.jpa.PropertyValue;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 顶点服务接口，定义了顶点相关的操作方法。
 */
public interface VertexService {

  /**
   * 创建一个新的顶点。
   *
   * @param newVertexRequest 新顶点请求对象
   * @return 创建的顶点对象
   */
  Vertex newVertex(NewVertexRequest newVertexRequest);

  /**
   * 根据ID获取顶点。
   *
   * @param id 顶点ID
   * @return 顶点对象的Optional包装
   */
  Optional<Vertex> getVertex(String id);

  /**
   * 获取顶点分页列表。
   *
   * @param keyword  关键字
   * @param types    顶点类型集合
   * @param useEs    是否使用ES
   * @param pageable 分页信息
   * @return 顶点分页对象
   */
  Page<Vertex> getVertices(String keyword, Set<String> types, boolean useEs, Pageable pageable);

  /**
   * 根据ID删除顶点。
   *
   * @param id 顶点ID
   */
  void deleteVertex(String id);

  /**
   * 批量删除顶点。
   *
   * @param vertexIds 顶点ID列表
   */
  void deleteVertices(List<String> vertexIds);

  /**
   * 更新顶点名称。
   *
   * @param id   顶点ID
   * @param name 新名称
   * @return 更新后的顶点对象
   */
  Vertex updateVertex(String id, String name);

  /**
   * 为顶点添加新属性。
   *
   * @param vertex             顶点对象
   * @param newPropertyRequest 新属性请求对象
   */
  void newProperty(Vertex vertex, NewPropertyRequest newPropertyRequest);

  /**
   * 为边添加新属性。
   *
   * @param edge               边对象
   * @param newPropertyRequest 新属性请求对象
   */
  void newProperty(Edge edge, NewPropertyRequest newPropertyRequest);

  /**
   * 根据顶点和键获取属性。
   *
   * @param vertex 顶点对象
   * @param key    属性键
   * @return 属性对象的Optional包装
   */
  Optional<Property> getProperty(Vertex vertex, String key);

  /**
   * 根据边和键获取属性。
   *
   * @param edge 边对象
   * @param key  属性键
   * @return 属性对象的Optional包装
   */
  Optional<Property> getProperty(Edge edge, String key);

  /**
   * 根据属性和值获取属性值。
   *
   * @param property 属性对象
   * @param value    属性值
   * @return 属性值对象的Optional包装
   */
  default Optional<PropertyValue> getPropertyValue(@Nonnull Property property,
      @Nonnull String value) {

    return getPropertyValue(property, value, "raw");
  }

  /**
   * 根据属性、值和模式获取属性值。
   *
   * @param property 属性对象
   * @param value    属性值
   * @param mode     模式
   * @return 属性值对象的Optional包装
   */
  default Optional<PropertyValue> getPropertyValue(@Nonnull Property property,
      @Nonnull String value, @Nonnull String mode) {

    String md5;
    if (mode.equals("md5")) {
      md5 = value;
    } else {
      md5 = MD5.create().digestHex(value);
    }
    return property.getValues()
        .stream()
        .filter(it -> it.getMd5().equals(md5))
        .findFirst();
  }

  /**
   * 设置属性值的特征。
   *
   * @param propertyValue 属性值对象
   * @param key           特征键
   * @param value         特征值
   */
  void setFeature(PropertyValue propertyValue, String key, String value);

  /**
   * 设置边的特征。
   *
   * @param edge  边对象
   * @param key   特征键
   * @param value 特征值
   */
  void setFeature(Edge edge, String key, String value);

  /**
   * 为属性值添加证据。
   *
   * @param propertyValue      属性值对象
   * @param newEvidenceRequest 新证据请求对象
   */
  void addEvidence(PropertyValue propertyValue, NewEvidenceRequest newEvidenceRequest);

  /**
   * 为边添加证据。
   *
   * @param edge               边对象
   * @param newEvidenceRequest 新证据请求对象
   */
  void addEvidence(Edge edge, NewEvidenceRequest newEvidenceRequest);

  /**
   * 更新顶点属性。
   *
   * @param vertex  顶点对象
   * @param request 更新属性请求对象
   */
  void updateProperty(Vertex vertex, UpdatePropertyRequest request);

  /**
   * 更新边属性。
   *
   * @param edge                  边对象
   * @param updatePropertyRequest 更新属性请求对象
   */
  void updateProperty(Edge edge, UpdatePropertyRequest updatePropertyRequest);

  /**
   * 删除顶点的属性值。
   *
   * @param vertex 顶点对象
   * @param key    属性键
   * @param value  属性值
   * @param mode   模式
   */
  void deletePropertyValue(Vertex vertex, String key, String value, String mode);

  /**
   * 删除顶点的属性。
   *
   * @param vertex 顶点对象
   * @param key    属性键
   */
  void deleteProperty(Vertex vertex, String key);

  /**
   * 删除边的属性。
   *
   * @param edge  边对象
   * @param key   属性键
   * @param value 属性值
   * @param mode  模式
   */
  void deleteProperty(Edge edge, String key, String value, String mode);

  /**
   * 创建新的边。
   *
   * @param inVertex  入顶点
   * @param outVertex 出顶点
   * @param request   新边请求对象
   * @return 创建的边对象
   */
  Edge newEdge(Vertex inVertex, Vertex outVertex, NewEdgeRequest request);

  /**
   * 根据名称、入顶点、出顶点和范围获取边。
   *
   * @param name      边名称
   * @param inVertex  入顶点
   * @param outVertex 出顶点
   * @param scope     范围
   * @return 边对象的Optional包装
   */
  Optional<Edge> getEdge(String name, Vertex inVertex, Vertex outVertex, String scope);

  /**
   * 删除边。
   *
   * @param edge 边对象
   */
  void deleteEdge(Edge edge);

  /**
   * 更新边。
   *
   * @param oldEdge 旧边对象
   * @param request 更新边请求对象
   */
  void updateEdge(Edge oldEdge, UpdateEdgeRequest request);

  /**
   * 批量删除顶点。
   *
   * @param types 顶点类型集合
   */
  void batchDeleteVertex(Set<String> types);

  /**
   * 批量更新顶点类型。
   *
   * @param oldType 旧类型
   * @param newType 新类型
   */
  void batchUpdateVertex(String oldType, String newType);

  /**
   * 批量更新顶点属性键。
   *
   * @param vertexType 顶点类型
   * @param oldKey     旧键
   * @param newKey     新键
   */
  void batchUpdateVertexProperty(String vertexType, String oldKey, String newKey);

  /**
   * 批量删除顶点属性。
   *
   * @param vertexType 顶点类型
   * @param key        属性键
   */
  void batchDeleteVertexProperty(String vertexType, String key);

  /**
   * 批量更新顶点边。
   *
   * @param name          边名称
   * @param newName       新边名称
   * @param inVertexType  入顶点类型
   * @param outVertexType 出顶点类型
   */
  void batchUpdateVertexEdge(String name, String newName, String inVertexType,
      String outVertexType);

  /**
   * 批量更新顶点边。
   *
   * @param name    边名称
   * @param newName 新边名称
   */
  void batchUpdateVertexEdge(String name, String newName);

  /**
   * 批量删除顶点边。
   *
   * @param name          边名称
   * @param inVertexType  入顶点类型
   * @param outVertexType 出顶点类型
   */
  void batchDeleteVertexEdge(String name, String inVertexType, String outVertexType);

  /**
   * 批量删除顶点边。
   *
   * @param name 边名称
   */
  void batchDeleteVertexEdge(String name);

  /**
   * 获取顶点的证据分页列表。
   *
   * @param vertex   顶点对象
   * @param key      属性键
   * @param value    属性值
   * @param mode     模式
   * @param pageable 分页信息
   * @return 证据分页对象
   */
  Page<Evidence> getEvidences(Vertex vertex, String key, String value, String mode,
      Pageable pageable);

  /**
   * 获取边的证据分页列表。
   *
   * @param edge     边对象
   * @param key      属性键
   * @param value    属性值
   * @param mode     模式
   * @param pageable 分页信息
   * @return 证据分页对象
   */
  Page<Evidence> getEvidences(Edge edge, String key, String value, String mode, Pageable pageable);

  /**
   * @param keyword  实体名称
   * @param name     关系名称
   * @param pageable 分页信息
   * @return 关系分页对象
   */
  Page<Edge> getEdges(String keyword, String name, Pageable pageable);

  /**
   * 获取边的数量
   *
   * @return 边的数量
   */
  Long countEdges();

  /**
   * 获取顶点的数量
   *
   * @param level 顶点层次
   * @return 顶点的数量
   */
  Long countVertices(String level);

  /**
   * 获取顶点的属性值
   *
   * @param vertex 顶点对象
   * @return 顶点的数量
   */
  Collection<PropertyValue> getPropertyValues(Vertex vertex);

  /**
   * 获取事件趋势
   *
   * @param id 事件id
   * @return 事件趋势
   */
  Page<EventItem> getEventTrend(String id, Pageable pageable);
}
