package com.singhand.sr.graphservice.bizgraph.model;

import cn.hutool.core.bean.BeanUtil;
import com.singhand.sr.graphservice.bizgraph.util.CalendarUtil;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import java.util.Calendar;
import java.util.Optional;
import lombok.Data;

@Data
public class EventItem {

  private String ID;

  private String name;

  private String type;

  private Calendar time;

  private String timeStr;

  private String location;

  private String trigger;

  private String inVertex;

  private String outVertex;

  public EventItem(Vertex vertex) {

    BeanUtil.copyProperties(vertex, this);

    Optional.ofNullable(vertex.getProperties().get("时间"))
        .ifPresent(property -> {
          timeStr = property.getMaxConfidenceOrBlankValue().getValue();
          time = CalendarUtil.format(timeStr);
        });

    Optional.ofNullable(vertex.getProperties().get("地点"))
        .ifPresent(property -> location = property.getMaxConfidenceOrBlankValue().getValue());
  }
}
