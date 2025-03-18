package com.singhand.sr.graphservice.bizbatchservice.importer.vertex;

import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem;

public interface VertexImporter {

  /**
   * 导入文件
   *
   * @param filePath 文件路径
   * @return 导入的顶点
   * @throws Exception 异常
   */
  ImportVertexItem importFromFile(String filePath) throws Exception;

  /**
   * 是否支持该文件格式
   *
   * @param filePath 文件路径
   * @return 是否支持
   */
  boolean supports(String filePath);
}
