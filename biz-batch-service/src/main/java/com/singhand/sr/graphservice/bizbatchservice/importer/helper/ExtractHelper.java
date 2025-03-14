package com.singhand.sr.graphservice.bizbatchservice.importer.helper;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizbatchservice.converter.MsWordConverter;
import com.singhand.sr.graphservice.bizbatchservice.converter.PdfConverter;
import com.singhand.sr.graphservice.bizbatchservice.converter.TxtConverter;
import com.singhand.sr.graphservice.bizbatchservice.converter.picture.S3PictureManager;
import com.singhand.sr.graphservice.bizmodel.model.jpa.DatasourceContent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExtractHelper {

  private final MsWordConverter msWordConverter;

  private final PdfConverter pdfConverter;

  @Autowired
  public ExtractHelper(S3PictureManager s3PictureManager) {

    this.msWordConverter = new MsWordConverter(s3PictureManager);
    this.pdfConverter = new PdfConverter(s3PictureManager);
  }

  public List<String> extractFile(String tempFilename, DatasourceContent datasourceContent)
      throws Exception {

    final var extName = FileNameUtil.extName(tempFilename).toLowerCase();

    return switch (extName) {
      case "txt" -> {
        final var str = FileUtil.readString(tempFilename, StandardCharsets.UTF_8);
        datasourceContent.setHtml(TxtConverter.str2html(str));
        datasourceContent.setText(str);
        yield StrUtil.split(str, "\n");
      }
      case "docx" -> {
        datasourceContent.setHtml(msWordConverter.docx2html(tempFilename));
        datasourceContent.setText(msWordConverter.docx2txt(tempFilename));
        yield msWordConverter.docx2lines(tempFilename);
      }
      case "doc" -> {
        datasourceContent.setHtml(msWordConverter.doc2html(tempFilename));
        datasourceContent.setText(msWordConverter.doc2txt(tempFilename));
        yield msWordConverter.doc2lines(tempFilename);
      }
      case "pdf" -> {
        datasourceContent.setHtml(pdfConverter.pdf2html(tempFilename));
        datasourceContent.setText(pdfConverter.pdf2txt(tempFilename));
        yield pdfConverter.pdf2lines(tempFilename);
      }
      default -> {
        log.warn("不支持的文件类型：{}", extName);
        yield List.of();
      }
    };
  }
}
