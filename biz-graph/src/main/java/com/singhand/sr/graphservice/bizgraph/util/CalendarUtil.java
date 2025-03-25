package com.singhand.sr.graphservice.bizgraph.util;

import cn.hutool.core.date.DatePattern;
import jakarta.annotation.Nonnull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;

public class CalendarUtil {

  private static final Set<SimpleDateFormat> dateFormats = Set.of(
      new SimpleDateFormat(DatePattern.NORM_DATETIME_PATTERN),
      new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
      new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"),
      new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"),
      new SimpleDateFormat(DatePattern.NORM_DATE_PATTERN),
      new SimpleDateFormat("dd-MM-yyyy"),
      new SimpleDateFormat("MM/dd/yyyy"),
      new SimpleDateFormat(DatePattern.CHINESE_DATE_PATTERN),
      new SimpleDateFormat(DatePattern.PURE_DATETIME_PATTERN),
      new SimpleDateFormat(DatePattern.PURE_DATETIME_MS_PATTERN),
      new SimpleDateFormat(DatePattern.NORM_DATETIME_MINUTE_PATTERN),
      new SimpleDateFormat(DatePattern.CHINESE_DATE_TIME_PATTERN),
      new SimpleDateFormat(DatePattern.PURE_DATE_PATTERN)
  );

  public static @Nonnull Calendar format(@Nonnull String time) {

    for (final var sdf : dateFormats) {
      try {
        final var date = sdf.parse(time);
        final var calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
      } catch (ParseException e) {
        throw new IllegalArgumentException("无效的日期格式: " + time);
      }
    }
    throw new IllegalArgumentException("无效的日期格式: " + time);
  }
}
