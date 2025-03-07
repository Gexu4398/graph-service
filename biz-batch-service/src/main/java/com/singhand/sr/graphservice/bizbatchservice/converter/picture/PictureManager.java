package com.singhand.sr.graphservice.bizbatchservice.converter.picture;

public interface PictureManager {

  String picture(byte[] bytes, String mime, String extName);
}
