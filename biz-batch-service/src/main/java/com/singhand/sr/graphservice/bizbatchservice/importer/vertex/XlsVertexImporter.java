package com.singhand.sr.graphservice.bizbatchservice.importer.vertex;

import static com.singhand.sr.graphservice.bizbatchservice.importer.helper.ExcelHelper.getCellValue;
import static com.singhand.sr.graphservice.bizbatchservice.importer.helper.ExcelHelper.getHeaders;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem;
import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem.PropertyItem;
import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem.RelationItem;
import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem.VertexItem;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationInstanceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationModelRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class XlsVertexImporter implements VertexImporter {

  private final VertexService vertexService;

  private final VertexRepository vertexRepository;

  private final OntologyRepository ontologyRepository;

  private final RelationModelRepository relationModelRepository;

  private final RelationInstanceRepository relationInstanceRepository;

  public XlsVertexImporter(VertexService vertexService, VertexRepository vertexRepository,
      OntologyRepository ontologyRepository, RelationModelRepository relationModelRepository,
      RelationInstanceRepository relationInstanceRepository) {

    this.vertexService = vertexService;
    this.vertexRepository = vertexRepository;
    this.ontologyRepository = ontologyRepository;
    this.relationModelRepository = relationModelRepository;
    this.relationInstanceRepository = relationInstanceRepository;
  }

  @Override
  public ImportVertexItem importFromFile(String filePath) throws Exception {

    @Cleanup final var inputStream = FileUtil.getInputStream(filePath);

    final var vertices = new ArrayList<VertexItem>();
    final var relations = new ArrayList<RelationItem>();

    final var workbook = new HSSFWorkbook(inputStream);

    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
      final var sheetName = workbook.getSheetName(i);
      log.info("正在处理第 {} 个 sheet: {}", i + 1, sheetName);

      if (sheetName.endsWith("-属性")) {
        processEntitySheet(workbook, workbook.getSheetAt(i), vertices);
      } else if (sheetName.endsWith("-关系")) {
        processRelationSheet(workbook, workbook.getSheetAt(i), relations);
      } else {
        log.warn("未知的工作格式: {}, 已跳过", sheetName);
      }
    }

    final var vertexItem = new ImportVertexItem();
    vertexItem.setEntities(vertices);
    vertexItem.setRelations(relations);
    return vertexItem;
  }

  private void processEntitySheet(@Nonnull Workbook workbook, @Nonnull HSSFSheet sheet,
      List<VertexItem> vertices) {

    final var rowIterator = sheet.iterator();
    if (!rowIterator.hasNext()) {
      log.warn("该 sheet 为空, 已跳过");
      return;
    }
    final var headerRow = rowIterator.next();

    final var headers = getHeaders(workbook, headerRow);
    Integer nameIndex = null;
    Integer typeIndex = null;
    for (final var entry : headers.entrySet()) {
      if (entry.getValue().equals("名称")) {
        nameIndex = entry.getKey();
      } else if (entry.getValue().equals("类型")) {
        typeIndex = entry.getKey();
      }
    }
    if (null == nameIndex || null == typeIndex) {
      return;
    }
    while (rowIterator.hasNext()) {
      final var row = rowIterator.next();
      final var nameCell = row.getCell(nameIndex);
      final var typeCell = row.getCell(typeIndex);
      if (null == nameCell || null == typeCell) {
        continue;
      }
      final var name = getCellValue(workbook, nameCell);
      final var type = getCellValue(workbook, typeCell);
      if (StrUtil.isBlank(name) || StrUtil.isBlank(type)) {
        continue;
      }

      final var vertexItem = new VertexItem();
      vertexItem.setName(name);
      vertexItem.setType(type);

      for (int i = 0; i < headers.size(); i++) {
        if (i == nameIndex || i == typeIndex) {
          continue;
        }
        final var cell = row.getCell(i);
        if (null == cell) {
          continue;
        }
        final var header = headers.get(i);
        final var value = getCellValue(workbook, cell);
        if (StrUtil.isBlank(value)) {
          continue;
        }

        final var strings = value.split("[；;]");
        final var values = Arrays.stream(strings).map(String::trim).toList();

        final var propertyItem = new PropertyItem();
        propertyItem.setKey(header);
        propertyItem.setValue(values);
        vertexItem.getProperties().add(propertyItem);
      }
      vertices.add(vertexItem);
    }
  }

  private void processRelationSheet(@Nonnull Workbook workbook, @Nonnull HSSFSheet sheet,
      List<RelationItem> relations) {

    final var rowIterator = sheet.iterator();
    if (!rowIterator.hasNext()) {
      log.warn("该 sheet 为空, 已跳过");
      return;
    }

    final var headerRow = rowIterator.next();
    final var headers = getHeaders(workbook, headerRow);

    Integer sourceEntityIndex = null;
    Integer sourceTypeIndex = null;
    Integer relationNameIndex = null;
    Integer targetEntityIndex = null;
    Integer targetTypeIndex = null;

    for (final var entry : headers.entrySet()) {
      String header = entry.getValue();
      if ("主语实体名称".equals(header)) {
        sourceEntityIndex = entry.getKey();
      } else if ("主语实体类型".equals(header)) {
        sourceTypeIndex = entry.getKey();
      } else if ("关系名称".equals(header)) {
        relationNameIndex = entry.getKey();
      } else if ("宾语实体名称".equals(header)) {
        targetEntityIndex = entry.getKey();
      } else if ("宾语实体类型".equals(header)) {
        targetTypeIndex = entry.getKey();
      }
    }

    if (null == sourceEntityIndex
        || null == sourceTypeIndex
        || null == relationNameIndex
        || null == targetEntityIndex
        || null == targetTypeIndex) {
      return;
    }

    while (rowIterator.hasNext()) {
      final var row = rowIterator.next();
      final var sourceEntityCell = row.getCell(sourceEntityIndex);
      final var sourceTypeCell = row.getCell(sourceTypeIndex);
      final var relationNameCell = row.getCell(relationNameIndex);
      final var targetEntityCell = row.getCell(targetEntityIndex);
      final var targetTypeCell = row.getCell(targetTypeIndex);

      if (null == sourceEntityCell
          || null == sourceTypeCell
          || null == relationNameCell
          || null == targetEntityCell
          || null == targetTypeCell) {
        continue;
      }

      final var sourceEntity = getCellValue(workbook, sourceEntityCell);
      final var sourceType = getCellValue(workbook, sourceTypeCell);
      final var relationName = getCellValue(workbook, relationNameCell);
      final var targetEntity = getCellValue(workbook, targetEntityCell);
      final var targetType = getCellValue(workbook, targetTypeCell);

      if (StrUtil.isBlank(sourceEntity)
          || StrUtil.isBlank(sourceType)
          || StrUtil.isBlank(relationName)
          || StrUtil.isBlank(targetEntity)
          || StrUtil.isBlank(targetType)) {
        continue;
      }

      final var inVertex = getVertex(sourceEntity, sourceType);
      final var outVertex = getVertex(targetEntity, targetType);
      if (null == inVertex || null == outVertex) {
        continue;
      }

      if (!relationModelRepository.existsByName(relationName)) {
        continue;
      }

      final var exists = relationInstanceRepository
          .existsByNameAndInOntology_NameAndOutOntology_Name(relationName, sourceType, targetType);

      if (!exists) {
        continue;
      }

      final var relationItem = new RelationItem();
      relationItem.setName(relationName);
      relationItem.setInVertex(VertexItem.builder()
          .name(inVertex.getName())
          .type(inVertex.getType())
          .build());
      relationItem.setOutVertex(VertexItem.builder()
          .name(outVertex.getName())
          .type(outVertex.getType())
          .build());
      relations.add(relationItem);
    }
  }

  private Vertex getVertex(String name, String type) {

    if (!ontologyRepository.existsByName(type)) {
      return null;
    }

    return vertexRepository.findByNameAndType(name, type).orElseGet(() -> {
      final var request = new NewVertexRequest();
      request.setName(name);
      request.setType(type);
      return vertexService.newVertex(request);
    });
  }

  @Override
  public boolean supports(String filePath) {

    return "xls".equalsIgnoreCase(FileNameUtil.extName(filePath));
  }
}