package com.singhand.sr.graphservice.bizbatchservice.converter.picture;

import cn.hutool.core.codec.Base64;
import org.springframework.stereotype.Component;

@Component
public class Base64PictureManager implements PictureManager {

  @Override
  public String picture(byte[] bytes, String mime, String extName) {

    return String.format("data:%s;base64,%s", mime, Base64.encode(bytes));
  }
}
