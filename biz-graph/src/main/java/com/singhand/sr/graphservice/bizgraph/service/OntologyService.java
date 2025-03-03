package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdateOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyPropertyNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.dto.OntologyTreeDTO;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OntologyService {

  OntologyNode getOntology(String id);

  OntologyNode newOntology(NewOntologyRequest request);

  OntologyNode updateOntology(OntologyNode ontologyNode, NewOntologyRequest request);

  void deleteOntology(OntologyNode ontologyNode);

  List<OntologyTreeDTO> getOntologyTree();

  Page<OntologyNode> getOntologies(String keyword, Pageable pageable);

  OntologyNode newProperty(OntologyNode ontology, NewOntologyPropertyRequest request);

  void deleteProperties(OntologyNode ontology, Set<String> propertyIds);

  OntologyNode updateProperty(OntologyNode ontology, OntologyPropertyNode propertyNode,
      UpdateOntologyPropertyRequest request);
}
