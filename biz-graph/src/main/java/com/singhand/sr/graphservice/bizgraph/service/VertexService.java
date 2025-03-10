package com.singhand.sr.graphservice.bizgraph.service;

import cn.hutool.crypto.digest.MD5;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEvidenceRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdatePropertyRequest;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Property;
import com.singhand.sr.graphservice.bizmodel.model.jpa.PropertyValue;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VertexService {

  Vertex newVertex(NewVertexRequest request);

  Optional<Vertex> getVertex(String id);

  Page<Vertex> getVertices(String keyword, Set<String> types, Map<String, String> keyValues,
      Pageable pageable);

  void deleteVertex(String id);

  void deleteVertices(List<String> vertexIds);

  CompletableFuture<Void> batchDeleteVertex(Set<String> types);

  CompletableFuture<Void> batchUpdateVertex(String oldType, String newType);

  Vertex updateVertex(String id, String name);

  void newProperty(Vertex vertex, NewPropertyRequest newPropertyRequest);

  Optional<Property> getProperty(Vertex vertex, String key);

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

  void updateProperty(Vertex vertex, UpdatePropertyRequest request);

  void deleteProperty(Vertex vertex, String key, String value, String mode);
}
