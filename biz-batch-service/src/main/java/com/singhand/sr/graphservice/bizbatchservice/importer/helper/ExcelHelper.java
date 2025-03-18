package com.singhand.sr.graphservice.bizbatchservice.importer.helper;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

public interface ExcelHelper {

  /**
   * 获取单元格的值
   *
   * @param workbook 工作簿
   * @param cell     单元格
   * @return 单元格的值
   */
  static String getCellValue(@Nonnull Workbook workbook, @Nonnull Cell cell) {

    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> String.valueOf(cell.getNumericCellValue());
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> {
        final var formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
        yield getCellValue(workbook, formulaEvaluator.evaluateInCell(cell));
      }
      default -> "";
    };
  }

  static @Nonnull Map<Integer, String> getHeaders(@Nonnull Workbook workbook, @Nonnull Row row) {

    final var headers = new HashMap<Integer, String>();
    for (int i = 0; i < row.getLastCellNum(); i++) {
      final var cell = row.getCell(i);
      if (null != cell) {
        final var cellValue = getCellValue(workbook, cell);
        headers.put(i, cellValue);
      }
    }
    return headers;
  }
}
