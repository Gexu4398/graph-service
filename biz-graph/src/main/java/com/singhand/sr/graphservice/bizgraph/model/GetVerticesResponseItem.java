package com.singhand.sr.graphservice.bizgraph.model;

import cn.hutool.core.bean.BeanUtil;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Calendar;
import lombok.Data;

@Data
@Schema
public class GetVerticesResponseItem {

  private String ID;

  private String name;

  private String type;

  private Calendar createdAt;

  private Calendar updatedAt;

  public GetVerticesResponseItem(Vertex vertex) {

    BeanUtil.copyProperties(vertex, this);
  }
}
