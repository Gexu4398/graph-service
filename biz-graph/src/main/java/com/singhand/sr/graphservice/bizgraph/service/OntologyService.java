package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizgraph.model.NewOntologyRequest;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.dto.OntologyTreeDTO;
import java.util.List;

public interface OntologyService {

  OntologyNode getOntology(String id);

  OntologyNode newOntology(NewOntologyRequest request);

  OntologyNode updateOntology(OntologyNode ontologyNode, NewOntologyRequest request);

  void deleteOntology(OntologyNode ontologyNode);

  List<OntologyTreeDTO> getOntologyTree();
}
