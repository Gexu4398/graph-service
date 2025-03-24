package com.singhand.sr.graphservice.bizservice.demo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.config.OntologyBuildProperties;
import com.singhand.sr.graphservice.bizgraph.config.RelationModelBuildProperties;
import com.singhand.sr.graphservice.bizgraph.datastructure.TreeNode;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.RelationModelService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyPropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationModelRepository;
import jakarta.annotation.Nonnull;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Profile("dev")
@Transactional("bizTransactionManager")
public class OntologyDemoService implements DemoService {

  private final OntologyBuildProperties ontologyBuildProperties;

  private final RelationModelBuildProperties relationModelBuildProperties;

  private final OntologyService ontologyService;

  private final OntologyRepository ontologyRepository;

  private final OntologyPropertyRepository ontologyPropertyRepository;

  private final RelationModelRepository relationModelRepository;

  private final RelationModelService relationModelService;

  @Autowired
  public OntologyDemoService(OntologyBuildProperties ontologyBuildProperties,
      RelationModelBuildProperties relationModelBuildProperties,
      OntologyService ontologyService, OntologyRepository ontologyRepository,
      OntologyPropertyRepository ontologyPropertyRepository,
      RelationModelRepository relationModelRepository, RelationModelService relationModelService) {

    this.ontologyBuildProperties = ontologyBuildProperties;
    this.relationModelBuildProperties = relationModelBuildProperties;
    this.ontologyService = ontologyService;
    this.ontologyRepository = ontologyRepository;
    this.ontologyPropertyRepository = ontologyPropertyRepository;
    this.relationModelRepository = relationModelRepository;
    this.relationModelService = relationModelService;
  }

  @Override
  public void run() {

    final var types = ontologyBuildProperties.getTypes();
    types.forEach(this::processOntologyType);

    final var relationNames = relationModelBuildProperties.asSet();
    relationNames.forEach(it -> {
      final var exists = relationModelRepository.findByName(it);
      if (exists.isPresent()) {
        return;
      }
      relationModelService.newRelationModel(it);
    });
  }

  /**
   * 处理本体类型
   *
   * @param type 类型
   */
  private void processOntologyType(@Nonnull TreeNode type) {

    final var ontology = getOntology(type.getName(), null);
    if (CollUtil.isNotEmpty(type.getAttributes())) {
      addProperty(ontology, type.getAttributes());
    }
    if (CollUtil.isNotEmpty(type.getChildren())) {
      processChildren(ontology, type.getChildren());
    }
  }

  /**
   * 处理子类型
   *
   * @param parent   父类型
   * @param children 子类型
   */
  private void processChildren(@Nonnull Ontology parent, @Nonnull Set<TreeNode> children) {

    children.forEach(child -> {
      final var childOntology = getOntology(child.getName(), parent.getID());

      if (CollUtil.isNotEmpty(child.getAttributes())) {
        addProperty(childOntology, child.getAttributes());
      }

      if (CollUtil.isNotEmpty(child.getChildren())) {
        processChildren(childOntology, child.getChildren());
      }
    });
  }

  /**
   * 获取本体
   *
   * @param name     本体名称
   * @param parentId 父本体ID
   * @return 本体
   */
  private Ontology getOntology(String name, Long parentId) {

    return ontologyRepository.findByName(name)
        .orElseGet(() -> ontologyService.newOntology(name, parentId));
  }

  /**
   * 添加本体属性
   *
   * @param ontology   本体
   * @param attributes 属性
   */
  private void addProperty(@Nonnull Ontology ontology, @Nonnull Set<String> attributes) {

    attributes.forEach(attribute -> {
      final var exists = ontologyPropertyRepository.existsByOntologyAndName(ontology, attribute);
      if (!exists) {
        final var request = new NewOntologyPropertyRequest();
        request.setName(attribute);
        request.setType("文本");
        final var singleValue = isSingleValue(ontology, attribute);
        request.setMultiValue(!singleValue);
        ontologyService.newOntologyProperty(ontology, request);
      }
    });
  }

  /**
   * 判断是否是单值属性
   *
   * @param ontology  本体
   * @param attribute 属性
   * @return 是否是单值属性
   */
  private boolean isSingleValue(@Nonnull Ontology ontology, String attribute) {

    return ontology.getName().equals("事件") && StrUtil.equalsAny(attribute, "时间", "地点");
  }
}
