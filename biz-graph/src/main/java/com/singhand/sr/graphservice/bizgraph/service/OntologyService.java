package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizgraph.model.NewOntologyRequest;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;

public interface OntologyService {

  OntologyNode getOntology(String id);

  OntologyNode newOntology(NewOntologyRequest request);

  OntologyNode updateOntology(OntologyNode ontologyNode, NewOntologyRequest request);
}
