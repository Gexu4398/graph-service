package com.singhand.sr.graphservice.bizgraph.service;

import cn.hutool.crypto.digest.MD5;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VertexService {

  Vertex newVertex(NewVertexRequest request);

  Optional<Vertex> getVertex(String id);

  Page<Vertex> getVertices(String keyword, Set<String> types, Map<String, String> keyValues,
      Pageable pageable);

  void deleteVertex(String id);

  void deleteVertices(List<String> vertexIds);

  Vertex updateVertex(String id, String name);

  void newProperty(Vertex vertex, NewPropertyRequest newPropertyRequest);

  void newProperty(Edge edge, NewPropertyRequest newPropertyRequest);

  Optional<Property> getProperty(Vertex vertex, String key);

  Optional<Property> getProperty(Edge edge, String key);

  void setVerified(Vertex vertex, String key, String value);

  default Optional<PropertyValue> getPropertyValue(@Nonnull Property property,
      @Nonnull String value) {

    return getPropertyValue(property, value, "raw");
  }

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

  void setFeature(PropertyValue propertyValue, String key, String value);

  void addEvidence(PropertyValue propertyValue, NewEvidenceRequest newEvidenceRequest);

  void addEvidence(Edge edge, NewEvidenceRequest newEvidenceRequest);

  void updateProperty(Vertex vertex, UpdatePropertyRequest request);

  void updateProperty(Edge edge, UpdatePropertyRequest updatePropertyRequest);

  void deletePropertyValue(Vertex vertex, String key, String value, String mode);

  void deleteProperty(Vertex vertex, String key);

  void deleteProperty(Edge edge, String key, String value, String mode);

  Edge newEdge(Vertex inVertex, Vertex outVertex, NewEdgeRequest request);

  void setFeature(Edge edge, String key, String value);

  void setVerified(Edge edge);

  Optional<Edge> getEdge(String name, Vertex inVertex, Vertex outVertex, String scope);

  void setVerified(Edge edge, String key, String value);

  void deleteEdge(Edge edge);

  void updateEdge(Edge oldEdge, UpdateEdgeRequest request);

  void batchDeleteVertex(Set<String> types);

  void batchUpdateVertex(String oldType, String newType);

  void batchUpdateVertexProperty(String vertexType, String oldKey, String newKey);

  void batchDeleteVertexProperty(String vertexType, String key);

  void batchUpdateVertexEdge(String name, String newName, String inVertexType,
      String outVertexType);

  void batchUpdateVertexEdge(String name, String newName);

  void batchDeleteVertexEdge(String name, String inVertexType, String outVertexType);

  void batchDeleteVertexEdge(String name);

  Page<Evidence> getEvidences(Vertex vertex, String key, String value, String mode,
      Pageable pageable);

  Page<Evidence> getEvidences(Edge edge, String key, String value, String mode, Pageable pageable);

  void setChecked(Vertex vertex, String key, String value);

  void setChecked(Edge edge, String key, String value);

  void setChecked(Edge edge);
}
