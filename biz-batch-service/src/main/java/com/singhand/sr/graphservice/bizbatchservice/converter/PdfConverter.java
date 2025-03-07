package com.singhand.sr.graphservice.bizbatchservice.converter;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizbatchservice.converter.picture.Base64PictureManager;
import com.singhand.sr.graphservice.bizbatchservice.converter.picture.PictureManager;
import io.github.se_be.pdf2dom.PDFDomTree;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.Cleanup;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.usermodel.PictureType;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;

public class PdfConverter {

  private final PictureManager pictureManager;

  public PdfConverter(PictureManager pictureManager) {

    this.pictureManager = pictureManager;
  }

  public String pdf2html(String pdfPath) throws Exception {

    @Cleanup final var document = Loader.loadPDF(
        new RandomAccessReadBufferedFile(new File(pdfPath)));
    @Cleanup final var output = new ByteArrayOutputStream();
    @Cleanup final var writer = IoUtil.getWriter(output, StandardCharsets.UTF_8);
    final var domTree = new PDFDomTree();
    final var dom = domTree.createDOM(document);

    final var images = dom.getElementsByTagName("img");
    final var imageNum = images.getLength();
    for (var i = 0; i < imageNum; i++) {
      final var image = images.item(i);
      final var src = image.getAttributes().getNamedItem("src");
      final var base64 = StrUtil.subAfter(src.getNodeValue(), "base64,", true).trim();
      final var key = resolvePicture(base64);
      src.setNodeValue(key);
    }

    final var registry = DOMImplementationRegistry.newInstance();
    final var impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
    final var lsSerializer = impl.createLSSerializer();
    final var lsOutput = impl.createLSOutput();
    lsSerializer.getDomConfig().setParameter("format-pretty-print", true);
    lsOutput.setCharacterStream(writer);
    lsSerializer.write(dom, lsOutput);
    final var str = IoUtil.toStr(output, StandardCharsets.UTF_8);
    writer.close();
    output.close();
    return str;
  }

  private String resolvePicture(String base64) {

    if (pictureManager instanceof Base64PictureManager) {
      return base64;
    }

    final var bytes = Base64.decode(base64);
    final var pictureType = PictureType.findMatchingType(bytes);
    return pictureManager.picture(bytes, pictureType.getMime(), pictureType.getExtension());
  }

  public List<String> pdf2lines(String pdfPath) throws Exception {

    final var file = new File(pdfPath);
    @Cleanup final var document = Loader.loadPDF(new RandomAccessReadBufferedFile(file));
    final var numberOfPages = document.getNumberOfPages();
    final var stripper = new PDFTextStripper();
    final var contents = new ArrayList<String>();
    for (int i = 1; i <= numberOfPages; i++) {
      stripper.setStartPage(i);
      stripper.setEndPage(i);
      contents.addAll(StrUtil.split(stripper.getText(document), "\n"));
    }
    return contents;
  }

  public String pdf2txt(String pdfPath) throws Exception {

    return StrUtil.join("", pdf2lines(pdfPath));
  }
}
