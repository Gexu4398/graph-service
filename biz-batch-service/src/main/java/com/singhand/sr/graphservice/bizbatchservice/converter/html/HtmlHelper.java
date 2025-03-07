package com.singhand.sr.graphservice.bizbatchservice.converter.html;

import java.util.Formatter;
import org.apache.poi.ss.usermodel.CellStyle;

/**
 * This interface is used where code wants to be independent of the workbook formats.  If you are
 * writing such code, you can add a method to this interface, and then implement it for both HSSF
 * and XSSF workbooks, letting the driving code stay independent of format.
 *
 * @author Ken Arnold, Industrious Media LLC
 */
public interface HtmlHelper {

  /**
   * Outputs the appropriate CSS style for the given cell style.
   *
   * @param style The cell style.
   * @param out   The place to write the output.
   */
  void colorStyles(CellStyle style, Formatter out);
}