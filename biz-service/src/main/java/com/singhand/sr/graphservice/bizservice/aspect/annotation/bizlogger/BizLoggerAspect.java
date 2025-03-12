package com.singhand.sr.graphservice.bizservice.aspect.annotation.bizlogger;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.singhand.sr.graphservice.bizkeycloakmodel.helper.JwtHelper;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.KeycloakRole;
import com.singhand.sr.graphservice.bizkeycloakmodel.repository.UserEntityRepository;
import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.BizLog;
import com.singhand.sr.graphservice.bizmodel.model.jpa.BizLogTarget;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.BizLogRepository;
import com.singhand.sr.graphservice.bizservice.aspect.annotation.resolver.Resolve;
import com.singhand.sr.graphservice.bizservice.aspect.annotation.resolver.Resolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class BizLoggerAspect {

  private final ApplicationContext applicationContext;

  private final BizLogRepository bizLogRepository;

  private final UserEntityRepository userEntityRepository;

  private final KeycloakService keycloakService;

  private final PlatformTransactionManager keycloakTransactionManager;

  @Autowired
  public BizLoggerAspect(ApplicationContext applicationContext, BizLogRepository bizLogRepository,
      UserEntityRepository userEntityRepository, KeycloakService keycloakService,
      @Qualifier("keycloakTransactionManager") PlatformTransactionManager keycloakTransactionManager) {

    this.applicationContext = applicationContext;
    this.bizLogRepository = bizLogRepository;
    this.userEntityRepository = userEntityRepository;
    this.keycloakService = keycloakService;
    this.keycloakTransactionManager = keycloakTransactionManager;
  }

  public static HttpServletRequest getRequest() {

    return ((ServletRequestAttributes) Objects.requireNonNull(
        RequestContextHolder.getRequestAttributes())).getRequest();
  }

  private ArrayList<String> resolveValuesNotResponse(ProceedingJoinPoint joinPoint,
      Resolve... resolves) {

    final var contentValues = new ArrayList<String>();
    Arrays.stream(resolves).forEachOrdered(resolve -> {
      final var beanPath = resolve.value();
      contentValues.add(beanPath.startsWith("response") ?
          "" : getValueFromResolve(joinPoint, resolve));
    });
    return contentValues;
  }

  private void resolveValuesNeedResponse(ProceedingJoinPoint joinPoint, Object proceed,
      Resolve[] resolves, List<String> values) {

    for (var i = 0; i < resolves.length; i++) {
      final var beanPath = resolves[i].value();
      if (beanPath.startsWith("response")) {
        values.set(i, getValueFromResolve(joinPoint, proceed, resolves[i]));
      }
    }
  }

  private String getValueFromResolve(ProceedingJoinPoint joinPoint, Resolve resolve) {

    return getValueFromResolve(joinPoint, null, resolve);
  }

  private String getValueFromResolve(ProceedingJoinPoint joinPoint, Object proceed,
      Resolve resolve) {

    try {
      final var resolver = (Resolver) applicationContext.getBean(resolve.resolver());
      return resolver.getProperty(joinPoint, proceed, resolve.value()).toString();
    } catch (Exception e) {
      log.error("日志取值出错 beanPath = {}", resolve.value(), e);
      return "";
    }
  }

  @Around("@annotation(bizLogger)")
  @SneakyThrows
  public Object around(ProceedingJoinPoint joinPoint, BizLogger bizLogger) {

    // 执行前
    final var moduleResolves = resolveValuesNotResponse(joinPoint, bizLogger.module());
    final var type = bizLogger.type();
    final var contentFormat = bizLogger.contentFormat();
    final var contentResolves = resolveValuesNotResponse(joinPoint,
        bizLogger.contentFormatArguments());
    final var targetResolves = resolveValuesNotResponse(joinPoint, bizLogger.targetId(),
        bizLogger.targetName(), bizLogger.targetType());

    final var bizLog = new BizLog();
    String username;
    if (bizLogger.isLogin()) {
      username = JwtHelper.getUsername();
    } else {
      username = targetResolves.get(1);
    }
    bizLog.setUsername(username);
    bizLog.setType(type);
    bizLog.setIp(JakartaServletUtil.getClientIP(getRequest()));
    // 函数执行
    final var proceed = joinPoint.proceed();

    // 执行后
    resolveValuesNeedResponse(joinPoint, proceed, bizLogger.contentFormatArguments(),
        contentResolves);
    resolveValuesNeedResponse(joinPoint, proceed, new Resolve[]{bizLogger.targetId(),
        bizLogger.targetName(), bizLogger.targetType()}, targetResolves);
    bizLog.setContent(String.format(contentFormat, contentResolves.toArray()));
    resolveValuesNeedResponse(joinPoint, proceed, new Resolve[]{bizLogger.module()},
        moduleResolves);
    bizLog.setModule(moduleResolves.getFirst());

    try {
      bizLog.setTarget(BizLogTarget.builder()
          .targetId(StringUtils.isNotBlank(targetResolves.get(0)) ?
              targetResolves.get(0) : bizLogger.isLogin() ?
              "" : keycloakService.getUserResource(username).toRepresentation().getId())
          .targetName(targetResolves.get(1))
          .targetType(targetResolves.get(2))
          .build());

      new TransactionTemplate(keycloakTransactionManager).executeWithoutResult(transactionStatus ->
          userEntityRepository.findByUsernameAndRealmId(username, keycloakService.getRealm())
              .flatMap(userEntity -> userEntity.getRoles()
                  .stream()
                  .filter(KeycloakRole::getClientRole)
                  .findFirst())
              .ifPresent(keycloakRole -> bizLog.setUserRole(keycloakRole.getName())));

      bizLogRepository.save(bizLog);
    } catch (Exception e) {
      log.error("保存日志出错", e);
    }

    return proceed;
  }

  @Around("@annotation(bizLoggers)")
  @SneakyThrows
  public Object around(ProceedingJoinPoint joinPoint, BizLoggers bizLoggers) {

    // 函数执行
    final var proceed = joinPoint.proceed();

    for (BizLogger bizLogger : bizLoggers.value()) {
      // 执行前
      final var moduleResolves = resolveValuesNotResponse(joinPoint, bizLogger.module());
      final var type = bizLogger.type();
      final var contentFormat = bizLogger.contentFormat();
      final var contentResolves = resolveValuesNotResponse(joinPoint,
          bizLogger.contentFormatArguments());
      final var targetResolves = resolveValuesNotResponse(joinPoint, bizLogger.targetId(),
          bizLogger.targetName(), bizLogger.targetType());

      final var bizLog = new BizLog();
      String username;
      if (bizLogger.isLogin()) {
        username = JwtHelper.getUsername();
      } else {
        username = targetResolves.get(1);
      }
      bizLog.setUsername(username);
      bizLog.setType(type);

      bizLog.setIp(JakartaServletUtil.getClientIP(getRequest()));

      // 执行后
      resolveValuesNeedResponse(joinPoint, proceed, bizLogger.contentFormatArguments(),
          contentResolves);
      resolveValuesNeedResponse(joinPoint, proceed, new Resolve[]{bizLogger.targetId(),
          bizLogger.targetName(), bizLogger.targetType()}, targetResolves);
      bizLog.setContent(String.format(contentFormat, contentResolves.toArray()));
      resolveValuesNeedResponse(joinPoint, proceed, new Resolve[]{bizLogger.module()},
          moduleResolves);
      bizLog.setModule(moduleResolves.getFirst());

      try {
        bizLog.setTarget(BizLogTarget.builder()
            .targetId(StringUtils.isNotBlank(targetResolves.get(0)) ?
                targetResolves.get(0) : bizLogger.isLogin() ?
                "" : keycloakService.getUserResource(username).toRepresentation().getId())
            .targetName(targetResolves.get(1))
            .targetType(targetResolves.get(2))
            .build());

        new TransactionTemplate(keycloakTransactionManager).executeWithoutResult(
            transactionStatus ->
                userEntityRepository.findByUsernameAndRealmId(username, keycloakService.getRealm())
                    .flatMap(userEntity -> userEntity.getRoles()
                        .stream()
                        .filter(KeycloakRole::getClientRole)
                        .findFirst())
                    .ifPresent(keycloakRole ->
                        bizLog.setUserRole(keycloakRole.getName())));

        bizLogRepository.save(bizLog);
      } catch (Exception e) {
        log.error("保存日志出错", e);
      }
    }

    return proceed;
  }
}
