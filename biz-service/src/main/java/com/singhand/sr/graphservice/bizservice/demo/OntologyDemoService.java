package com.singhand.sr.graphservice.bizservice.demo;

import cn.hutool.core.collection.CollUtil;
import com.singhand.sr.graphservice.bizgraph.config.OntologyBuildProperties;
import com.singhand.sr.graphservice.bizgraph.datastructure.TreeNode;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyPropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
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

  private final OntologyService ontologyService;

  private final OntologyRepository ontologyRepository;

  private final OntologyPropertyRepository ontologyPropertyRepository;

  @Autowired
  public OntologyDemoService(OntologyBuildProperties ontologyBuildProperties,
      OntologyService ontologyService, OntologyRepository ontologyRepository,
      OntologyPropertyRepository ontologyPropertyRepository) {

    this.ontologyBuildProperties = ontologyBuildProperties;
    this.ontologyService = ontologyService;
    this.ontologyRepository = ontologyRepository;
    this.ontologyPropertyRepository = ontologyPropertyRepository;
  }

  @Override
  public void run() {

    final var types = ontologyBuildProperties.getTypes();
    types.forEach(this::processOntologyType);
  }

  private void processOntologyType(@Nonnull TreeNode type) {

    final var ontology = getOntology(type.getName(), null);
    if (CollUtil.isNotEmpty(type.getAttributes())) {
      addProperty(ontology, type.getAttributes());
    }
    if (CollUtil.isNotEmpty(type.getChildren())) {
      processChildren(ontology, type.getChildren());
    }
  }

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

  private Ontology getOntology(String name, Long parentId) {

    return ontologyRepository.findByName(name)
        .orElseGet(() -> ontologyService.newOntology(name, parentId));
  }

  private void addProperty(@Nonnull Ontology ontology, @Nonnull Set<String> attributes) {

    attributes.forEach(attribute -> {
      final var exists = ontologyPropertyRepository.existsByOntologyAndName(ontology, attribute);
      if (!exists) {
        final var request = new NewOntologyPropertyRequest();
        request.setName(attribute);
        request.setType("文本");
        request.setMultiValue(true);
        ontologyService.newOntologyProperty(ontology, request);
      }
    });
  }
}
