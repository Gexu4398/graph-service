package com.singhand.sr.graphservice.bizbatchservice.converter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizbatchservice.converter.picture.PictureManager;
import java.io.File;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.usermodel.PictureType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.OpcPackage;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;

@Slf4j
public class MsWordConverter {

  private final PictureManager pictureManager;

  public MsWordConverter(PictureManager pictureManager) {

    this.pictureManager = pictureManager;
  }

  public static String getParagraphNumLevelText(XWPFParagraph paragraph) {

    if (paragraph.getNumLevelText() != null) {
      return paragraph.getNumLevelText();
    }

    final var styleId = paragraph.getStyleID();
    if (styleId == null) {
      return null;
    }

    final var style = paragraph.getDocument().getStyles().getStyle(styleId);
    if (style == null) {
      return null;
    }

    try {
      final var numbering = paragraph.getDocument().getNumbering();
      final var numId = style.getCTStyle().getPPr().getNumPr().getNumId().getVal();
      if (numId != null && numbering != null) {
        final var num = numbering.getNum(numId);
        if (num != null) {
          final var ilvl = style.getCTStyle().getPPr().getNumPr().getIlvl().getVal();
          final var ctNum = num.getCTNum();
          if (ctNum == null) {
            return null;
          }

          final var ctDecimalNumber = ctNum.getAbstractNumId();
          if (ctDecimalNumber == null) {
            return null;
          }

          final var abstractNumId = ctDecimalNumber.getVal();
          if (abstractNumId == null) {
            return null;
          }

          final var xwpfAbstractNum = numbering.getAbstractNum(abstractNumId);

          if (xwpfAbstractNum == null) {
            return null;
          }

          final var anum = xwpfAbstractNum.getCTAbstractNum();

          if (anum == null) {
            return null;
          }

          CTLvl level = null;
          for (int i = 0; i < anum.sizeOfLvlArray(); i++) {
            final var lvl = anum.getLvlArray(i);
            if (lvl != null && lvl.getIlvl() != null && lvl.getIlvl().equals(ilvl)) {
              level = lvl;
              break;
            }
          }
          if (level != null && level.getLvlText() != null
              && level.getLvlText().getVal() != null) {
            return level.getLvlText().getVal();
          }
        }
      }
      return null;
    } catch (Exception ex) {
      return null;
    }
  }

  private static BigInteger getParagraphNumID(XWPFParagraph paragraph) {

    if (paragraph.getNumID() != null) {
      return paragraph.getNumID();
    }

    final var styleId = paragraph.getStyleID();
    if (styleId == null) {
      return null;
    }

    if (paragraph.getDocument().getStyles() == null) {
      return null;
    }

    final var style = paragraph.getDocument().getStyles().getStyle(styleId);
    if (style == null) {
      return null;
    }

    try {
      return style.getCTStyle().getPPr().getNumPr().getNumId().getVal();
    } catch (Exception ex) {
      return null;
    }
  }

  private static BigInteger getParagraphNumILvlID(XWPFParagraph paragraph) {

    if (paragraph.getNumIlvl() != null) {
      return paragraph.getNumIlvl();
    }

    final var styleId = paragraph.getStyleID();
    if (styleId == null) {
      return null;
    }

    final var style = paragraph.getDocument().getStyles().getStyle(styleId);
    if (style == null) {
      return null;
    }

    try {
      return style.getCTStyle().getPPr().getNumPr().getIlvl().getVal();
    } catch (Exception ex) {
      return null;
    }
  }

  public String doc2html(String docPath) throws Exception {

    @Cleanup final var document = new HWPFDocument(FileUtil.getInputStream(docPath));
    final var newDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    final var wordToHtmlConverter = new WordToHtmlConverter(newDocument);
    wordToHtmlConverter.setPicturesManager(
        (content, pictureType, suggestedName, widthInches, heightInches) ->
            pictureManager.picture(content, pictureType.getMime(), pictureType.getExtension()));
    wordToHtmlConverter.processDocument(document);
    final var stringWriter = new StringWriter();
    final var transformer = TransformerFactory.newInstance()
        .newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
    transformer.setOutputProperty(OutputKeys.METHOD, "html");
    transformer.transform(
        new DOMSource(wordToHtmlConverter.getDocument()),
        new StreamResult(stringWriter));
    return stringWriter.toString();
  }

  public String docx2html(String docxPath) throws Exception {

    @Cleanup final var inputStream = FileUtil.getInputStream(docxPath);
    final var opcPackage = OpcPackage.load(inputStream);
    final var tempFilename = File.createTempFile("docx2html", ".html");
    @Cleanup final var outputStream = FileUtil.getOutputStream(tempFilename);
    final var htmlSettings = Docx4J.createHTMLSettings();
    htmlSettings.setOpcPackage(opcPackage);
    htmlSettings.setImageHandler((abstractWordXmlPicture, relationship, binaryPart) -> {
      final var pictureType = PictureType.findMatchingType(binaryPart.getBytes());
      return pictureManager.picture(binaryPart.getBytes(), pictureType.getMime(),
          pictureType.getExtension());
    });
    Docx4J.toHTML(htmlSettings, outputStream, Docx4J.FLAG_EXPORT_PREFER_XSL);
    return StrUtil.join("",
        FileUtil.readLines(tempFilename.getAbsolutePath(), StandardCharsets.UTF_8));
  }

  public List<String> doc2lines(String docPath) throws Exception {

    @Cleanup final var inputStream = FileUtil.getInputStream(docPath);
    final var document = new HWPFDocument(inputStream);
    final var range = document.getRange();
    final var paragraphs = range.numParagraphs();
    final var contents = new ArrayList<String>();
    for (int i = 0; i < paragraphs; i++) {
      final var paragraph = range.getParagraph(i);
      final var text = paragraph.text();
      contents.add(text);
    }
    return contents;
  }

  private void removeFootnotes(String docxPath) throws Exception {

    @Cleanup final var inputStream = FileUtil.getInputStream(docxPath);
    @Cleanup final var document = new XWPFDocument(inputStream);
    document.getFootnotes().clear();
    for (final var paragraph : document.getParagraphs()) {
      paragraph.getRuns().forEach(xwpfRun -> {
        if (CollUtil.isNotEmpty(xwpfRun.getCTR().getFootnoteRefList())) {
          xwpfRun.getCTR().getFootnoteRefList().clear();
        }
        if (CollUtil.isNotEmpty(xwpfRun.getCTR().getFootnoteReferenceList())) {
          xwpfRun.getCTR().getFootnoteReferenceList().clear();
        }
      });
    }
    final var outputStream = FileUtil.getOutputStream(docxPath);
    document.write(outputStream);
    outputStream.close();
    document.close();
  }

  public List<String> docx2lines(String docxPath) throws Exception {

    removeFootnotes(docxPath);

    @Cleanup final var inputStream = FileUtil.getInputStream(docxPath);
    @Cleanup final var document = new XWPFDocument(inputStream);
    final var paragraphs = new LinkedList<String>();
    final var numMapping = new HashMap<BigInteger, ArrayList<Integer>>();
    for (final var paragraph : document.getParagraphs()) {
      if (paragraph.getText().isBlank()) {
        continue;
      }
      final var numID = getParagraphNumID(paragraph);
      if (numID == null) {
        paragraphs.add(paragraph.getText());
      } else {
        numMapping.putIfAbsent(numID, new ArrayList<>(List.of(0)));
        var levelDepth = getParagraphNumILvlID(paragraph);
        var nums = numMapping.get(numID);
        if (nums.isEmpty()) {
          nums.add(1);
        } else {
          if (levelDepth == null) {
            levelDepth = BigInteger.ONE;
          }
          if (levelDepth.intValue() < nums.size() - 1) {
            nums = new ArrayList<>(nums.subList(0, levelDepth.intValue() + 1));
            nums.set(levelDepth.intValue(), nums.get(levelDepth.intValue()) + 1);
          } else {
            nums.add(0);
          }
          numMapping.put(numID, nums);
        }
        var fmt = getParagraphNumLevelText(paragraph);
        if (fmt != null) {
          for (var i = 0; i < nums.size(); i++) {
            final var numFmt = StrUtil.blankToDefault(paragraph.getNumFmt(), "decimal");
            log.debug("文档中包含的列表样式：{}", numFmt);
            final var value = switch (numFmt) {
              case "lowerLetter" -> String.format("%c", nums.get(i) + 'a' - 1);
              case "upperLetter" -> String.format("%c", nums.get(i) + 'A' - 1);
              case "chineseCounting", "decimalEnclosedCircleChinese" -> nums.get(i).toString();
              default -> nums.get(i).toString();
            };
            fmt = fmt.replaceAll("%" + (i + 1), value);
          }
        }
        paragraphs.add(fmt + " " + paragraph.getText());
      }
    }
    return paragraphs;
  }

  public String doc2txt(String docPath) throws Exception {

    return StrUtil.join("\n", doc2lines(docPath));
  }

  public String docx2txt(String docxPath) throws Exception {

    return StrUtil.join("\n", docx2lines(docxPath));
  }
}
