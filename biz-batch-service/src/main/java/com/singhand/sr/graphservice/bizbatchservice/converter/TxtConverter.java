package com.singhand.sr.graphservice.bizbatchservice.converter;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import java.nio.charset.StandardCharsets;

public class TxtConverter {

  public static String txt2html(String txtPath) {

    return str2html(FileUtil.readString(txtPath, StandardCharsets.UTF_8));
  }

  public static String str2html(String str) {

    return StrUtil.split(str, "\n")
        .stream()
        .map(it -> "<p>" + it + "</p>")
        .reduce((a, b) -> a + b)
        .orElse("");
  }
}
