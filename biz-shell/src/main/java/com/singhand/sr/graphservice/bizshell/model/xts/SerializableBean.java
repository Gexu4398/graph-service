package com.singhand.sr.graphservice.bizshell.model.xts;

import cn.hutool.json.JSONUtil;
import java.io.Serial;
import java.io.Serializable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SerializableBean implements Serializable, Cloneable {

  @Serial
  private static final long serialVersionUID = 1L;

  public SerializableBean() {

  }

  public String toString() {

    return JSONUtil.toJsonStr(this);
  }

  public Object clone() {

    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      log.error("克隆对象出错", e);
      return null;
    }
  }
}
