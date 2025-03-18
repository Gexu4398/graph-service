package com.singhand.sr.graphservice.bizbatchservice.importer.vertex;

import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem;

public interface VertexImporter {

  ImportVertexItem importFromFile(String filePath) throws Exception;

  boolean supports(String filePath);
}
