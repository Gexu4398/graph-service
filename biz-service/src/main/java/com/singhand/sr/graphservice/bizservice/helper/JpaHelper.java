package com.singhand.sr.graphservice.bizservice.helper;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.ReflectUtil;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public final class JpaHelper {

  public static <T> Pair<Map<Object, ?>, Map<Object, ?>> updateOneToMany(T managedOne, T inputOne,
      String forwardFieldName, String backwardFieldName, String... ignoreFields) {

    final var ot = ReflectUtil.getField(managedOne.getClass(), forwardFieldName).getType();
    final var nt = ReflectUtil.getField(inputOne.getClass(), forwardFieldName).getType();

    assert ot.equals(nt);

    final var os = ReflectUtil.getFieldValue(managedOne, forwardFieldName);
    final var ns = ReflectUtil.getFieldValue(inputOne, forwardFieldName);

    assert os instanceof Collection<?> && ns instanceof Collection<?>;

    final var oldSet = (Collection<?>) os;
    final var newSet = (Collection<?>) ns;

    oldSet.stream().filter(it -> !newSet.contains(it))
        .forEach(it -> ReflectUtil.setFieldValue(it, backwardFieldName, null));
    ReflectUtil.invoke(oldSet, "retainAll", newSet);
    ReflectUtil.invoke(oldSet, "addAll", newSet);
    newSet.forEach(it -> ReflectUtil.setFieldValue(it, backwardFieldName, managedOne));

    final var om = oldSet.stream()
        .filter(it -> ReflectUtil.getFieldValue(it, "ID") != null)
        .collect(Collectors.toMap(it -> ReflectUtil.getFieldValue(it, "ID"), it -> it));
    final var nm = newSet.stream()
        .filter(it -> ReflectUtil.getFieldValue(it, "ID") != null)
        .collect(Collectors.toMap(it -> ReflectUtil.getFieldValue(it, "ID"), it -> it));

    om.forEach((k, v) -> BeanUtil.copyProperties(nm.get(k), v, ignoreFields));

    return Pair.of(om, nm);
  }
}
