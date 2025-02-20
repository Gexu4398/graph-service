package com.singhand.sr.graphservice.bizservice.aspect.annotation.resolver;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizkeycloakmodel.helper.JwtHelper;
import com.singhand.sr.graphservice.bizservice.aspect.annotation.bizlogger.BizLoggerAspect;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class DefaultResolver implements Resolver {

  static Pattern pattern = Pattern.compile("^'(?<text>.*)'$");

  static Object getValueFromRequestPath(String beanPath) {

    final var path = StrUtil.subAfter(beanPath, ".", true);
    final var request = BizLoggerAspect.getRequest();
    final var attributes = (Map<?, ?>) request.getAttribute(
        HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    return attributes.get(path);
  }

  @SneakyThrows
  static Object getValueFromRequestBody(ProceedingJoinPoint joinPoint, String beanPath) {

    final var methodSignature = (MethodSignature) joinPoint.getSignature();
    final var method = methodSignature.getMethod();
    final var parameterAnnotations = method.getParameterAnnotations();
    final var args = joinPoint.getArgs();
    assert args.length == parameterAnnotations.length;

    for (var i = 0; i < args.length; i++) {
      for (var annotation : parameterAnnotations[i]) {
        if (annotation instanceof RequestBody) {
          final var bean = args[i];
          final var path = StrUtil.subAfter(beanPath, "request.body.", true);
          return BeanUtil.getProperty(bean, path);
        }
      }
    }
    throw new Exception("BizLogger 中没有发现 ResponseBody 参数！");
  }

  @SneakyThrows
  static Object getValueFromRequestParam(String beanPath) {

    final var param = StrUtil.subAfter(beanPath, ".", true);
    final var request = BizLoggerAspect.getRequest();
    final var values = request.getParameterValues(param);
    return values == null ? ""
        : StrUtil.join(",", Arrays.asList(request.getParameterValues(param)));
  }

  @SneakyThrows
  static Object getValueFromToken(String beanPath) {

    final var param = StrUtil.subAfter(beanPath, ".", true);
    return switch (param) {
      case "username" -> JwtHelper.getUsername();
      case "name" -> JwtHelper.getName();
      case "userId" -> JwtHelper.getUserId();
      default -> "unknown field " + param;
    };
  }

  @Override
  public Object getProperty(ProceedingJoinPoint joinPoint, Object proceed, String beanPath)
      throws Exception {

    if (beanPath.startsWith("response.")) {
      final var path = StrUtil.subAfter(beanPath, ".", true);
      // 没取到值说明返回结果的结构跟预想的不一致，属于开发和设计阶段的错误，需要及时纠正，因此直接抛出异常。
      return BeanUtil.getProperty(proceed, path);
    } else if (beanPath.startsWith("request.path")) {
      return getValueFromRequestPath(beanPath);
    } else if (beanPath.startsWith("request.param")) {
      return getValueFromRequestParam(beanPath);
    } else if (beanPath.startsWith("request.body")) {
      // 不支持多个 RequestBody
      return getValueFromRequestBody(joinPoint, beanPath);
    } else if (beanPath.startsWith("token")) {
      return getValueFromToken(beanPath);
    } else {
      final var matcher = pattern.matcher(beanPath);
      if (matcher.matches()) {
        return matcher.group("text");
      }
      throw new Exception(String.format("未知 beanPath 表达式: %s", beanPath));
    }
  }
}
